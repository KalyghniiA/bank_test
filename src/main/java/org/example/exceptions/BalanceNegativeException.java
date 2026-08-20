package org.example.exceptions;

public class BalanceNegativeException extends RuntimeException {
    public BalanceNegativeException(String message) {
        super(message);
    }
}
