package org.example.exceptions;

public class RepositoryItemExistsException extends RuntimeException {
    public RepositoryItemExistsException(String message) {
        super(message);
    }
}
