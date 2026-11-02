package com.finpilot.core.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionEventRepository extends JpaRepository<TransactionEvent, String> {
    List<TransactionEvent> findByTransactionIdOrderByCreatedAtAsc(String transactionId);
}
