package org.example.model;

import org.example.exceptions.BalanceNegativeException;
import org.example.util.AccountType;
import org.example.util.AccountStatus;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class BankAccount {
    protected final UUID id;
    protected final UUID userId;
    protected BigDecimal balance;//пока в однопоточном
    protected final Lock lock = new ReentrantLock();
    protected AccountType accountType;
    protected AccountStatus status;

    public BankAccount(UUID userId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.balance = new BigDecimal(0);
        this.accountType = AccountType.DEFAULT;
        this.status = AccountStatus.ACTIVE;
    }

    public BankAccount(UUID userId, BigDecimal balance) {
        if (balance.doubleValue() < 0) throw new BalanceNegativeException("Balance cannot be negative");
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.balance = balance;
        this.accountType = AccountType.DEFAULT;
        this.status = AccountStatus.ACTIVE;
    }

    public BankAccount(UUID id, UUID userId, BigDecimal balance) {
        if (balance.doubleValue() < 0) throw new BalanceNegativeException("Balance cannot be negative");
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.accountType = AccountType.DEFAULT;
        this.status = AccountStatus.ACTIVE;
    }

    public BankAccount(UUID id, UUID userId, BigDecimal balance, AccountType accountType) {
        if (balance.doubleValue() < 0) throw new BalanceNegativeException("Balance cannot be negative");
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.accountType = accountType;
        this.status = AccountStatus.ACTIVE;
    }

    public BankAccount(UUID id, UUID userId, BigDecimal balance, AccountType accountType, AccountStatus status) {
        if (balance.doubleValue() < 0) throw new BalanceNegativeException("Balance cannot be negative");
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.accountType = accountType;
        this.status = status;
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

    public AccountType getAccountType() {
        return accountType;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
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
        BankAccount account = (BankAccount) o;
        return Objects.equals(id, account.id) && Objects.equals(userId, account.userId) && Objects.equals(balance, account.balance) && Objects.equals(lock, account.lock) && accountType == account.accountType && status == account.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, balance, lock, accountType, status);
    }

    @Override
    public String toString() {
        return "BankAccount{" +
                "id=" + id +
                ", userId=" + userId +
                ", balance=" + balance +
                ", lock=" + lock +
                ", accountType=" + accountType +
                ", status=" + status +
                '}';
    }
}
