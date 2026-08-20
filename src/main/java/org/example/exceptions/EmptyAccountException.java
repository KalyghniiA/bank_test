package org.example.exceptions;

public class EmptyAccountException  extends RuntimeException {
    public EmptyAccountException(String message) {
        super(String.format("Счета с id %s не существует", message));
    }
}
