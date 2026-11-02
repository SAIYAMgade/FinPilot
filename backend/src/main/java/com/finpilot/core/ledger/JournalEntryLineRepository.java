package com.finpilot.core.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLine, String> {

    List<JournalEntryLine> findByAccountId(String accountId);

    @Query("SELECT COALESCE(SUM(jel.debitAmount), 0) FROM JournalEntryLine jel WHERE jel.account.id = :accountId AND jel.journalEntry.postingStatus = 'POSTED'")
    BigDecimal sumDebitsByAccountId(@Param("accountId") String accountId);

    @Query("SELECT COALESCE(SUM(jel.creditAmount), 0) FROM JournalEntryLine jel WHERE jel.account.id = :accountId AND jel.journalEntry.postingStatus = 'POSTED'")
    BigDecimal sumCreditsByAccountId(@Param("accountId") String accountId);
}
