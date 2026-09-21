package org.example.util;

public enum TransactionType {
    DEPOSIT("DEPOSIT"),
    WITHDRAW("WITHDRAW"),
    TRANSFER_IN("TRANSFER_IN"),
    TRANSFER_OUT("TRANSFER_OUT");

    private String message;
    TransactionType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public static TransactionType fromString(String type) {
        for (TransactionType t : TransactionType.values()) {
            if (t.name().equalsIgnoreCase(type)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown transaction type: " + type);
    }
}
