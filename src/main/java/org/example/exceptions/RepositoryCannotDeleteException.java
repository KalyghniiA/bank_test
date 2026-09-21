package org.example.exceptions;

public class RepositoryCannotDeleteException extends RuntimeException {
    public RepositoryCannotDeleteException(String message) {
        super(message);
    }
}
