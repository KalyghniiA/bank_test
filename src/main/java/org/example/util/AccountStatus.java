package org.example.util;

public enum AccountStatus {
    ACTIVE("ACTIVE"),
    BLOCKED("BLOCKED"),
    DELETE("DELETE");

    private String status;

    AccountStatus(String status) {
        this.status = status;
    }

    public static AccountStatus fromString(String status) {
        for (AccountStatus type : AccountStatus.values()) {
            if (type.status.equalsIgnoreCase(status)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown status type: " + status);
    }


    public String getMessage() {
        return status;
    }
}
