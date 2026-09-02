package org.example.exceptions;

public class TransactionTypeException extends RuntimeException {
    public TransactionTypeException(String message) {
        super(message);
    }
}
