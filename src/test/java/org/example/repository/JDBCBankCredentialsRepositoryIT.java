package org.example.repository;

import org.example.exceptions.RepositoryException;
import org.example.exceptions.SQLTransactionException;
import org.example.model.Credentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class JDBCBankCredentialsRepositoryIT extends AbstractRepositoryIT {
    private static final byte[] HASH = "hash-bytes".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SALT = "salt-bytes-16-by".getBytes(StandardCharsets.UTF_8);

    private final JDBCBankCredentialsRepository credentialsRepository = new JDBCBankCredentialsRepository();

    @BeforeEach
    void setUp() throws SQLException {
        executeUpdate("truncate credentials");
    }

    @Test
    @DisplayName("save + get: учётные данные сохраняются вместе с байтами хеша и соли")
    void checkSaveAndGet() {
        Credentials credentials = new Credentials(PERSON_1, "ivan", HASH, SALT, 1000);

        inTransaction(() -> credentialsRepository.save(credentials.id(), credentials));

        Credentials answer = inTransaction(() -> credentialsRepository.get(credentials.id())).orElseThrow();
        assertAll(
                () -> assertEquals(credentials, answer),
                () -> assertArrayEquals(HASH, answer.passwordHash()),
                () -> assertArrayEquals(SALT, answer.salt())
        );
    }

    @Test
    @DisplayName("getByLogin и getByPersonId находят свои учётные данные")
    void checkGetByLoginAndPersonId() throws SQLException {
        Credentials ivan = new Credentials(PERSON_1, "ivan", HASH, SALT, 1000);
        Credentials anna = new Credentials(PERSON_2, "anna", HASH, SALT, 1000);
        insertCredentials(ivan);
        insertCredentials(anna);

        Credentials byLogin = inTransaction(() -> credentialsRepository.getByLogin("anna")).orElseThrow();
        Credentials byPerson = inTransaction(() -> credentialsRepository.getByPersonId(PERSON_1)).orElseThrow();

        assertAll(
                () -> assertEquals(anna, byLogin),
                () -> assertEquals(ivan, byPerson)
        );
    }

    @Test
    @DisplayName("Поиск несуществующих учётных данных — пустой Optional")
    void checkGetUnknown() {
        Optional<Credentials> byId = inTransaction(() -> credentialsRepository.get(UUID.randomUUID()));
        Optional<Credentials> byLogin = inTransaction(() -> credentialsRepository.getByLogin("nobody"));
        Optional<Credentials> byPerson = inTransaction(() -> credentialsRepository.getByPersonId(PERSON_2));

        assertAll(
                () -> assertTrue(byId.isEmpty()),
                () -> assertTrue(byLogin.isEmpty()),
                () -> assertTrue(byPerson.isEmpty())
        );
    }

    @Test
    @DisplayName("Повторный логин — RepositoryException с причиной 23505")
    void checkSaveDuplicateLogin() throws SQLException {
        insertCredentials(new Credentials(PERSON_1, "ivan", HASH, SALT, 1000));
        Credentials sameLogin = new Credentials(PERSON_2, "ivan", HASH, SALT, 1000);

        RepositoryException exception = assertThrows(RepositoryException.class,
                () -> inTransaction(() -> credentialsRepository.save(sameLogin.id(), sameLogin)));

        assertEquals("23505", sqlState(exception));
    }

    @Test
    @DisplayName("Вторые учётные данные для того же клиента — RepositoryException с причиной 23505")
    void checkSaveSecondCredentialsForPerson() throws SQLException {
        insertCredentials(new Credentials(PERSON_1, "ivan", HASH, SALT, 1000));
        Credentials second = new Credentials(PERSON_1, "ivan2", HASH, SALT, 1000);

        RepositoryException exception = assertThrows(RepositoryException.class,
                () -> inTransaction(() -> credentialsRepository.save(second.id(), second)));

        assertEquals("23505", sqlState(exception));
    }

    @Test
    @DisplayName("Учётные данные несуществующего клиента — RepositoryException с причиной 23503")
    void checkSaveForUnknownPerson() {
        Credentials orphan = new Credentials(UUID.randomUUID(), "ghost", HASH, SALT, 1000);

        RepositoryException exception = assertThrows(RepositoryException.class,
                () -> inTransaction(() -> credentialsRepository.save(orphan.id(), orphan)));

        assertEquals("23503", sqlState(exception));
    }

    @Test
    @DisplayName("update меняет логин, хеш, соль и число итераций")
    void checkUpdate() throws SQLException {
        Credentials credentials = new Credentials(PERSON_1, "ivan", HASH, SALT, 1000);
        insertCredentials(credentials);
        byte[] newHash = "new-hash".getBytes(StandardCharsets.UTF_8);
        byte[] newSalt = "new-salt-16-byte".getBytes(StandardCharsets.UTF_8);
        Credentials changed = new Credentials(credentials.id(), PERSON_1, "ivan_new", newHash, newSalt, 2000);

        inTransaction(() -> {
            Credentials current = credentialsRepository.get(credentials.id()).orElseThrow();
            credentialsRepository.update(current, changed);
        });

        Credentials answer = inTransaction(() -> credentialsRepository.get(credentials.id())).orElseThrow();
        assertEquals(changed, answer);
    }

    @Test
    @DisplayName("update по устаревшей версии — SQLTransactionException, данные не меняются")
    void checkUpdateRejectsStaleVersion() throws SQLException {
        Credentials credentials = new Credentials(PERSON_1, "ivan", HASH, SALT, 1000);
        insertCredentials(credentials);
        Credentials stale = new Credentials(credentials.id(), PERSON_1, "old_login", HASH, SALT, 1000);
        Credentials changed = new Credentials(credentials.id(), PERSON_1, "hacker", HASH, SALT, 1000);

        assertThrows(SQLTransactionException.class,
                () -> inTransaction(() -> credentialsRepository.update(stale, changed)));

        assertEquals("ivan", inTransaction(() -> credentialsRepository.get(credentials.id())).orElseThrow().login());
    }

    @Test
    @DisplayName("delete и getAll не поддерживаются")
    void checkUnsupportedOperations() {
        assertAll(
                () -> assertThrows(UnsupportedOperationException.class, () -> credentialsRepository.delete(UUID.randomUUID())),
                () -> assertThrows(UnsupportedOperationException.class, credentialsRepository::getAll)
        );
    }

    // ---------- вспомогательные методы ----------

    private static String sqlState(RepositoryException exception) {
        return assertInstanceOf(PSQLException.class, exception.getCause()).getSQLState();
    }

    private static void insertCredentials(Credentials credentials) throws SQLException {
        String sql = "insert into credentials (id, person_id, login, password_hash, salt, iterations) values (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DATA_SOURCE.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, credentials.id());
            ps.setObject(2, credentials.personId());
            ps.setString(3, credentials.login());
            ps.setBytes(4, credentials.passwordHash());
            ps.setBytes(5, credentials.salt());
            ps.setInt(6, credentials.iterations());
            ps.executeUpdate();
        }
    }
}
