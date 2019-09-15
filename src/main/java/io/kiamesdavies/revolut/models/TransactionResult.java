package io.kiamesdavies.revolut.models;

public class TransactionResult {

    public static class Success extends TransactionResult {
        public final String transactionId;

        public Success(String transactionId) {
            this.transactionId = transactionId;
        }
    }

    public static class Failure extends TransactionResult {
        public final Throwable exception;

        public Failure(Throwable exception) {
            this.exception = exception;
        }
    }
}
