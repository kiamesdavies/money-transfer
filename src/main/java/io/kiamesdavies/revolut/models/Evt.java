package io.kiamesdavies.revolut.models;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * An Event class
 */
public abstract class Evt implements Serializable {
    static final long serialVersionUID = 42L;


    public abstract static class BaseAccountEvt extends Evt {
        private String bankAccountId;
        private BigDecimal amount;
        private String transactionId;

        BaseAccountEvt(String bankAccountId,String transactionId, BigDecimal amount) {
            this.setBankAccountId(bankAccountId);
            this.transactionId = transactionId;
            this.setAmount(amount);
        }

        BaseAccountEvt() {
        }


        public String getBankAccountId() {
            return bankAccountId;
        }

        public void setBankAccountId(String bankAccountId) {
            this.bankAccountId = bankAccountId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public final static class DepositEvent extends BaseAccountEvt {

        DepositEvent() {
            
        }

        public DepositEvent(String bankAccountId,String transactionId, BigDecimal amount) {

            super(bankAccountId,transactionId, amount);
        }

        public DepositEvent(Cmd.DepositCmd depositCmd) {
            //a transaction id with rollback shows that it was intended for rollback and simply replaced
            this(depositCmd.bankAccountId, depositCmd.transactionId.replace("-rollback",""), depositCmd.amount);
        }
    }

    public final static class WithdrawEvent extends BaseAccountEvt {

        WithdrawEvent() {

        }

        public WithdrawEvent(String bankAccountId,String transactionId, BigDecimal amount) {

            super(bankAccountId,transactionId, amount);
        }

        public WithdrawEvent(Cmd.WithdrawCmd withdrawCmd) {
            //a transaction id with rollback shows that it was intended for rollback and simply replaced
            this(withdrawCmd.bankAccountId, withdrawCmd.transactionId.replace("-rollback",""), withdrawCmd.amount);
        }
    }

    public final static class FailedEvent extends Evt {


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

        @Override


        public String toString() {
            return "FailedEvent{" +
                    "bankAccountIds='" + getBankAccountId() + '\'' +
                    ", type=" + getType() +
                    ", additionalDescription='" + getAdditionalDescription() + '\'' +
                    '}';
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
    }


    public final static class TransactionEvent extends Evt {
        private String transactionId;
        private BigDecimal amount;
        private String remarks;
        private String source;
        private String accountToId;
        private String accountFromId;
        private TransactionStatus status;

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
        }

        public TransactionEvent(String transactionId, Cmd.TransferCmd transferCmd, TransactionStatus status) {
            this(transactionId, transferCmd.accountFromId, transferCmd.accountToId, transferCmd.amount, transferCmd.remarks, transferCmd.source, status);
        }


        public TransactionEvent with(TransactionStatus transactionStatus) {
            return new TransactionEvent(getTransactionId(), getAccountFromId(), getAccountToId(), getAmount(), getRemarks(), getSource(), transactionStatus);
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
    }


}
