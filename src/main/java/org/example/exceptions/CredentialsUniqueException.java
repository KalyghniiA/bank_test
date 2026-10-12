package org.example.exceptions;

public class CredentialsUniqueException extends RuntimeException {
    public CredentialsUniqueException(String message, Throwable cause) {
        super(message, cause);
    }
}
