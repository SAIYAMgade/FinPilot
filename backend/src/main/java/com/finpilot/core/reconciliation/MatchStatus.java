package com.finpilot.core.reconciliation;

public enum MatchStatus {
    MATCHED,
    AMOUNT_MISMATCH,
    MISSING_INTERNAL,
    MISSING_EXTERNAL,
    DUPLICATE,
    DATE_MISMATCH,
    NEEDS_REVIEW
}
