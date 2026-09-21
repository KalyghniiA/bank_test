package org.example.model;

import org.example.exceptions.BalanceNegativeException;
import org.example.util.AccountType;
import org.example.util.AccountStatus;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class CheckingAccount extends BankAccount {
    private final BigDecimal overdraftLimit; // не уверен насчет финализации

    public CheckingAccount(UUID userId) {
        super(userId);
        this.overdraftLimit = new BigDecimal("10000");
        this.accountType = AccountType.CHECKING;
        this.status = AccountStatus.ACTIVE;
    }

    public CheckingAccount(UUID userId, String overdraftLimit) {
        super(userId);
        this.overdraftLimit = new BigDecimal(overdraftLimit);
        this.accountType = AccountType.CHECKING;
        this.status = AccountStatus.ACTIVE;
    }

    public CheckingAccount(UUID userId, BigDecimal balance) {
        super(userId, balance);
        this.overdraftLimit = new BigDecimal("10000");
        this.accountType = AccountType.CHECKING;
        this.status = AccountStatus.ACTIVE;
    }

    public CheckingAccount(UUID userId, BigDecimal balance, String overdraftLimit) {
        super(userId, balance);
        this.overdraftLimit = new BigDecimal(overdraftLimit);
        this.accountType = AccountType.CHECKING;
        this.status = AccountStatus.ACTIVE;
    }

    public CheckingAccount(UUID id, UUID userId, BigDecimal balance) {
        super(id, userId, balance);
        this.overdraftLimit = new BigDecimal("10000");
        this.accountType = AccountType.CHECKING;
        this.status = AccountStatus.ACTIVE;
    }

    public CheckingAccount(UUID id, UUID userId, BigDecimal balance, String overdraftLimit) {
        super(id, userId, balance);
        this.overdraftLimit = new BigDecimal(overdraftLimit);
        this.accountType = AccountType.CHECKING;
        this.status = AccountStatus.ACTIVE;
    }

    public CheckingAccount(UUID id, UUID userId, BigDecimal balance, BigDecimal overdraftLimit,  AccountStatus status) {
        super(id, userId, balance);
        this.overdraftLimit = overdraftLimit;
        this.accountType = AccountType.CHECKING;
        this.status = status;
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
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CheckingAccount that = (CheckingAccount) o;
        return Objects.equals(getOverdraftLimit(), that.getOverdraftLimit()) &&
                Objects.equals(id, that.id) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(balance, that.balance) &&
                Objects.equals(lock, that.lock) &&
                accountType == that.accountType &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getOverdraftLimit());
    }
}
