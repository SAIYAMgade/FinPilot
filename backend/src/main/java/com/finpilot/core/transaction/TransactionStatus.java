package com.finpilot.core.transaction;

public enum TransactionStatus {
    RECEIVED,
    VALIDATED,
    PENDING,
    POSTED,
    FAILED,
    REVERSED,
    REQUIRES_REVIEW
}
