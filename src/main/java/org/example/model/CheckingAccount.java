package org.example.model;

import org.example.exceptions.BalanceNegativeException;

import java.math.BigDecimal;
import java.util.UUID;

public class CheckingAccount extends BankAccount {
    private final BigDecimal overdraftLimit; // не уверен насчет финализации

    public CheckingAccount(UUID userId) {
        super(userId);
        this.overdraftLimit = new BigDecimal("10000");
    }

    public CheckingAccount(UUID userId, String overdraftLimit) {
        super(userId);
        this.overdraftLimit = new BigDecimal(overdraftLimit);
    }

    public CheckingAccount(UUID userId, BigDecimal balance) {
        super(userId, balance);
        this.overdraftLimit = new BigDecimal("10000");
    }

    public CheckingAccount(UUID userId, BigDecimal balance, String overdraftLimit) {
        super(userId, balance);
        this.overdraftLimit = new BigDecimal(overdraftLimit);
    }

    public CheckingAccount(UUID id, UUID userId, BigDecimal balance) {
        super(id, userId, balance);
        this.overdraftLimit = new BigDecimal("10000");
    }

    public CheckingAccount(UUID id, UUID userId, BigDecimal balance, String overdraftLimit) {
        super(id, userId, balance);
        this.overdraftLimit = new BigDecimal(overdraftLimit);
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    @Override
    public void setBalance(BigDecimal balance) {
        if (overdraftLimit.add(balance).doubleValue() < 0) throw new BalanceNegativeException("Баланс не может быть ниже допустимого овердрафта");
        this.balance = balance;
    }

    @Override
    public boolean checkBalanceLimit(BigDecimal amount) {
       return balance.add(overdraftLimit).compareTo(amount) < 0;
    }
}
