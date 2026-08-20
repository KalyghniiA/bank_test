package org.example.exceptions;

public class NegativeBalanceException extends RuntimeException {
  public NegativeBalanceException(String message) {
    super(message);
  }
}
