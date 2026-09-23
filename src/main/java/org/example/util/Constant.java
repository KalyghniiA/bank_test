package org.example.util;

import java.math.BigDecimal;


public class Constant {
    public static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.16");
    public static final int MAX_WITHDRAWALS = 5;
    public static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final int HASH_ITERATIONS = 600_000;
    public static final int SALT_SIZE = 32;
    public static final int KEY_SIZE = 256;
}
