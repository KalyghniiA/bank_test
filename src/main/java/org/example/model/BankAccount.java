package org.example.model;

import org.example.exceptions.BalanceNegativeException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class BankAccount {
    private final UUID id;
    private final UUID userId;
    protected BigDecimal balance;//пока в однопоточном
    private final Lock lock = new ReentrantLock();

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

    public BankAccount lock() {
        lock.lock();
        return this;
    }

    public void unlock() {
        lock.unlock();
    }

    public boolean checkBalanceLimit(BigDecimal amount) {
        return balance.compareTo(amount) < 0;
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
