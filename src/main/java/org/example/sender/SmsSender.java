package org.example.sender;

import org.example.model.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class SmsSender implements NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(SmsSender.class);

    @Override
    public void send(Person person, String message) {
        log.info("[SMS]: {}", message);
    }
}
