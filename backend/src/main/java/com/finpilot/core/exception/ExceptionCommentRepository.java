package com.finpilot.core.exception;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExceptionCommentRepository extends JpaRepository<ExceptionComment, String> {
    List<ExceptionComment> findByFinancialExceptionIdOrderByCreatedAtAsc(String exceptionId);
}
