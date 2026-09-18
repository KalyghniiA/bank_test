package org.example.util;

public enum AccountType {
    DEFAULT("DEFAULT"),
    SAVING("SAVING"),
    CHECKING("CHECKING");

    private final String type;
    AccountType(String type) {

        this.type = type;
    }

    public static AccountType fromString(String type) {
        for (AccountType accountType : AccountType.values()) {
            if (accountType.type.equals(type)) {
                return accountType;
            }
        }
        throw new IllegalArgumentException("Unknown account type: " + type);
    }

    public String getMessage() {
        return type;
    }


}
