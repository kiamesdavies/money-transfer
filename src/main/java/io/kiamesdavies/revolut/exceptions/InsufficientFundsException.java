package io.kiamesdavies.revolut.exceptions;

/**
 * Signals that account has insufficient balance
 */
public class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
