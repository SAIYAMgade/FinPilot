package com.finpilot.core.exception;

import com.finpilot.core.auth.User;
import com.finpilot.core.auth.UserRepository;
import com.finpilot.core.common.ApiException;
import com.finpilot.core.common.PagedResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinancialExceptionService {

    private final FinancialExceptionRepository exceptionRepository;
    private final ExceptionCommentRepository commentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResponse<ExceptionDtos.ExceptionResponse> getExceptions(
            ExceptionStatus status,
            ExceptionSeverity severity,
            ExceptionCategory category,
            String assignedToId,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<FinancialException> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (StringUtils.hasText(assignedToId)) {
                predicates.add(cb.equal(root.get("assignedTo").get("id"), assignedToId));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("exceptionNumber")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("rootCause")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<FinancialException> pageResult = exceptionRepository.findAll(spec, pageable);
        List<ExceptionDtos.ExceptionResponse> dtoList = pageResult.getContent().stream()
                .map(ExceptionDtos.ExceptionResponse::fromEntity)
                .toList();

        return new PagedResponse<>(dtoList, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements(), pageResult.getTotalPages(), pageResult.isLast());
    }

    @Transactional(readOnly = true)
    public ExceptionDtos.ExceptionResponse getExceptionById(String id) {
        FinancialException exception = exceptionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Exception not found: " + id));
        return ExceptionDtos.ExceptionResponse.fromEntity(exception);
    }

    @Transactional
    public ExceptionDtos.ExceptionResponse assignException(String id, String userId) {
        FinancialException exception = exceptionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Exception not found: " + id));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + userId));

        exception.setAssignedTo(user);
        if (exception.getStatus() == ExceptionStatus.OPEN) {
            exception.setStatus(ExceptionStatus.INVESTIGATING);
        }

        FinancialException saved = exceptionRepository.save(exception);
        return ExceptionDtos.ExceptionResponse.fromEntity(saved);
    }

    @Transactional
    public ExceptionDtos.CommentResponse addComment(String exceptionId, String commentText, String authorUsername) {
        FinancialException exception = exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> ApiException.notFound("Exception not found: " + exceptionId));

        User author = userRepository.findByUsername(authorUsername)
                .orElseThrow(() -> ApiException.notFound("Author user not found: " + authorUsername));

        ExceptionComment comment = ExceptionComment.builder()
                .id("comm-" + UUID.randomUUID().toString().substring(0, 8))
                .financialException(exception)
                .author(author)
                .commentText(commentText.trim())
                .createdAt(Instant.now())
                .build();

        ExceptionComment saved = commentRepository.save(comment);
        return ExceptionDtos.CommentResponse.fromEntity(saved);
    }

    @Transactional
    public ExceptionDtos.ExceptionResponse updateStatus(String id, ExceptionDtos.UpdateStatusRequest req) {
        FinancialException exception = exceptionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Exception not found: " + id));

        if (req.getStatus() != null) {
            exception.setStatus(req.getStatus());
        }
        if (StringUtils.hasText(req.getRootCause())) {
            exception.setRootCause(req.getRootCause());
        }
        if (StringUtils.hasText(req.getResolutionNotes())) {
            exception.setResolutionNotes(req.getResolutionNotes());
        }

        FinancialException saved = exceptionRepository.save(exception);
        return ExceptionDtos.ExceptionResponse.fromEntity(saved);
    }
}
