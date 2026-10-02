package org.example.exceptions;

public class TransactionManagerSQLException extends RuntimeException {
    public TransactionManagerSQLException(String message) {
        super(message);
    }

    public TransactionManagerSQLException(String message, Throwable cause) {
        super(message, cause);
    }
}
