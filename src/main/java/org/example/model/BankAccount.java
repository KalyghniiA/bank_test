package org.example.model;

import org.example.exceptions.BalanceNegativeException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;


public class BankAccount {
    private final UUID id;
    private final UUID userId;
    private BigDecimal balance;//пока в однопоточном

    public BankAccount(UUID userId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.balance = new BigDecimal(0);
    }

    public BankAccount(UUID userId, BigDecimal balance) {
        if (balance.doubleValue() < 0) throw new BalanceNegativeException("Balance cannot be negative");
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.balance = balance;
    }

    public BankAccount(UUID id, UUID userId, BigDecimal balance) {
        if (balance.doubleValue() < 0) throw new BalanceNegativeException("Balance cannot be negative");
        this.id = id;
        this.userId = userId;
        this.balance = balance;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal amount) {
        if (amount.doubleValue() < 0) throw new BalanceNegativeException("Баланс не может быть отрицательным");
        this.balance = amount;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BankAccount that = (BankAccount) o;
        return Objects.equals(id, that.id) && Objects.equals(userId, that.userId) && Objects.equals(balance, that.balance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, balance);
    }

    @Override
    public String toString() {
        return "BankAccount{" +
                "id=" + id +
                ", userId=" + userId +
                ", balance=" + balance +
                '}';
    }
}
