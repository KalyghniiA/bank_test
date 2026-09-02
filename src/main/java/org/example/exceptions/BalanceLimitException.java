package org.example.exceptions;

public class BalanceLimitException extends RuntimeException {
    public BalanceLimitException(String message) {
        super(message);
    }
}
