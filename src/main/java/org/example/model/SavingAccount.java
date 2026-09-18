package org.example.model;

import org.example.exceptions.BalanceLimitException;
import org.example.util.AccountType;
import org.example.util.Constant;
import org.example.util.AccountStatus;

import java.math.BigDecimal;
import java.time.Clock;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public class SavingAccount extends BankAccount implements InterestBearingAccount {
    private int withdrawLimit;
    private final int maxWithdrawalLimit;
    private LocalDateTime dateLastAccrual = LocalDateTime.now();

    public SavingAccount(UUID userId) {
        super(userId);
        this.withdrawLimit = Constant.MAX_WITHDRAWALS;
        this.maxWithdrawalLimit = Constant.MAX_WITHDRAWALS;
        this.accountType = AccountType.SAVING;
        this.status = AccountStatus.ACTIVE;
    }

    public SavingAccount(UUID userId, int withdrawLimit) {
        super(userId);
        this.withdrawLimit = withdrawLimit;
        this.maxWithdrawalLimit = withdrawLimit;
        this.accountType = AccountType.SAVING;
        this.status = AccountStatus.ACTIVE;
    }

    public SavingAccount(UUID userId, int withdrawLimit, int maxWithdrawalLimit) {
        super(userId);
        this.withdrawLimit = withdrawLimit;
        this.maxWithdrawalLimit = maxWithdrawalLimit;
        this.accountType = AccountType.SAVING;
        this.status = AccountStatus.ACTIVE;
    }

    public SavingAccount(UUID userId, BigDecimal balance) {
        super(userId, balance);
        this.withdrawLimit = Constant.MAX_WITHDRAWALS;
        this.maxWithdrawalLimit = Constant.MAX_WITHDRAWALS;
        this.accountType = AccountType.SAVING;
        this.status = AccountStatus.ACTIVE;
    }

    public SavingAccount(UUID userId, BigDecimal balance, int withdrawLimit) {
        super(userId, balance);
        this.withdrawLimit = withdrawLimit;
        this.maxWithdrawalLimit = withdrawLimit;
        this.accountType = AccountType.SAVING;
        this.status = AccountStatus.ACTIVE;
    }

    public SavingAccount(UUID userId, BigDecimal balance, int withdrawLimit, int maxWithdrawalLimit) {
        super(userId, balance);
        this.withdrawLimit = withdrawLimit;
        this.maxWithdrawalLimit = maxWithdrawalLimit;
        this.accountType = AccountType.SAVING;
        this.status = AccountStatus.ACTIVE;
    }

    public SavingAccount(UUID id, UUID userId, BigDecimal balance) {
        super(id, userId, balance);
        this.withdrawLimit = Constant.MAX_WITHDRAWALS;
        this.maxWithdrawalLimit = Constant.MAX_WITHDRAWALS;
        this.accountType = AccountType.SAVING;
        this.status = AccountStatus.ACTIVE;
    }

    public SavingAccount(UUID id, UUID userId, BigDecimal balance, int withdrawLimit) {
        super(id, userId, balance);
        this.withdrawLimit = withdrawLimit;
        this.maxWithdrawalLimit = withdrawLimit;
        this.accountType = AccountType.SAVING;
        this.status = AccountStatus.ACTIVE;
    }

    public SavingAccount(UUID id, UUID userId, BigDecimal balance, int withdrawLimit, int maxWithdrawalLimit) {
        super(id, userId, balance);
        this.withdrawLimit = withdrawLimit;
        this.maxWithdrawalLimit = maxWithdrawalLimit;
        this.accountType = AccountType.SAVING;
        this.status = AccountStatus.ACTIVE;
    }

    public SavingAccount(UUID id, UUID userId, BigDecimal balance, int withdrawLimit, LocalDateTime dateLastAccrual) {
        this(id, userId, balance, withdrawLimit);
        this.dateLastAccrual = dateLastAccrual;
        this.accountType = AccountType.SAVING;
        this.status = AccountStatus.ACTIVE;
    }

    public SavingAccount(UUID id, UUID userId, BigDecimal balance, int withdrawLimit,int maxWithdrawalLimit, LocalDateTime dateLastAccrual) {
        this(id, userId, balance, withdrawLimit, maxWithdrawalLimit);
        this.dateLastAccrual = dateLastAccrual;
        this.accountType = AccountType.SAVING;
        this.status = AccountStatus.ACTIVE;
    }

    public SavingAccount(UUID id, UUID userId, BigDecimal balance, int withdrawLimit,int maxWithdrawalLimit, LocalDateTime dateLastAccrual, AccountStatus status) {
        this(id, userId, balance, withdrawLimit, maxWithdrawalLimit);
        this.dateLastAccrual = dateLastAccrual;
        this.accountType = AccountType.SAVING;
        this.status = status;
    }

    public int getWithdrawLimit() {
        return withdrawLimit;
    }

    public int getMaxWithdrawalLimit() {return maxWithdrawalLimit;}

    public LocalDateTime getDateLastAccrual() {return dateLastAccrual;}

    @Override
    public void accrueInterestIfDue(Clock clock) {
        LocalDateTime timeUpdated = LocalDateTime.now(clock);
        int differenceMonth = (int) ChronoUnit.DAYS.between(dateLastAccrual, timeUpdated) / 30;
        for (int i = 0; i < differenceMonth; i++) {
            BigDecimal percent = this.balance.multiply(Constant.DEPOSIT_RATE);
            this.balance = this.balance.add(percent);
            this.withdrawLimit = this.maxWithdrawalLimit;
        }

        this.dateLastAccrual = this.dateLastAccrual.plusDays(differenceMonth * 30);
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SavingAccount that = (SavingAccount) o;
        return getWithdrawLimit() == that.getWithdrawLimit() &&
                maxWithdrawalLimit == that.maxWithdrawalLimit &&
                Objects.equals(dateLastAccrual, that.dateLastAccrual) &&
                Objects.equals(id, that.id) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(balance, that.balance) &&
                Objects.equals(lock, that.lock) &&
                accountType == that.accountType &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getWithdrawLimit(), maxWithdrawalLimit, dateLastAccrual);
    }
}
