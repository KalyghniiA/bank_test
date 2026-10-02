package org.example.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.model.BankAccount;
import org.example.model.CheckingAccount;
import org.example.model.SavingAccount;
import org.example.util.AccountStatus;
import org.example.util.AccountType;
import org.example.util.Dictionaries;
import org.example.util.PersonStatus;
import org.example.util.transaction_manager.ConnectionHolder;
import org.example.util.transaction_manager.JDBCTransactionManager;
import org.example.util.transaction_manager.TransactionManager;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class JDBCBankAccountRepositoryIT {
    private static HikariDataSource ds;
    private static Dictionaries dictionaries;
    private static JDBCBankAccountRepository bankAccountRepository;
    private static UUID person1Id;
    private static UUID person2Id;
    private static int personStatusActiveId;
    private static TransactionManager transactionManager;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16-alpine")
            .withInitScript("schema.sql");

    @BeforeAll
    static void init() throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(postgreSQLContainer.getJdbcUrl());
        hikariConfig.setUsername(postgreSQLContainer.getUsername());
        hikariConfig.setPassword(postgreSQLContainer.getPassword());
        ds = new HikariDataSource(hikariConfig);
        person1Id = UUID.randomUUID();
        person2Id = UUID.randomUUID();

        dictionaries = new Dictionaries(ds);
        dictionaries.loadDictionary();

        personStatusActiveId = Dictionaries.statusPersonDictionary
                .entrySet()
                .stream()
                .filter(statusEntry -> statusEntry.getValue().equals(PersonStatus.ACTIVE.toString()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Person status not found"));

        bankAccountRepository = new JDBCBankAccountRepository();

        transactionManager = new JDBCTransactionManager(ds);


        executeUpdate(
                String.format("insert into person (id, first_name, surname, birth_date, status) values ('%s', 'first_name_1', 'sur_name_1', '1990-12-01', %d)", person1Id.toString(), personStatusActiveId),
                String.format("insert into person (id, first_name, surname, birth_date, status) values ('%s', 'first_name_2', 'sur_name_2', '1990-12-01', %d)", person2Id.toString(), personStatusActiveId)
        );

    }

    @AfterAll
    static void destroy() {
        ds.close();
    }

    @BeforeEach
    void setUp() throws SQLException {
        executeUpdate("truncate \"transaction\" restart identity cascade;",
                "truncate bank_account restart identity cascade;",
                "truncate credentials restart identity cascade;",
                "truncate checking_account_details restart identity cascade;",
                "truncate saving_account_details restart identity cascade;");
    }

    @Test
    @DisplayName("Создание счета")
    void checkSaveAccountDefault() throws SQLException {
        BankAccount bankAccount = new BankAccount(person1Id);
        int expectedCount = 1;
        int accountTypeId = getAccountTypeId(AccountType.DEFAULT);
        int accountStatusId = getAccountStatusId(AccountStatus.ACTIVE);
        Map<String, Object> answer = new HashMap<>();


        transactionManager.runInTransaction(() -> bankAccountRepository.save(bankAccount.getId(), bankAccount));

        String sqlGet = "select id, type, person_id, balance, status from bank_account where person_id = ?";
        try (Connection conn = ds.getConnection(); PreparedStatement ps = conn.prepareStatement(sqlGet)) {
            ps.setObject(1, person1Id);
            ResultSet rs = ps.executeQuery();
            int row = 0;
            while (rs.next()) {
                answer.put("id", UUID.fromString(rs.getString("id")));
                answer.put("type", rs.getInt("type"));
                answer.put("person_id", UUID.fromString(rs.getString("person_id")));
                answer.put("balance", rs.getBigDecimal("balance"));
                answer.put("status", rs.getInt("status"));
                row++;
            }
            answer.put("count", row);

        }

        assertAll(
                () -> assertEquals(expectedCount, answer.get("count")),
                () -> assertEquals(bankAccount.getId(), answer.get("id")),
                () -> assertEquals(accountTypeId, answer.get("type")),
                () -> assertEquals(person1Id, answer.get("person_id")),
                () -> assertEquals(0, bankAccount.getBalance().compareTo((BigDecimal) answer.get("balance"))),
                () -> assertEquals(accountStatusId, answer.get("status"))
        );

    }

    @Test
    @DisplayName("Создание депозитного счета")
    void checkSaveAccountSaving() throws SQLException {
        SavingAccount savingAccount = new SavingAccount(person1Id);
        int expectedCount = 1;
        int accountTypeId = getAccountTypeId(AccountType.SAVING);
        int accountStatusId = getAccountStatusId(AccountStatus.ACTIVE);
        Map<String,Object> answer = new HashMap<>();

        transactionManager.runInTransaction(() -> bankAccountRepository.save(savingAccount.getId(), savingAccount));

        String sqlGet = """
                select bank_account.id as id, type, person_id, balance, status, withdraw_limit, max_withdraw_limit, date_last_accrual from bank_account
                    full join saving_account_details on bank_account.id = saving_account_details.account_id
                where person_id = ?;
                """;
        try (Connection conn = ds.getConnection(); PreparedStatement ps = conn.prepareStatement(sqlGet)) {
            ps.setObject(1, person1Id);
            ResultSet rs = ps.executeQuery();
            int row = 0;
            while (rs.next()) {
                answer.put("id", UUID.fromString(rs.getString("id")));
                answer.put("type", rs.getInt("type"));
                answer.put("person_id", UUID.fromString(rs.getString("person_id")));
                answer.put("balance", rs.getBigDecimal("balance"));
                answer.put("status", rs.getInt("status"));
                answer.put("withdraw_limit", rs.getInt("withdraw_limit"));
                answer.put("max_withdraw_limit", rs.getInt("max_withdraw_limit"));
                answer.put("date_last_accrual", rs.getTimestamp("date_last_accrual").toLocalDateTime());//вот тут не уверен, так как может быть npe
                row++;
            }

            answer.put("count", row);
        }

        assertAll(
                () -> assertEquals(expectedCount, answer.get("count")),
                () -> assertEquals(savingAccount.getId(), answer.get("id")),
                () -> assertEquals(accountTypeId, answer.get("type")),
                () -> assertEquals(person1Id, answer.get("person_id")),
                () -> assertEquals(0, savingAccount.getBalance().compareTo((BigDecimal) answer.get("balance"))),
                () -> assertEquals(accountStatusId, answer.get("status")),
                () -> assertEquals(savingAccount.getWithdrawLimit(), answer.get("withdraw_limit")),
                () -> assertEquals(savingAccount.getMaxWithdrawalLimit(), answer.get("max_withdraw_limit")),
                () -> assertTrue(savingAccount.getDateLastAccrual().isEqual((LocalDateTime) answer.get("date_last_accrual")))
        );

    }

    @Test
    @DisplayName("Проверка создания счета с овердрафтом")
    void checkSaveCheckingAccount() throws SQLException {
        CheckingAccount checkingAccount = new CheckingAccount(person1Id);
        int expectedCount = 1;
        int accountTypeId = getAccountTypeId(AccountType.CHECKING);
        int accountStatusId = getAccountStatusId(AccountStatus.ACTIVE);
        Map<String,Object> answer = new HashMap<>();

        transactionManager.runInTransaction(() -> bankAccountRepository.save(checkingAccount.getId(), checkingAccount));

        String sql = """
                select bank_account.id as id, type, person_id, balance, status, overdraft_limit from bank_account
                full join checking_account_details on bank_account.id = checking_account_details.account_id
                where person_id = ?;
                """;

        try (Connection conn = ds.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, person1Id);
            ResultSet rs = ps.executeQuery();
            int row = 0;
            while (rs.next()) {
                answer.put("id", UUID.fromString(rs.getString("id")));
                answer.put("type", rs.getInt("type"));
                answer.put("person_id", UUID.fromString(rs.getString("person_id")));
                answer.put("balance", rs.getBigDecimal("balance"));
                answer.put("status", rs.getInt("status"));
                answer.put("overdraft_limit", rs.getBigDecimal("overdraft_limit"));
                row++;
            }

            answer.put("count", row);
        }

        assertAll(
                () -> assertEquals(expectedCount, answer.get("count")),
                () -> assertEquals(checkingAccount.getId(), answer.get("id")),
                () -> assertEquals(accountTypeId, answer.get("type")),
                () -> assertEquals(person1Id, answer.get("person_id")),
                () -> assertEquals(0, checkingAccount.getBalance().compareTo((BigDecimal) answer.get("balance"))),
                () -> assertEquals(accountStatusId, answer.get("status")),
                () -> assertEquals(0, checkingAccount.getOverdraftLimit().compareTo((BigDecimal) answer.get("overdraft_limit")))
        );

    }

    private static void executeUpdate(String... sqls) throws SQLException {
        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            for (String sql : sqls) {
                st.addBatch(sql);
            }
            st.executeBatch();
        }
    }

    private static int getAccountTypeId(AccountType accountType) {
        return Dictionaries.typeAccountDictionary.entrySet()
                .stream()
                .filter(typeEntry -> typeEntry.getValue().equals(accountType.getMessage()))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new RuntimeException("Account type not found"));
    }

    private static int getAccountStatusId(AccountStatus accountStatus) {
        return Dictionaries.statusAccountDictionary.entrySet()
                .stream()
                .filter(statusEntry -> statusEntry.getValue().equals(accountStatus.getMessage()))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new RuntimeException("Account status not found"));
    }
}
