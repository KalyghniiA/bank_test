package org.example.exceptions;

public class PersonAuthException extends RuntimeException {
    public PersonAuthException(String message) {
        super(message);
    }
}
