package org.example.exceptions;

public class TransactionParamsException extends RuntimeException {
    public TransactionParamsException(String message) {
        super(message);
    }
}
