package com.finpilot.core.ledger;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "reference_id", nullable = false, length = 100)
    private String referenceId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "posting_status", nullable = false, length = 50)
    @Builder.Default
    private PostingStatus postingStatus = PostingStatus.DRAFT;

    @Column(name = "posted_timestamp")
    private Instant postedTimestamp;

    @Column(name = "reversed_timestamp")
    private Instant reversedTimestamp;

    @Column(name = "reversal_of_id", length = 36)
    private String reversalOfId;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<JournalEntryLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (postingStatus == null) postingStatus = PostingStatus.DRAFT;
    }
}
