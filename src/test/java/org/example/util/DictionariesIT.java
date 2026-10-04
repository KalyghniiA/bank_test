package org.example.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class DictionariesIT {
    private static HikariDataSource ds;
    private static Dictionaries dictionaries;
    private static final Set<String> TRANSACTION_TYPES = Set.of("DEPOSIT", "WITHDRAW", "TRANSFER_IN", "TRANSFER_OUT");
    private static final Set<String> ACCOUNT_TYPES = Set.of("DEFAULT", "SAVING", "CHECKING");
    private static final Set<String> ACCOUNT_STATUSES = Set.of("ACTIVE", "BLOCKED", "DELETE");
    private static final Set<String> PERSON_STATUSES = Set.of("ACTIVE", "BLOCKED");

    @Container
    private static final PostgreSQLContainer container = new PostgreSQLContainer("postgres:16-alpine")
            .withInitScript("schema.sql");

    @BeforeAll
    static void init() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        ds = new HikariDataSource(hikariConfig);
    }

    @AfterAll
    static void close() {
        ds.close();
    }

    @BeforeEach
    void setUp() {
        dictionaries = new Dictionaries(ds);
        dictionaries.clearDictionary();
    }

    @Test
    @DisplayName("Проверка заполнения словаря")
    void checkLoadDictionary() {
        dictionaries.loadDictionary();

        assertDictionaries(TRANSACTION_TYPES, ACCOUNT_TYPES, ACCOUNT_STATUSES, PERSON_STATUSES);
    }

    @Test
    @DisplayName("Проверка очистки словаря")
    void checkClearDictionary() {
        dictionaries.loadDictionary();
        dictionaries.clearDictionary();

        assertAll(
                () -> assertTrue(Dictionaries.statusPersonDictionary.isEmpty()),
                () -> assertTrue(Dictionaries.statusAccountDictionary.isEmpty()),
                () -> assertTrue(Dictionaries.typeAccountDictionary.isEmpty()),
                () -> assertTrue(Dictionaries.typeTransactionDictionary.isEmpty())
        );
    }

    @Test
    @DisplayName("Проверка консистенций данных")
    void checkReloadReflectsDatabaseChanges() throws SQLException {
        executeBatch(
                "insert into transaction_type (name) values ('TEST')",
                "insert into bank_account_type (name) values ('TEST')",
                "insert into bank_account_status (name) values ('TEST')",
                "insert into person_status (name) values ('TEST')"
        );

        try {
            dictionaries.loadDictionary();
            assertDictionaries(with(TRANSACTION_TYPES, "TEST"), with(ACCOUNT_TYPES, "TEST"), with(ACCOUNT_STATUSES, "TEST"), with(PERSON_STATUSES, "TEST"));
        } finally {
            deleteTestRows();
        }

        dictionaries.loadDictionary();
        assertDictionaries(TRANSACTION_TYPES, ACCOUNT_TYPES, ACCOUNT_STATUSES, PERSON_STATUSES);

    }

    private static void assertDictionaries(Set<String> transactionTypes, Set<String> accountTypes,
                                           Set<String> accountStatuses, Set<String> personStatuses) {
        assertAll(
                () -> assertEquals(transactionTypes, Set.copyOf(Dictionaries.typeTransactionDictionary.values())),
                () -> assertEquals(accountTypes, Set.copyOf(Dictionaries.typeAccountDictionary.values())),
                () -> assertEquals(accountStatuses, Set.copyOf(Dictionaries.statusAccountDictionary.values())),
                () -> assertEquals(personStatuses, Set.copyOf(Dictionaries.statusPersonDictionary.values()))
        );
    }

    private static Set<String> with(Set<String> base, String extra) {
        Set<String> result = new HashSet<>(base);
        result.add(extra);
        return result;
    }

    private static void deleteTestRows() throws SQLException {
        executeBatch(
                "delete from transaction_type where name = 'TEST';",
                "delete from  bank_account_type where name = 'TEST';",
                "delete from  bank_account_status where name = 'TEST';",
                "delete from person_status where name = 'TEST';"
        );
    }

    private static void executeBatch(String... sqls) throws SQLException {
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : sqls) {
                stmt.addBatch(sql);
            }
            stmt.executeBatch();
        }
    }
}
