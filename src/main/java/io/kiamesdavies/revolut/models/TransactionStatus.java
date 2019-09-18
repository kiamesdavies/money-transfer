package io.kiamesdavies.revolut.models;

/**
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
