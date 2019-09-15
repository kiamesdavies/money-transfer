package io.kiamesdavies.revolut.models;

public enum TransactionStatus {
    NEW,
    WITHDRAWN,
    COMPLETED,
    WITHDRAW_FAILED,
    DEPOSIT_FAILED,
    ROLLBACK,
    ROLLBACK_FAILED,
    FAILED
}
