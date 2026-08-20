package org.example.exceptions;

public class EmptyPersonException extends RuntimeException {
    public EmptyPersonException(String message) {
        super(message);
    }
}
