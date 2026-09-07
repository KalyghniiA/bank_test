package org.example.util;

import org.example.model.BankAccount;
import org.example.model.InterestBearingAccount;

import java.time.Clock;

public class InterestAccrualService {
    public static void accrueIfDue(BankAccount bankAccount, Clock clock) {
        if (bankAccount instanceof InterestBearingAccount interestBearingAccount) {
            interestBearingAccount.accrueInterestIfDue(clock);
        }
    }
}
