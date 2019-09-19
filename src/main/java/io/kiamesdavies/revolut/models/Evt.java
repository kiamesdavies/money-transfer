package io.kiamesdavies.revolut.models;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * The base Event class.
 */
public abstract class Evt implements Serializable {
    static final long serialVersionUID = 42L;


    public abstract static class BaseAccountEvt extends Evt {
        private String bankAccountId;
        private BigDecimal amount;
        private String transactionId;
        private long messageNanoTime;



        BaseAccountEvt() {
        }

        BaseAccountEvt(String bankAccountId, String transactionId, BigDecimal amount) {
            this.setBankAccountId(bankAccountId);
            this.transactionId = transactionId;
            this.setAmount(amount);
            messageNanoTime = System.nanoTime();
        }


        public String getBankAccountId() {
            return bankAccountId;
        }

        public void setBankAccountId(String bankAccountId) {
            this.bankAccountId = bankAccountId;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public long getMessageNanoTime() {
            return messageNanoTime;
        }

        public void setMessageNanoTime(long messageNanoTime) {
            this.messageNanoTime = messageNanoTime;
        }
    }

    public static final class DepositEvent extends BaseAccountEvt {

        DepositEvent() {

        }

        public DepositEvent(String bankAccountId, String transactionId, BigDecimal amount) {

            super(bankAccountId, transactionId, amount);
        }

        public DepositEvent(Cmd.DepositCmd depositCmd) {
            this(depositCmd.bankAccountId, depositCmd.transactionId, depositCmd.amount);
        }
    }

    public static final class WithdrawEvent extends BaseAccountEvt {

        WithdrawEvent() {

        }

        public WithdrawEvent(String bankAccountId, String transactionId, BigDecimal amount) {

            super(bankAccountId, transactionId, amount);
        }

        public WithdrawEvent(Cmd.WithdrawCmd withdrawCmd) {
            this(withdrawCmd.bankAccountId, withdrawCmd.transactionId, withdrawCmd.amount);
        }
    }

    public static final class FailedEvent extends Evt {


        private String bankAccountId;
        private Type type;
        private String additionalDescription;

        public FailedEvent() {
        }

        public FailedEvent(String bankAccountId, Type type, String additionalDescription) {
            this.bankAccountId = bankAccountId;
            this.type = type;
            this.additionalDescription = additionalDescription;
        }

        public FailedEvent(String bankAccountId, Type type) {
            this(bankAccountId, type, null);
        }

        public String getBankAccountId() {
            return bankAccountId;
        }

        public Type getType() {
            return type;
        }

        public String getAdditionalDescription() {
            return additionalDescription;
        }

        public enum Type {
            INSUFFICIENT_FUNDS,
            INVALID_AMOUNT
        }

        @Override
        public String toString() {
            return "FailedEvent{" +
                    "bankAccountIds='" + getBankAccountId() + '\'' +
                    ", type=" + getType() +
                    ", additionalDescription='" + getAdditionalDescription() + '\'' +
                    '}';
        }
    }


    public static final class TransactionEvent extends Evt {
        private String transactionId;
        private BigDecimal amount;
        private String remarks;
        private String source;
        private String accountToId;
        private String accountFromId;
        private TransactionStatus status;
        private long messageNanoTime;

        public TransactionEvent() {
        }

        public TransactionEvent(String transactionId, String accountFromId, String accountToId, BigDecimal amount, String remarks, String source, TransactionStatus status) {
            this.transactionId = transactionId;
            this.amount = amount;
            this.remarks = remarks;
            this.source = source;
            this.accountToId = accountToId;
            this.accountFromId = accountFromId;
            this.status = status;
            messageNanoTime = System.nanoTime();
        }

        public TransactionEvent(String transactionId, Cmd.TransferCmd transferCmd, TransactionStatus status) {
            this(transactionId, transferCmd.accountFromId, transferCmd.accountToId, transferCmd.amount, transferCmd.remarks, transferCmd.source, status);
        }


        public TransactionEvent with(TransactionStatus transactionStatus) {
            return new TransactionEvent(getTransactionId(), getAccountFromId(), getAccountToId(), getAmount(), getRemarks(), getSource(), transactionStatus);
        }




        public String getTransactionId() {
            return transactionId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getRemarks() {
            return remarks;
        }

        public String getSource() {
            return source;
        }

        public String getAccountToId() {
            return accountToId;
        }

        public String getAccountFromId() {
            return accountFromId;
        }

        public TransactionStatus getStatus() {
            return status;
        }

        public long getMessageNanoTime() {
            return messageNanoTime;
        }

        public void setMessageNanoTime(long messageNanoTime) {
            this.messageNanoTime = messageNanoTime;
        }


        @Override
        public String toString() {
            return "TransactionEvent{" +
                    "transactionId='" + getTransactionId() + '\'' +
                    ", amount=" + getAmount() +
                    ", remarks='" + getRemarks() + '\'' +
                    ", source='" + getSource() + '\'' +
                    ", accountToId='" + getAccountToId() + '\'' +
                    ", accountFromId='" + getAccountFromId() + '\'' +
                    ", status=" + getStatus() +
                    '}';
        }
    }


}
