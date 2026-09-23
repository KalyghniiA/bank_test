package org.example.util;

import java.math.BigDecimal;


public abstract class AccountField<T> {
    public static final AccountField<BigDecimal> BALANCE = new AccountField<>() {};
    public static final AccountField<AccountStatus> STATUS = new AccountField<>() {};
    public static final AccountField<BigDecimal> OVERDRAFT_LIMIT = new AccountField<>() {};;
    public static final AccountField<Integer> MAX_WITHDRAWAL_LIMIT = new AccountField<>() {};;
}
