package com.finpilot.core.transaction;

import com.finpilot.core.common.ApiException;
import com.finpilot.core.common.PagedResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository transactionEventRepository;

    @Transactional
    public TransactionDtos.TransactionResponse createTransaction(TransactionDtos.CreateTransactionRequest req, String initiatedBy) {
        // 1. Idempotency Check
        if (StringUtils.hasText(req.getIdempotencyKey())) {
            Optional<Transaction> existingOpt = transactionRepository.findByIdempotencyKey(req.getIdempotencyKey());
            if (existingOpt.isPresent()) {
                Transaction existing = existingOpt.get();
                // Compare critical fields
                boolean amountMatches = existing.getAmount().compareTo(req.getAmount()) == 0;
                boolean currencyMatches = existing.getCurrency().equalsIgnoreCase(req.getCurrency());
                boolean sourceMatches = existing.getSourceSystem().equalsIgnoreCase(req.getSourceSystem());
                boolean typeMatches = existing.getTransactionType() == req.getTransactionType();

                if (amountMatches && currencyMatches && sourceMatches && typeMatches) {
                    log.info("Idempotent replay detected for key={}", req.getIdempotencyKey());
                    return TransactionDtos.TransactionResponse.fromEntity(existing);
                } else {
                    log.warn("Idempotency key collision with mismatched payload for key={}", req.getIdempotencyKey());
                    throw ApiException.conflict("Idempotency key reused with different transaction parameters");
                }
            }
        }

        // 2. Build Transaction
        String txnId = "txn-" + UUID.randomUUID().toString().substring(0, 8);
        Instant txnTimestamp = req.getTransactionTimestamp() != null ? req.getTransactionTimestamp() : Instant.now();

        Transaction txn = Transaction.builder()
                .id(txnId)
                .externalReference(req.getExternalReference())
                .sourceSystem(req.getSourceSystem())
                .sourceAccountNumber(req.getSourceAccountNumber())
                .destinationAccountNumber(req.getDestinationAccountNumber())
                .amount(req.getAmount())
                .currency(req.getCurrency() != null ? req.getCurrency().toUpperCase() : "INR")
                .transactionType(req.getTransactionType())
                .status(TransactionStatus.RECEIVED)
                .idempotencyKey(req.getIdempotencyKey())
                .description(req.getDescription())
                .metadata(req.getMetadata())
                .transactionTimestamp(txnTimestamp)
                .receivedTimestamp(Instant.now())
                .build();

        Transaction saved = transactionRepository.save(txn);

        // Record Initial Event
        recordEvent(saved.getId(), null, TransactionStatus.RECEIVED, initiatedBy, "Transaction received by ingestion gateway");

        // Validate and progress to PENDING
        saved.setStatus(TransactionStatus.VALIDATED);
        recordEvent(saved.getId(), TransactionStatus.RECEIVED, TransactionStatus.VALIDATED, initiatedBy, "Mandatory fields, currency, and amount validated");

        saved.setStatus(TransactionStatus.PENDING);
        recordEvent(saved.getId(), TransactionStatus.VALIDATED, TransactionStatus.PENDING, initiatedBy, "Ready for double-entry posting or reconciliation");
        saved = transactionRepository.save(saved);

        return TransactionDtos.TransactionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionDtos.TransactionResponse> getTransactions(
            TransactionStatus status,
            String sourceSystem,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(sourceSystem)) {
                predicates.add(cb.equal(cb.lower(root.get("sourceSystem")), sourceSystem.toLowerCase()));
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("id")), pattern),
                        cb.like(cb.lower(root.get("externalReference")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Transaction> pageResult = transactionRepository.findAll(spec, pageable);
        List<TransactionDtos.TransactionResponse> dtoList = pageResult.getContent().stream()
                .map(TransactionDtos.TransactionResponse::fromEntity)
                .toList();

        return new PagedResponse<>(dtoList, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements(), pageResult.getTotalPages(), pageResult.isLast());
    }

    @Transactional(readOnly = true)
    public TransactionDtos.TransactionResponse getTransactionById(String id) {
        Transaction txn = transactionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Transaction not found with id: " + id));
        return TransactionDtos.TransactionResponse.fromEntity(txn);
    }

    @Transactional(readOnly = true)
    public List<TransactionEvent> getTransactionEvents(String transactionId) {
        if (!transactionRepository.existsById(transactionId)) {
            throw ApiException.notFound("Transaction not found: " + transactionId);
        }
        return transactionEventRepository.findByTransactionIdOrderByCreatedAtAsc(transactionId);
    }

    @Transactional
    public void recordEvent(String transactionId, TransactionStatus from, TransactionStatus to, String actor, String reason) {
        TransactionEvent event = TransactionEvent.builder()
                .id("ev-" + UUID.randomUUID().toString().substring(0, 8))
                .transactionId(transactionId)
                .fromStatus(from)
                .toStatus(to)
                .triggeredBy(actor != null ? actor : "SYSTEM")
                .reason(reason)
                .createdAt(Instant.now())
                .build();
        transactionEventRepository.save(event);
    }

    @Transactional
    public TransactionDtos.TransactionImportResult importCsv(MultipartFile file, String importedBy) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("Uploaded CSV file is empty");
        }

        int total = 0;
        int imported = 0;
        int duplicates = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build().parse(reader)) {

            for (CSVRecord record : csvParser) {
                total++;
                try {
                    String extRef = record.get("external_reference");
                    String source = record.isSet("source_system") ? record.get("source_system") : "CSV_IMPORT";
                    String amountStr = record.get("amount");
                    String currency = record.isSet("currency") ? record.get("currency") : "INR";
                    String typeStr = record.isSet("transaction_type") ? record.get("transaction_type") : "PAYMENT";
                    String desc = record.isSet("description") ? record.get("description") : "Imported transaction";

                    if (!StringUtils.hasText(amountStr)) {
                        failed++;
                        errors.add("Row " + total + ": Missing amount");
                        continue;
                    }

                    BigDecimal amount = new BigDecimal(amountStr);
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        failed++;
                        errors.add("Row " + total + ": Amount must be positive");
                        continue;
                    }

                    TransactionType type = TransactionType.valueOf(typeStr.toUpperCase());

                    // Check duplicate external reference
                    if (StringUtils.hasText(extRef) && transactionRepository.findByExternalReference(extRef).isPresent()) {
                        duplicates++;
                        continue;
                    }

                    TransactionDtos.CreateTransactionRequest req = TransactionDtos.CreateTransactionRequest.builder()
                            .externalReference(extRef)
                            .sourceSystem(source)
                            .amount(amount)
                            .currency(currency)
                            .transactionType(type)
                            .description(desc)
                            .transactionTimestamp(Instant.now())
                            .build();

                    createTransaction(req, importedBy);
                    imported++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + total + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse transaction CSV", e);
            throw ApiException.badRequest("CSV parsing failed: " + e.getMessage());
        }

        return TransactionDtos.TransactionImportResult.builder()
                .totalRecords(total)
                .importedCount(imported)
                .duplicateCount(duplicates)
                .failedCount(failed)
                .errorMessages(errors)
                .build();
    }
}
