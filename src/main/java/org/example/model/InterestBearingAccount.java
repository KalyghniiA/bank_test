package org.example.model;

import java.time.Clock;

public interface InterestBearingAccount {
    void accrueInterestIfDue(Clock clock);
}
