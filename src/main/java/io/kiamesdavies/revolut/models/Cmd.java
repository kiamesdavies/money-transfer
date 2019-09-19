package io.kiamesdavies.revolut.models;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A command class.
 */
public abstract class Cmd implements Serializable {
    static final long serialVersionUID = 42L;

    public static abstract class BaseAccountCmd extends Cmd {
        public final long deliveryId;
        public final String transactionId;
        public final String bankAccountId;
        public final BigDecimal amount;

        BaseAccountCmd(long deliveryId, String transactionId, String bankAccountId, BigDecimal amount) {
            this.deliveryId = deliveryId;
            this.transactionId = transactionId;
            this.amount = amount;
            this.bankAccountId = bankAccountId;
        }
    }

    public static final class DepositCmd extends BaseAccountCmd {

        public DepositCmd(long deliveryId, String transactionId, String bankAccountId, BigDecimal amount) {
            super(deliveryId, transactionId, bankAccountId, amount);
        }
    }

    public static final class WithdrawCmd extends BaseAccountCmd {

        public WithdrawCmd(long deliveryId, String transactionId, String bankAccountId, BigDecimal amount) {
            super(deliveryId, transactionId, bankAccountId, amount);
        }
    }

    public static final class TransferCmd extends Cmd {
        public final BigDecimal amount;
        public final String remarks;
        public final String source;
        public final String accountToId;
        public final String accountFromId;

        public TransferCmd(String accountFromId, String accountToId, BigDecimal amount, String remarks, String source) {
            this.amount = amount;
            this.remarks = remarks;
            this.source = source;
            this.accountToId = accountToId;
            this.accountFromId = accountFromId;
        }
    }

}
