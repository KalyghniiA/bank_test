package org.example.model;

import org.example.exceptions.BalanceLimitException;
import org.example.util.Constant;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class SavingAccount extends BankAccount implements InterestBearingAccount{
    private int withdrawLimit;
    private final int maxWithdrawalLimit;
    private LocalDate dateLastAccrual = LocalDate.now();

    public SavingAccount(UUID userId) {
        super(userId);
        this.withdrawLimit = Constant.MAX_WITHDRAWALS;
        this.maxWithdrawalLimit = Constant.MAX_WITHDRAWALS;
    }

    public SavingAccount(UUID userId, int withdrawLimit) {
        super(userId);
        this.withdrawLimit = withdrawLimit;
        this.maxWithdrawalLimit = withdrawLimit;
    }

    public SavingAccount(UUID userId, BigDecimal balance) {
        super(userId, balance);
        this.withdrawLimit = Constant.MAX_WITHDRAWALS;
        this.maxWithdrawalLimit = Constant.MAX_WITHDRAWALS;
    }

    public SavingAccount(UUID userId, BigDecimal balance, int withdrawLimit) {
        super(userId, balance);
        this.withdrawLimit = withdrawLimit;
        this.maxWithdrawalLimit = withdrawLimit;
    }

    public SavingAccount(UUID id, UUID userId, BigDecimal balance) {
        super(id, userId, balance);
        this.withdrawLimit = Constant.MAX_WITHDRAWALS;
        this.maxWithdrawalLimit = Constant.MAX_WITHDRAWALS;
    }

    public SavingAccount(UUID id, UUID userId, BigDecimal balance, int withdrawLimit) {
        super(id, userId, balance);
        this.withdrawLimit = withdrawLimit;
        this.maxWithdrawalLimit = withdrawLimit;
    }

    public SavingAccount(UUID id, UUID userId, BigDecimal balance, int withdrawLimit, LocalDate dateLastAccrual) {
        this(id, userId, balance, withdrawLimit);
        this.dateLastAccrual = dateLastAccrual;
    }

    public int getWithdrawLimit() {
        return withdrawLimit;
    }

    @Override
    public void accrueInterestIfDue(Clock clock) {
        LocalDate timeUpdated = LocalDate.now(clock);
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
}
