package io.kiamesdavies.revolut.exceptions;

/**
 * Signals that bank account was not found.
 */
public class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
