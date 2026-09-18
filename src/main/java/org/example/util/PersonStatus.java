package org.example.util;

public enum PersonStatus {
    ACTIVE("ACTIVE"),
    BLOCKED("BLOCKED");

    private final String message;
    PersonStatus(String value) {
        this.message = value;
    }

    public String getMessage() {
        return message;
    }

    public static PersonStatus fromString(String value) {
        for (PersonStatus personStatus : PersonStatus.values()) {
            if (personStatus.name().equalsIgnoreCase(value)) {
                return personStatus;
            }
        }

        throw new IllegalArgumentException("Invalid value " + value);
    }
}
