package io.kiamesdavies.revolut.models;

/**
 * The type of a transaction been carried out
 */
public enum TransactionType {
    /**
     * Indicates that is a transfer
     */
    TRANSFER,

    /**
     * Indicates that is a deposit type, like over the counter deposit
     */
    DEPOSIT,

    /**
     * Indicates that this is a withdrawal type, like withdrawing in an ATM
     */
    WITHDRAWAL
}
