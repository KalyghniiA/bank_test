package org.example.sender;

import org.example.model.Person;

public interface NotificationSender {
    void send(Person person, String message);
}
