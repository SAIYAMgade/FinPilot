package com.finpilot.core.ledger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, String> {

    List<JournalEntry> findByReferenceId(String referenceId);

    Page<JournalEntry> findByPostingStatus(PostingStatus postingStatus, Pageable pageable);

    @Query("SELECT j FROM JournalEntry j WHERE j.postedTimestamp BETWEEN :startDate AND :endDate ORDER BY j.postedTimestamp DESC")
    List<JournalEntry> findByPostedDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(j) FROM JournalEntry j WHERE j.postingStatus = :status")
    long countByStatus(@Param("status") PostingStatus status);
}
