package com.finpilot.core.reconciliation;

import com.finpilot.core.common.ApiException;
import com.finpilot.core.common.PagedResponse;
import com.finpilot.core.exception.*;
import com.finpilot.core.transaction.Transaction;
import com.finpilot.core.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final ReconciliationRunRepository runRepository;
    private final ExternalStatementRecordRepository externalRecordRepository;
    private final ReconciliationMatchRepository matchRepository;
    private final TransactionRepository transactionRepository;
    private final FinancialExceptionRepository exceptionRepository;

    @Transactional
    public ReconciliationDtos.ReconciliationRunResponse createRun(MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("Statement CSV file cannot be empty");
        }

        String checksum;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            checksum = hexString.toString();
        } catch (Exception e) {
            throw ApiException.badRequest("Failed to calculate SHA-256 checksum: " + e.getMessage());
        }

        String runId = "rec-" + UUID.randomUUID().toString().substring(0, 8);
        String runNumber = "REC-" + System.currentTimeMillis() % 1000000;

        ReconciliationRun run = ReconciliationRun.builder()
                .id(runId)
                .runNumber(runNumber)
                .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "statement.csv")
                .fileChecksum(checksum)
                .matchingRulesVersion("v1.0")
                .status("PENDING")
                .totalInternalRecords(0)
                .totalExternalRecords(0)
                .matchedCount(0)
                .discrepancyCount(0)
                .executionDurationMs(0)
                .createdAt(Instant.now())
                .build();

        ReconciliationRun savedRun = runRepository.save(run);

        // Parse and persist external records
        List<ExternalStatementRecord> externalRecords = new ArrayList<>();
        Set<String> seenRefs = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build().parse(reader)) {

            for (CSVRecord record : parser) {
                String ref = record.get("transaction_reference");
                String amountStr = record.get("amount");
                String currency = record.isSet("currency") ? record.get("currency") : "INR";
                String dateStr = record.isSet("date") ? record.get("date") : null;
                String status = record.isSet("status") ? record.get("status") : "SETTLED";

                if (!StringUtils.hasText(ref) || !StringUtils.hasText(amountStr)) {
                    continue;
                }

                Instant recordDate = Instant.now();
                if (StringUtils.hasText(dateStr)) {
                    try {
                        recordDate = Instant.parse(dateStr + "T00:00:00Z");
                    } catch (Exception ignored) {
                        recordDate = Instant.now();
                    }
                }

                ExternalStatementRecord ext = ExternalStatementRecord.builder()
                        .id("esr-" + UUID.randomUUID().toString().substring(0, 8))
                        .reconciliationRunId(savedRun.getId())
                        .transactionReference(ref.trim())
                        .amount(new BigDecimal(amountStr.trim()))
                        .currency(currency.trim().toUpperCase())
                        .recordDate(recordDate)
                        .rawStatus(status.trim())
                        .rawData(record.toString())
                        .build();

                externalRecords.add(ext);
            }
        } catch (Exception e) {
            log.error("Failed to parse external statement CSV", e);
            savedRun.setStatus("FAILED");
            savedRun.setErrorDetails("CSV Parsing Error: " + e.getMessage());
            runRepository.save(savedRun);
            throw ApiException.badRequest("Invalid CSV format: " + e.getMessage());
        }

        externalRecordRepository.saveAll(externalRecords);
        savedRun.setTotalExternalRecords(externalRecords.size());
        savedRun = runRepository.save(savedRun);

        return ReconciliationDtos.ReconciliationRunResponse.fromEntity(savedRun);
    }

    @Transactional
    public ReconciliationDtos.ReconciliationRunResponse executeReconciliation(String runId, int dateToleranceDays) {
        ReconciliationRun run = runRepository.findById(runId)
                .orElseThrow(() -> ApiException.notFound("Reconciliation run not found: " + runId));

        if ("PROCESSING".equalsIgnoreCase(run.getStatus())) {
            throw ApiException.conflict("Reconciliation run is already executing");
        }

        Instant startTime = Instant.now();
        run.setStatus("PROCESSING");
        runRepository.save(run);

        List<ExternalStatementRecord> externalRecords = externalRecordRepository.findByReconciliationRunId(runId);
        List<Transaction> internalTransactions = transactionRepository.findAll();

        run.setTotalInternalRecords(internalTransactions.size());

        Map<String, Transaction> internalByRef = new HashMap<>();
        Map<String, Transaction> internalById = new HashMap<>();
        for (Transaction t : internalTransactions) {
            if (StringUtils.hasText(t.getExternalReference())) {
                internalByRef.put(t.getExternalReference(), t);
            }
            internalById.put(t.getId(), t);
        }

        Set<String> matchedTxnIds = new HashSet<>();
        List<ReconciliationMatch> matches = new ArrayList<>();
        int matchedCount = 0;
        int discrepancyCount = 0;

        for (ExternalStatementRecord ext : externalRecords) {
            String ref = ext.getTransactionReference();
            Transaction internal = internalByRef.get(ref);

            if (internal == null) {
                // Try to find by Transaction ID directly if reference matches ID pattern
                internal = internalById.get(ref);
            }

            if (internal != null) {
                matchedTxnIds.add(internal.getId());
                boolean currencyMatches = internal.getCurrency().equalsIgnoreCase(ext.getCurrency());
                BigDecimal amountDiff = internal.getAmount().subtract(ext.getAmount()).abs();
                boolean amountMatches = amountDiff.compareTo(new BigDecimal("0.01")) <= 0;

                long daysDiff = Math.abs(ChronoUnit.DAYS.between(internal.getTransactionTimestamp(), ext.getRecordDate()));

                if (amountMatches && currencyMatches && daysDiff == 0) {
                    // LEVEL 1: Exact Match
                    matchedCount++;
                    matches.add(ReconciliationMatch.builder()
                            .id("match-" + UUID.randomUUID().toString().substring(0, 8))
                            .reconciliationRunId(runId)
                            .transaction(internal)
                            .externalRecord(ext)
                            .matchType(MatchType.LEVEL_1_EXACT)
                            .matchStatus(MatchStatus.MATCHED)
                            .amountDifference(BigDecimal.ZERO)
                            .matchScore(100)
                            .notes("Level 1 Exact Reference, Amount and Currency Match")
                            .build());
                } else if (!amountMatches) {
                    // LEVEL 2: Amount Mismatch
                    discrepancyCount++;
                    ReconciliationMatch match = ReconciliationMatch.builder()
                            .id("match-" + UUID.randomUUID().toString().substring(0, 8))
                            .reconciliationRunId(runId)
                            .transaction(internal)
                            .externalRecord(ext)
                            .matchType(MatchType.LEVEL_2_AMOUNT_MISMATCH)
                            .matchStatus(MatchStatus.AMOUNT_MISMATCH)
                            .amountDifference(amountDiff)
                            .matchScore(70)
                            .notes(String.format("Level 2 Amount Discrepancy: Internal=%s %s, External=%s %s. Diff=%s",
                                    internal.getAmount(), internal.getCurrency(), ext.getAmount(), ext.getCurrency(), amountDiff))
                            .build();
                    matches.add(match);

                    // Generate Exception Case
                    createExceptionForDiscrepancy(run.getId(), internal, ext, amountDiff,
                            "External statement amount differs from internal transaction amount by " + amountDiff + " " + ext.getCurrency(),
                            ExceptionCategory.AMOUNT_DISCREPANCY,
                            amountDiff.compareTo(new BigDecimal("1000")) > 0 ? ExceptionSeverity.HIGH : ExceptionSeverity.MEDIUM);

                } else if (daysDiff <= dateToleranceDays) {
                    // LEVEL 3: Date Tolerance Match
                    matchedCount++;
                    matches.add(ReconciliationMatch.builder()
                            .id("match-" + UUID.randomUUID().toString().substring(0, 8))
                            .reconciliationRunId(runId)
                            .transaction(internal)
                            .externalRecord(ext)
                            .matchType(MatchType.LEVEL_3_DATE_TOLERANCE)
                            .matchStatus(MatchStatus.MATCHED)
                            .amountDifference(BigDecimal.ZERO)
                            .matchScore(90)
                            .notes("Level 3 Matched within date tolerance (" + daysDiff + " days drift)")
                            .build());
                } else {
                    // LEVEL 4 / Date Mismatch
                    discrepancyCount++;
                    matches.add(ReconciliationMatch.builder()
                            .id("match-" + UUID.randomUUID().toString().substring(0, 8))
                            .reconciliationRunId(runId)
                            .transaction(internal)
                            .externalRecord(ext)
                            .matchType(MatchType.LEVEL_4_RULE_MATCH)
                            .matchStatus(MatchStatus.DATE_MISMATCH)
                            .amountDifference(BigDecimal.ZERO)
                            .matchScore(50)
                            .notes("Date drift (" + daysDiff + " days) exceeds allowed tolerance of " + dateToleranceDays + " days")
                            .build());

                    createExceptionForDiscrepancy(run.getId(), internal, ext, BigDecimal.ZERO,
                            "Transaction date drift (" + daysDiff + " days) exceeds tolerance",
                            ExceptionCategory.DATE_DRIFT, ExceptionSeverity.LOW);
                }
            } else {
                // MISSING_INTERNAL: Bank has record, FinPilot has no record
                discrepancyCount++;
                matches.add(ReconciliationMatch.builder()
                        .id("match-" + UUID.randomUUID().toString().substring(0, 8))
                        .reconciliationRunId(runId)
                        .transaction(null)
                        .externalRecord(ext)
                        .matchType(MatchType.UNMATCHED)
                        .matchStatus(MatchStatus.MISSING_INTERNAL)
                        .amountDifference(ext.getAmount())
                        .matchScore(0)
                        .notes("Unmatched settlement record: No corresponding internal transaction found for ref " + ext.getTransactionReference())
                        .build());

                createExceptionForDiscrepancy(run.getId(), null, ext, ext.getAmount(),
                        "Unmatched bank statement record with ref: " + ext.getTransactionReference(),
                        ExceptionCategory.UNMATCHED_SETTLEMENT,
                        ext.getAmount().compareTo(new BigDecimal("5000")) > 0 ? ExceptionSeverity.CRITICAL : ExceptionSeverity.HIGH);
            }
        }

        // Check for Internal transactions that were missing from external statement
        for (Transaction internal : internalTransactions) {
            if (!matchedTxnIds.contains(internal.getId()) && internal.getStatus() == com.finpilot.core.transaction.TransactionStatus.PENDING) {
                discrepancyCount++;
                matches.add(ReconciliationMatch.builder()
                        .id("match-" + UUID.randomUUID().toString().substring(0, 8))
                        .reconciliationRunId(runId)
                        .transaction(internal)
                        .externalRecord(null)
                        .matchType(MatchType.UNMATCHED)
                        .matchStatus(MatchStatus.MISSING_EXTERNAL)
                        .amountDifference(internal.getAmount())
                        .matchScore(0)
                        .notes("Internal transaction pending settlement was not found in external statement: " + internal.getId())
                        .build());
            }
        }

        matchRepository.saveAll(matches);

        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        run.setStatus("COMPLETED");
        run.setMatchedCount(matchedCount);
        run.setDiscrepancyCount(discrepancyCount);
        run.setExecutionDurationMs(durationMs);
        run.setCompletedAt(Instant.now());

        ReconciliationRun completedRun = runRepository.save(run);
        log.info("Reconciliation run {} completed in {} ms. Matched={}, Discrepancies={}",
                run.getRunNumber(), durationMs, matchedCount, discrepancyCount);

        return ReconciliationDtos.ReconciliationRunResponse.fromEntity(completedRun);
    }

    private void createExceptionForDiscrepancy(
            String runId,
            Transaction txn,
            ExternalStatementRecord ext,
            BigDecimal financialImpact,
            String description,
            ExceptionCategory category,
            ExceptionSeverity severity
    ) {
        String excId = "exc-" + UUID.randomUUID().toString().substring(0, 8);
        String excNumber = "EXP-" + System.currentTimeMillis() % 1000000;

        FinancialException exception = FinancialException.builder()
                .id(excId)
                .exceptionNumber(excNumber)
                .transaction(txn)
                .reconciliationRunId(runId)
                .category(category)
                .severity(severity)
                .description(description)
                .financialImpact(financialImpact)
                .status(ExceptionStatus.OPEN)
                .rootCause(null)
                .resolutionNotes(null)
                .build();

        exceptionRepository.save(exception);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationDtos.ReconciliationRunResponse> getAllRuns() {
        return runRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ReconciliationDtos.ReconciliationRunResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReconciliationDtos.ReconciliationRunResponse getRunById(String id) {
        ReconciliationRun run = runRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Reconciliation run not found: " + id));
        return ReconciliationDtos.ReconciliationRunResponse.fromEntity(run);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReconciliationDtos.ReconciliationMatchResponse> getRunMatches(
            String runId,
            MatchStatus matchStatus,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReconciliationMatch> pageResult = matchStatus != null
                ? matchRepository.findByReconciliationRunIdAndMatchStatus(runId, matchStatus, pageable)
                : matchRepository.findByReconciliationRunId(runId, pageable);

        List<ReconciliationDtos.ReconciliationMatchResponse> dtoList = pageResult.getContent().stream()
                .map(ReconciliationDtos.ReconciliationMatchResponse::fromEntity)
                .toList();

        return new PagedResponse<>(dtoList, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements(), pageResult.getTotalPages(), pageResult.isLast());
    }

    @Transactional(readOnly = true)
    public ReconciliationDtos.ReconciliationReport getReport(String runId) {
        ReconciliationRun run = runRepository.findById(runId)
                .orElseThrow(() -> ApiException.notFound("Reconciliation run not found: " + runId));

        List<ReconciliationMatch> matches = matchRepository.findByReconciliationRunId(runId);
        int total = run.getMatchedCount() + run.getDiscrepancyCount();
        double matchRate = total > 0 ? (double) run.getMatchedCount() / total * 100.0 : 0.0;

        BigDecimal totalDiscrepancy = matches.stream()
                .filter(m -> m.getMatchStatus() == MatchStatus.AMOUNT_MISMATCH)
                .map(ReconciliationMatch::getAmountDifference)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ReconciliationDtos.ReconciliationReport.builder()
                .runSummary(ReconciliationDtos.ReconciliationRunResponse.fromEntity(run))
                .matchRatePercent(Math.round(matchRate * 100.0) / 100.0)
                .totalDiscrepancyAmount(totalDiscrepancy)
                .matches(matches.stream().map(ReconciliationDtos.ReconciliationMatchResponse::fromEntity).toList())
                .build();
    }
}
