package io.kiamesdavies.revolut.models;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * The base Event class.
 */
public abstract class Evt implements Serializable {
    static final long serialVersionUID = 42L;


    public abstract static class BaseAccountEvt extends Evt {
        public final String bankAccountId;
        public final BigDecimal amount;
        public final String transactionId;
        public final long messageNanoTime;

        BaseAccountEvt(String bankAccountId, String transactionId, BigDecimal amount) {
            this.bankAccountId = bankAccountId;
            this.transactionId = transactionId;
            this.amount= amount;
            messageNanoTime = System.nanoTime();
        }
    }

    public static final class DepositEvent extends BaseAccountEvt {
        public DepositEvent(String bankAccountId, String transactionId, BigDecimal amount) {

            super(bankAccountId, transactionId, amount);
        }

        public DepositEvent(Cmd.DepositCmd depositCmd) {
            this(depositCmd.bankAccountId, depositCmd.transactionId, depositCmd.amount);
        }

        @Override
        public String toString() {
            return "DepositEvent{" +
                    "bankAccountId='" + bankAccountId + '\'' +
                    ", amount=" + amount +
                    ", transactionId='" + transactionId + '\'' +
                    ", messageNanoTime=" + messageNanoTime +
                    '}';
        }
    }

    public static final class WithdrawEvent extends BaseAccountEvt {

        public WithdrawEvent(String bankAccountId, String transactionId, BigDecimal amount) {

            super(bankAccountId, transactionId, amount);
        }

        public WithdrawEvent(Cmd.WithdrawCmd withdrawCmd) {
            this(withdrawCmd.bankAccountId, withdrawCmd.transactionId, withdrawCmd.amount);
        }
    }

    public static final class FailedEvent extends Evt {

        public final String bankAccountId;
        public final Type type;
        public final String additionalDescription;

        public FailedEvent(String bankAccountId, Type type, String additionalDescription) {
            this.bankAccountId = bankAccountId;
            this.type = type;
            this.additionalDescription = additionalDescription;
        }

        public FailedEvent(String bankAccountId, Type type) {
            this(bankAccountId, type, null);
        }


        public enum Type {
            INSUFFICIENT_FUNDS,
            INVALID_AMOUNT
        }

        @Override
        public String toString() {
            return "FailedEvent{" +
                    "bankAccountIds='" + bankAccountId + '\'' +
                    ", type=" + type +
                    ", additionalDescription='" + additionalDescription + '\'' +
                    '}';
        }
    }


    public static final class TransactionEvent extends Evt {
        public final TransactionType transactionType;
        public final String transactionId;
        public final BigDecimal amount;
        public final String remarks;
        public final String source;
        public final String accountToId;
        public final String accountFromId;
        public final TransactionStatus status;
        public final long messageNanoTime;

        public TransactionEvent(String transactionId, String accountFromId, String accountToId, BigDecimal amount,TransactionType transactionType, String remarks, String source, TransactionStatus status) {
            this.transactionId = transactionId;
            this.amount = amount;
            this.transactionType= transactionType;
            this.remarks = remarks;
            this.source = source;
            this.accountToId = accountToId;
            this.accountFromId = accountFromId;
            this.status = status;
            messageNanoTime = System.nanoTime();
        }

        public TransactionEvent(String transactionId, Cmd.TransferCmd transferCmd, TransactionStatus status) {
            this(transactionId, transferCmd.accountFromId, transferCmd.accountToId, transferCmd.amount, transferCmd.transactionType, transferCmd.remarks, transferCmd.source, status);
        }


        public TransactionEvent with(TransactionStatus transactionStatus) {
            return new TransactionEvent(transactionId, accountFromId, accountToId, amount,transactionType,remarks, source, transactionStatus);
        }

        @Override
        public String toString() {
            return "TransactionEvent{" +
                    "transactionId='" + transactionId + '\'' +
                    ", amount=" + amount +
                    ", remarks='" + remarks + '\'' +
                    ", source='" + source + '\'' +
                    ", accountToId='" + accountToId + '\'' +
                    ", accountFromId='" + accountFromId + '\'' +
                    ", status=" + status +
                    '}';
        }
    }


}
