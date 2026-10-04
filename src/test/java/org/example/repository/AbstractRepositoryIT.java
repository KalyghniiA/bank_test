package org.example.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.util.Dictionaries;
import org.example.util.transaction_manager.JDBCTransactionManager;
import org.example.util.transaction_manager.TransactionManager;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Общая инфраструктура интеграционных тестов репозиториев.
 *
 * <p>Контейнер PostgreSQL поднимается один раз на весь запуск тестов (паттерн «singleton container»)
 * и останавливается автоматически при завершении JVM. Перед каждым тестовым классом
 * {@link #resetDatabase()} очищает все таблицы с данными и заново создаёт двух клиентов-фикстур,
 * поэтому классы не зависят друг от друга и от порядка запуска. Справочники не очищаются:
 * они заполняются из {@code schema.sql}.
 *
 * <p>Репозитории вызываются только внутри {@link #inTransaction}: они берут соединение
 * из {@code ConnectionHolder}, который заполняет настоящий {@link JDBCTransactionManager}.
 */
public abstract class AbstractRepositoryIT {
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withInitScript("schema.sql");
    protected static final HikariDataSource DATA_SOURCE;
    protected static final TransactionManager TRANSACTION_MANAGER;

    protected static final UUID PERSON_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    protected static final UUID PERSON_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

    static {
        POSTGRES.start();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword());
        config.setMaximumPoolSize(5);
        DATA_SOURCE = new HikariDataSource(config);

        TRANSACTION_MANAGER = new JDBCTransactionManager(DATA_SOURCE);
    }

    @BeforeAll
    static void resetDatabase() throws SQLException {
        // Словари статические и общие для всей JVM: другой тестовый класс мог их очистить.
        new Dictionaries(DATA_SOURCE).loadDictionary();

        executeUpdate("truncate \"transaction\", saving_account_details, checking_account_details, "
                + "bank_account, credentials, person cascade");
        insertPerson(PERSON_1, "Иван", "Петров");
        insertPerson(PERSON_2, "Анна", "Смирнова");
    }

    protected static void inTransaction(Runnable action) {
        TRANSACTION_MANAGER.runInTransaction(action);
    }

    protected static <T> T inTransaction(Supplier<T> action) {
        return TRANSACTION_MANAGER.runInTransaction(action);
    }

    protected static void executeUpdate(String... sqls) throws SQLException {
        try (Connection conn = DATA_SOURCE.getConnection(); Statement statement = conn.createStatement()) {
            for (String sql : sqls) {
                statement.addBatch(sql);
            }
            statement.executeBatch();
        }
    }

    protected static void insertPerson(UUID id, String firstName, String surname) throws SQLException {
        executeUpdate(String.format(
                "insert into person (id, first_name, surname, birth_date, status) values ('%s', '%s', '%s', '1990-12-01', %d)",
                id, firstName, surname, idOf(Dictionaries.statusPersonDictionary, "ACTIVE")));
    }

    protected static int idOf(Map<Integer, String> dictionary, String name) {
        return dictionary.entrySet().stream()
                .filter(entry -> entry.getValue().equals(name))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("В словаре нет значения " + name));
    }
}
