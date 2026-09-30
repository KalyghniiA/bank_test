package org.example.repository;

import org.example.model.Credentials;

import java.util.Optional;
import java.util.UUID;

public interface CredentialsRepository<ID, T> extends Repository<ID, T> {
    Optional<Credentials> getByPersonId(UUID personId);
    Optional<Credentials> getByLogin(String login);
}
