package org.example.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record Credentials(UUID id, UUID personId, String login, byte[] passwordHash, byte[] salt, int iterations) {
    public Credentials(UUID personId, String login, byte[] passwordHash, byte[] salt, int iterations) {
        this(UUID.randomUUID(), personId, login, passwordHash, salt, iterations);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Credentials that = (Credentials) o;
        return iterations() == that.iterations() && Objects.equals(id(), that.id()) && Objects.equals(personId(), that.personId()) && Objects.equals(login(), that.login()) && Objects.deepEquals(passwordHash(), that.passwordHash()) && Objects.deepEquals(salt(), that.salt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id(), personId(), login(), Arrays.hashCode(passwordHash()), Arrays.hashCode(salt()), iterations());
    }

    @Override
    public String toString() {
        return "Credentials{" +
                "id=" + id +
                ", personId=" + personId +
                ", login='" + login + '\'' +
                ", passwordHash=" + Arrays.toString(passwordHash) +
                ", salt=" + Arrays.toString(salt) +
                ", iterations=" + iterations +
                '}';
    }
}
