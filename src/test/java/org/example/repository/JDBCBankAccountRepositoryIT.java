package org.example.repository;

import org.example.exceptions.RepositoryItemExistsException;
import org.example.exceptions.RepositoryParamException;
import org.example.exceptions.SQLTransactionException;
import org.example.model.BankAccount;
import org.example.model.CheckingAccount;
import org.example.model.SavingAccount;
import org.example.util.AccountStatus;
import org.example.util.AccountType;
import org.example.util.Dictionaries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class JDBCBankAccountRepositoryIT extends AbstractRepositoryIT {
    /**
     * Фиксированная дата вместо LocalDateTime.now(): PostgreSQL хранит timestamp с точностью до микросекунд,
     * а now() на Linux даёт наносекунды — сравнение «как сохранили, так и прочитали» стало бы случайным.
     */
    private static final LocalDateTime ACCRUAL_DATE = LocalDateTime.of(2026, 1, 15, 10, 30, 45, 123_456_000);

    private final JDBCBankAccountRepository bankAccountRepository = new JDBCBankAccountRepository();

    @BeforeEach
    void setUp() throws SQLException {
        executeUpdate("truncate \"transaction\", saving_account_details, checking_account_details, bank_account cascade");
    }

    // ---------------------------------- save ----------------------------------

    @Test
    @DisplayName("Создание счета")
    void checkSaveAccountDefault() throws SQLException {
        BankAccount bankAccount = new BankAccount(PERSON_1);
        int expectedCount = 1;
        int accountTypeId = getAccountTypeId(AccountType.DEFAULT);
        int accountStatusId = getAccountStatusId(AccountStatus.ACTIVE);
        Map<String, Object> answer = new HashMap<>();

        inTransaction(() -> bankAccountRepository.save(bankAccount.getId(), bankAccount));

        String sqlGet = "select id, type, person_id, balance, status from bank_account where person_id = ?";
        try (Connection conn = DATA_SOURCE.getConnection(); PreparedStatement ps = conn.prepareStatement(sqlGet)) {
            ps.setObject(1, PERSON_1);
            ResultSet rs = ps.executeQuery();
            int row = 0;
            while (rs.next()) {
                answer.put("id", rs.getObject("id", UUID.class));
                answer.put("type", rs.getInt("type"));
                answer.put("person_id", rs.getObject("person_id", UUID.class));
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
                () -> assertEquals(PERSON_1, answer.get("person_id")),
                () -> assertEquals(0, bankAccount.getBalance().compareTo((BigDecimal) answer.get("balance"))),
                () -> assertEquals(accountStatusId, answer.get("status"))
        );
    }

    @Test
    @DisplayName("Создание депозитного счета")
    void checkSaveAccountSaving() throws SQLException {
        SavingAccount savingAccount = new SavingAccount(UUID.randomUUID(), PERSON_1, new BigDecimal("500"), 3, 5, ACCRUAL_DATE);
        int expectedCount = 1;
        int accountTypeId = getAccountTypeId(AccountType.SAVING);
        int accountStatusId = getAccountStatusId(AccountStatus.ACTIVE);
        Map<String, Object> answer = new HashMap<>();

        inTransaction(() -> bankAccountRepository.save(savingAccount.getId(), savingAccount));

        String sqlGet = """
                select bank_account.id as id, type, person_id, balance, status, withdraw_limit, max_withdraw_limit, date_last_accrual from bank_account
                    left join saving_account_details on bank_account.id = saving_account_details.account_id
                where person_id = ?;
                """;
        try (Connection conn = DATA_SOURCE.getConnection(); PreparedStatement ps = conn.prepareStatement(sqlGet)) {
            ps.setObject(1, PERSON_1);
            ResultSet rs = ps.executeQuery();
            int row = 0;
            while (rs.next()) {
                answer.put("id", rs.getObject("id", UUID.class));
                answer.put("type", rs.getInt("type"));
                answer.put("person_id", rs.getObject("person_id", UUID.class));
                answer.put("balance", rs.getBigDecimal("balance"));
                answer.put("status", rs.getInt("status"));
                answer.put("withdraw_limit", rs.getObject("withdraw_limit", Integer.class));
                answer.put("max_withdraw_limit", rs.getObject("max_withdraw_limit", Integer.class));
                // getObject вернёт null, если строки деталей нет, — без NPE, с понятным сообщением в assert
                answer.put("date_last_accrual", rs.getObject("date_last_accrual", LocalDateTime.class));
                row++;
            }
            answer.put("count", row);
        }

        assertAll(
                () -> assertEquals(expectedCount, answer.get("count")),
                () -> assertEquals(savingAccount.getId(), answer.get("id")),
                () -> assertEquals(accountTypeId, answer.get("type")),
                () -> assertEquals(PERSON_1, answer.get("person_id")),
                () -> assertEquals(0, savingAccount.getBalance().compareTo((BigDecimal) answer.get("balance"))),
                () -> assertEquals(accountStatusId, answer.get("status")),
                () -> assertEquals(savingAccount.getWithdrawLimit(), answer.get("withdraw_limit")),
                () -> assertEquals(savingAccount.getMaxWithdrawalLimit(), answer.get("max_withdraw_limit")),
                () -> assertEquals(ACCRUAL_DATE, answer.get("date_last_accrual"))
        );
    }

    @Test
    @DisplayName("Проверка создания счета с овердрафтом")
    void checkSaveCheckingAccount() throws SQLException {
        CheckingAccount checkingAccount = new CheckingAccount(PERSON_1);
        int expectedCount = 1;
        int accountTypeId = getAccountTypeId(AccountType.CHECKING);
        int accountStatusId = getAccountStatusId(AccountStatus.ACTIVE);
        Map<String, Object> answer = new HashMap<>();

        inTransaction(() -> bankAccountRepository.save(checkingAccount.getId(), checkingAccount));

        String sql = """
                select bank_account.id as id, type, person_id, balance, status, overdraft_limit from bank_account
                left join checking_account_details on bank_account.id = checking_account_details.account_id
                where person_id = ?;
                """;
        try (Connection conn = DATA_SOURCE.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, PERSON_1);
            ResultSet rs = ps.executeQuery();
            int row = 0;
            while (rs.next()) {
                answer.put("id", rs.getObject("id", UUID.class));
                answer.put("type", rs.getInt("type"));
                answer.put("person_id", rs.getObject("person_id", UUID.class));
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
                () -> assertEquals(PERSON_1, answer.get("person_id")),
                () -> assertEquals(0, checkingAccount.getBalance().compareTo((BigDecimal) answer.get("balance"))),
                () -> assertEquals(accountStatusId, answer.get("status")),
                () -> assertNotNull(answer.get("overdraft_limit"), "Строка checking_account_details не создана"),
                () -> assertEquals(0, checkingAccount.getOverdraftLimit().compareTo((BigDecimal) answer.get("overdraft_limit")))
        );
    }

    @Test
    @DisplayName("Тип SAVING у объекта, который не SavingAccount, — исключение, в базе ничего не остаётся")
    void checkSaveRejectsTypeAndClassMismatch() throws SQLException {
        BankAccount inconsistent = new BankAccount(UUID.randomUUID(), PERSON_1, BigDecimal.ZERO, AccountType.SAVING);

        assertThrows(RepositoryParamException.class,
                () -> inTransaction(() -> bankAccountRepository.save(inconsistent.getId(), inconsistent)));

        assertFalse(accountExists(inconsistent.getId()), "Счёт без деталей остался в базе");
    }

    // ---------------------------------- get ----------------------------------

    @Test
    @DisplayName("Получение простого счета")
    void checkGetDefaultAccount() throws SQLException {
        BankAccount bankAccount = new BankAccount(PERSON_1);
        insertAccount(bankAccount);

        BankAccount bankAccountAnswer = inTransaction(() ->
                bankAccountRepository.get(bankAccount.getId()).orElseThrow(() -> new RepositoryItemExistsException("Счет не сохранился")));

        assertAll(
                () -> assertEquals(BankAccount.class, bankAccountAnswer.getClass()),
                () -> assertEquals(bankAccount.getId(), bankAccountAnswer.getId()),
                () -> assertEquals(bankAccount.getUserId(), bankAccountAnswer.getUserId()),
                () -> assertEquals(0, bankAccount.getBalance().compareTo(bankAccountAnswer.getBalance())),
                () -> assertEquals(bankAccount.getStatus(), bankAccountAnswer.getStatus()),
                () -> assertEquals(bankAccount.getAccountType(), bankAccountAnswer.getAccountType())
        );
    }

    @Test
    @DisplayName("Получение сберегательного счета")
    void checkGetSavingAccount() throws SQLException {
        SavingAccount bankAccount = new SavingAccount(UUID.randomUUID(), PERSON_1, new BigDecimal("300"), 2, 4, ACCRUAL_DATE);
        insertAccount(bankAccount);
        insertSavingDetails(bankAccount);

        BankAccount answer = inTransaction(() ->
                bankAccountRepository.get(bankAccount.getId()).orElseThrow(() -> new RepositoryItemExistsException("Счет не сохранился")));

        SavingAccount bankAccountAnswer = assertInstanceOf(SavingAccount.class, answer);
        assertAll(
                () -> assertEquals(bankAccount.getId(), bankAccountAnswer.getId()),
                () -> assertEquals(bankAccount.getUserId(), bankAccountAnswer.getUserId()),
                () -> assertEquals(0, bankAccount.getBalance().compareTo(bankAccountAnswer.getBalance())),
                () -> assertEquals(bankAccount.getStatus(), bankAccountAnswer.getStatus()),
                () -> assertEquals(bankAccount.getAccountType(), bankAccountAnswer.getAccountType()),
                () -> assertEquals(bankAccount.getWithdrawLimit(), bankAccountAnswer.getWithdrawLimit()),
                () -> assertEquals(bankAccount.getMaxWithdrawalLimit(), bankAccountAnswer.getMaxWithdrawalLimit()),
                () -> assertEquals(ACCRUAL_DATE, bankAccountAnswer.getDateLastAccrual())
        );
    }

    @Test
    @DisplayName("Получение счета с овердрафтом")
    void checkGetCheckingAccount() throws SQLException {
        CheckingAccount bankAccount = new CheckingAccount(PERSON_1);
        insertAccount(bankAccount);
        insertCheckingDetails(bankAccount);

        BankAccount answer = inTransaction(() ->
                bankAccountRepository.get(bankAccount.getId()).orElseThrow(() -> new RepositoryItemExistsException("Счет не сохранился")));

        CheckingAccount bankAccountAnswer = assertInstanceOf(CheckingAccount.class, answer);
        assertAll(
                () -> assertEquals(bankAccount.getId(), bankAccountAnswer.getId()),
                () -> assertEquals(bankAccount.getUserId(), bankAccountAnswer.getUserId()),
                () -> assertEquals(0, bankAccount.getBalance().compareTo(bankAccountAnswer.getBalance())),
                () -> assertEquals(bankAccount.getStatus(), bankAccountAnswer.getStatus()),
                () -> assertEquals(bankAccount.getAccountType(), bankAccountAnswer.getAccountType()),
                () -> assertEquals(0, bankAccount.getOverdraftLimit().compareTo(bankAccountAnswer.getOverdraftLimit()))
        );
    }

    @Test
    @DisplayName("Получение несуществующего счета — пустой Optional")
    void checkGetUnknownAccount() {
        Optional<BankAccount> answer = inTransaction(() -> bankAccountRepository.get(UUID.randomUUID()));

        assertTrue(answer.isEmpty());
    }

    // ---------------------------------- update ----------------------------------

    @Test
    @DisplayName("Обновление баланса и статуса простого счета")
    void checkUpdateDefaultAccount() throws SQLException {
        BankAccount bankAccount = new BankAccount(UUID.randomUUID(), PERSON_1, new BigDecimal("100"));
        insertAccount(bankAccount);
        BankAccount changed = new BankAccount(bankAccount.getId(), PERSON_1, new BigDecimal("250"), AccountType.DEFAULT, AccountStatus.BLOCKED);

        inTransaction(() -> {
            BankAccount current = bankAccountRepository.get(bankAccount.getId()).orElseThrow();
            bankAccountRepository.update(current, changed);
        });

        BankAccount answer = inTransaction(() -> bankAccountRepository.get(bankAccount.getId())).orElseThrow();
        assertAll(
                () -> assertEquals(0, new BigDecimal("250").compareTo(answer.getBalance())),
                () -> assertEquals(AccountStatus.BLOCKED, answer.getStatus())
        );
    }

    @Test
    @DisplayName("Обновление баланса и деталей сберегательного счета")
    void checkUpdateSavingAccount() throws SQLException {
        SavingAccount bankAccount = new SavingAccount(UUID.randomUUID(), PERSON_1, new BigDecimal("300"), 2, 4, ACCRUAL_DATE);
        insertAccount(bankAccount);
        insertSavingDetails(bankAccount);
        LocalDateTime newDate = ACCRUAL_DATE.plusDays(30);
        SavingAccount changed = new SavingAccount(bankAccount.getId(), PERSON_1, new BigDecimal("200"), 1, 4, newDate);

        inTransaction(() -> {
            BankAccount current = bankAccountRepository.get(bankAccount.getId()).orElseThrow();
            bankAccountRepository.update(current, changed);
        });

        SavingAccount answer = (SavingAccount) inTransaction(() -> bankAccountRepository.get(bankAccount.getId())).orElseThrow();
        assertAll(
                () -> assertEquals(0, new BigDecimal("200").compareTo(answer.getBalance())),
                () -> assertEquals(1, answer.getWithdrawLimit()),
                () -> assertEquals(4, answer.getMaxWithdrawalLimit()),
                () -> assertEquals(newDate, answer.getDateLastAccrual())
        );
    }

    @Test
    @DisplayName("Обновление баланса и овердрафта счета с овердрафтом")
    void checkUpdateCheckingAccount() throws SQLException {
        CheckingAccount bankAccount = new CheckingAccount(UUID.randomUUID(), PERSON_1, new BigDecimal("10"), "1000");
        insertAccount(bankAccount);
        insertCheckingDetails(bankAccount);
        CheckingAccount changed = new CheckingAccount(bankAccount.getId(), PERSON_1, new BigDecimal("40"), new BigDecimal("3000"), AccountStatus.ACTIVE);

        inTransaction(() -> {
            BankAccount current = bankAccountRepository.get(bankAccount.getId()).orElseThrow();
            bankAccountRepository.update(current, changed);
        });

        CheckingAccount answer = (CheckingAccount) inTransaction(() -> bankAccountRepository.get(bankAccount.getId())).orElseThrow();
        assertAll(
                () -> assertEquals(0, new BigDecimal("40").compareTo(answer.getBalance())),
                () -> assertEquals(0, new BigDecimal("3000").compareTo(answer.getOverdraftLimit()))
        );
    }

    @Test
    @DisplayName("Обновление по устаревшей версии счета — SQLTransactionException, данные не меняются")
    void checkUpdateRejectsStaleVersion() throws SQLException {
        BankAccount bankAccount = new BankAccount(UUID.randomUUID(), PERSON_1, new BigDecimal("100"));
        insertAccount(bankAccount);
        BankAccount stale = new BankAccount(bankAccount.getId(), PERSON_1, new BigDecimal("999"));
        BankAccount changed = new BankAccount(bankAccount.getId(), PERSON_1, new BigDecimal("1"));

        assertThrows(SQLTransactionException.class,
                () -> inTransaction(() -> bankAccountRepository.update(stale, changed)));

        BankAccount answer = inTransaction(() -> bankAccountRepository.get(bankAccount.getId())).orElseThrow();
        assertEquals(0, new BigDecimal("100").compareTo(answer.getBalance()));
    }

    // ---------------------------------- delete и выборки ----------------------------------

    @Test
    @DisplayName("Проверка удаления счета")
    void checkDeleteAccount() throws SQLException {
        BankAccount bankAccount = new BankAccount(PERSON_1);
        insertAccount(bankAccount);

        BankAccount bankAccountAnswer1 = inTransaction(() ->
                bankAccountRepository.get(bankAccount.getId()).orElseThrow(() -> new RepositoryItemExistsException("Счет не сохранился")));

        inTransaction(() -> bankAccountRepository.delete(bankAccount.getId()));

        BankAccount bankAccountAnswer2 = inTransaction(() ->
                bankAccountRepository.get(bankAccount.getId()).orElseThrow(() -> new RepositoryItemExistsException("Счет не сохранился")));

        assertAll(
                () -> assertEquals(bankAccount.getStatus(), bankAccountAnswer1.getStatus()),
                () -> assertEquals(AccountStatus.DELETE, bankAccountAnswer2.getStatus())
        );
    }

    @Test
    @DisplayName("Проверка получения счетов пользователя")
    void checkGetByUser() throws SQLException {
        BankAccount bankAccount1 = new BankAccount(PERSON_1);
        BankAccount bankAccount2 = new BankAccount(PERSON_1);
        BankAccount bankAccount3 = new BankAccount(PERSON_1);
        BankAccount bankAccount4 = new BankAccount(PERSON_2);
        BankAccount bankAccount5 = new BankAccount(PERSON_2);
        for (BankAccount bankAccount : List.of(bankAccount1, bankAccount2, bankAccount3, bankAccount4, bankAccount5)) {
            insertAccount(bankAccount);
        }
        Set<UUID> bankAccountSetId = Set.of(bankAccount1.getId(), bankAccount2.getId(), bankAccount3.getId());

        List<BankAccount> result = inTransaction(() -> bankAccountRepository.getByUserId(PERSON_1));

        assertEquals(bankAccountSetId, ids(result));
    }

    @Test
    @DisplayName("Получение счетов по типу")
    void checkGetByAccountType() throws SQLException {
        SavingAccount saving1 = new SavingAccount(UUID.randomUUID(), PERSON_1, BigDecimal.ONE, 1, 1, ACCRUAL_DATE);
        SavingAccount saving2 = new SavingAccount(UUID.randomUUID(), PERSON_2, BigDecimal.ONE, 1, 1, ACCRUAL_DATE);
        BankAccount other = new BankAccount(PERSON_1);
        for (SavingAccount saving : List.of(saving1, saving2)) {
            insertAccount(saving);
            insertSavingDetails(saving);
        }
        insertAccount(other);

        List<BankAccount> result = inTransaction(() -> bankAccountRepository.getByAccountType(AccountType.SAVING));

        assertEquals(Set.of(saving1.getId(), saving2.getId()), ids(result));
    }

    @Test
    @DisplayName("Получение счетов по статусу")
    void checkGetByStatus() throws SQLException {
        BankAccount blocked = new BankAccount(UUID.randomUUID(), PERSON_1, BigDecimal.ONE, AccountType.DEFAULT, AccountStatus.BLOCKED);
        BankAccount active = new BankAccount(PERSON_2);
        insertAccount(blocked);
        insertAccount(active);

        List<BankAccount> result = inTransaction(() -> bankAccountRepository.getByStatus(AccountStatus.BLOCKED));

        assertEquals(Set.of(blocked.getId()), ids(result));
    }

    @Test
    @DisplayName("getAll не поддерживается")
    void checkGetAllIsUnsupported() {
        assertThrows(UnsupportedOperationException.class, bankAccountRepository::getAll);
    }

    // ---------------------------------- транзакции ----------------------------------

    @Test
    @DisplayName("Исключение внутри транзакции откатывает и счет, и его детали")
    void checkRollbackRemovesAccountAndDetails() throws SQLException {
        SavingAccount bankAccount = new SavingAccount(UUID.randomUUID(), PERSON_1, BigDecimal.TEN, 1, 1, ACCRUAL_DATE);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> inTransaction(() -> {
            bankAccountRepository.save(bankAccount.getId(), bankAccount);
            throw new IllegalStateException("сбой после сохранения");
        }));

        assertAll(
                () -> assertEquals("сбой после сохранения", thrown.getMessage()),
                () -> assertFalse(accountExists(bankAccount.getId()), "Счет не откатился"),
                () -> assertFalse(savingDetailsExist(bankAccount.getId()), "Детали счета не откатились")
        );
    }

    // ---------------------------------- вспомогательные методы (прямой SQL) ----------------------------------

    private static void insertAccount(BankAccount bankAccount) throws SQLException {
        String sql = "insert into bank_account (id, type, person_id, balance, status) values (?, ?, ?, ?, ?)";
        try (Connection conn = DATA_SOURCE.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, bankAccount.getId());
            ps.setInt(2, getAccountTypeId(bankAccount.getAccountType()));
            ps.setObject(3, bankAccount.getUserId());
            ps.setBigDecimal(4, bankAccount.getBalance());
            ps.setInt(5, getAccountStatusId(bankAccount.getStatus()));
            ps.executeUpdate();
        }
    }

    private static void insertSavingDetails(SavingAccount bankAccount) throws SQLException {
        String sql = "insert into saving_account_details (account_id, withdraw_limit, max_withdraw_limit, date_last_accrual) values (?, ?, ?, ?)";
        try (Connection conn = DATA_SOURCE.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, bankAccount.getId());
            ps.setInt(2, bankAccount.getWithdrawLimit());
            ps.setInt(3, bankAccount.getMaxWithdrawalLimit());
            ps.setObject(4, bankAccount.getDateLastAccrual());
            ps.executeUpdate();
        }
    }

    private static void insertCheckingDetails(CheckingAccount bankAccount) throws SQLException {
        String sql = "insert into checking_account_details (account_id, overdraft_limit) values (?, ?)";
        try (Connection conn = DATA_SOURCE.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, bankAccount.getId());
            ps.setBigDecimal(2, bankAccount.getOverdraftLimit());
            ps.executeUpdate();
        }
    }

    private static boolean accountExists(UUID id) throws SQLException {
        return exists("select 1 from bank_account where id = ?", id);
    }

    private static boolean savingDetailsExist(UUID id) throws SQLException {
        return exists("select 1 from saving_account_details where account_id = ?", id);
    }

    private static boolean exists(String sql, UUID id) throws SQLException {
        try (Connection conn = DATA_SOURCE.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static Set<UUID> ids(List<BankAccount> accounts) {
        return accounts.stream().map(BankAccount::getId).collect(Collectors.toSet());
    }

    private static int getAccountTypeId(AccountType accountType) {
        return idOf(Dictionaries.typeAccountDictionary, accountType.getMessage());
    }

    private static int getAccountStatusId(AccountStatus accountStatus) {
        return idOf(Dictionaries.statusAccountDictionary, accountStatus.getMessage());
    }
}
