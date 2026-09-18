package org.example.exceptions;

public class SQLTransactionException extends RuntimeException {
    public SQLTransactionException(String message) {
        super(message);
    }
}
