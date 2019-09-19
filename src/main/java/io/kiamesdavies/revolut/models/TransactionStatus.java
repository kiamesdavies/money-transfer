package io.kiamesdavies.revolut.models;

/**
 * Possible status of a transfer
 *
 * NEW ---> WITHDRAWN ---> COMPLETED -------|
 *     |             |--> DEPOSIT_FAILED ------ ROLLBACK
 *     |--> FAILED ----------------------|
 */
public enum TransactionStatus {
    NEW,
    WITHDRAWN,
    COMPLETED,
    DEPOSIT_FAILED,
    ROLLBACK,
    FAILED
}
