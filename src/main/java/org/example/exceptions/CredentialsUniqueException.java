package org.example.exceptions;

public class CredentialsUniqueException extends RuntimeException {
    public CredentialsUniqueException(String message) {
        super(message);
    }
}
