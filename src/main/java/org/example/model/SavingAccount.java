package org.example.model;

import org.example.exceptions.BalanceLimitException;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class SavingAccount extends BankAccount {
    private int withdrawLimit;

    public SavingAccount(UUID userId) {
        super(userId);
        this.withdrawLimit = 0;
    }

    public SavingAccount(UUID userId, int withdrawLimit) {
        super(userId);
        this.withdrawLimit = withdrawLimit;
    }

    public SavingAccount(UUID userId, BigDecimal balance) {
        super(userId, balance);
        this.withdrawLimit = 0;
    }

    public SavingAccount(UUID userId, BigDecimal balance, int withdrawLimit) {
        super(userId, balance);
        this.withdrawLimit = withdrawLimit;
    }

    public SavingAccount(UUID id, UUID userId, BigDecimal balance) {
        super(id, userId, balance);
        this.withdrawLimit = 0;
    }

    public SavingAccount(UUID id, UUID userId, BigDecimal balance, int withdrawLimit) {
        super(id, userId, balance);
        this.withdrawLimit = withdrawLimit;
    }

    public int getWithdrawLimit() {
        return withdrawLimit;
    }

    @Override
    public void setBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) > 0) {
            if (withdrawLimit > 0) {
                --withdrawLimit;
                this.balance = amount;
            } else throw new BalanceLimitException("Превышен допустимый лимит снятий");
        } else {
            this.balance = amount;
        }
    }

    @Override
    public boolean checkBalanceLimit(BigDecimal amount) {
        return withdrawLimit <= 0 || balance.compareTo(amount) < 0;
    }
}
