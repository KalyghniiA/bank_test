package org.example.repository;

import org.example.exceptions.RepositoryException;
import org.example.model.Transaction;
import org.example.util.Dictionaries;
import org.example.util.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class JDBCTransactionRepositoryIT extends AbstractRepositoryIT {
    private static final UUID ACCOUNT_1 = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_2 = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");

    private final JDBCTransactionRepository transactionRepository = new JDBCTransactionRepository();

    @BeforeEach
    void setUp() throws SQLException {
        executeUpdate("truncate \"transaction\", saving_account_details, checking_account_details, bank_account cascade");
        int defaultType = idOf(Dictionaries.typeAccountDictionary, "DEFAULT");
        int active = idOf(Dictionaries.statusAccountDictionary, "ACTIVE");
        executeUpdate(
                String.format("insert into bank_account (id, type, person_id, balance, status) values ('%s', %d, '%s', 0, %d)",
                        ACCOUNT_1, defaultType, PERSON_1, active),
                String.format("insert into bank_account (id, type, person_id, balance, status) values ('%s', %d, '%s', 0, %d)",
                        ACCOUNT_2, defaultType, PERSON_2, active));
    }

    @Test
    @DisplayName("save + get: пополнение без связанного счёта, время проставляет база")
    void checkSaveAndGetDeposit() {
        Transaction deposit = new Transaction(ACCOUNT_1, TransactionType.DEPOSIT, new BigDecimal("150.25"));
        LocalDateTime before = LocalDateTime.now().minusMinutes(1);

        inTransaction(() -> transactionRepository.save(deposit.getTransactionId(), deposit));

        Transaction answer = inTransaction(() -> transactionRepository.get(deposit.getTransactionId())).orElseThrow();
        assertAll(
                () -> assertEquals(deposit.getTransactionId(), answer.getTransactionId()),
                () -> assertEquals(ACCOUNT_1, answer.getAccountId()),
                () -> assertEquals(TransactionType.DEPOSIT, answer.getType()),
                () -> assertEquals(0, new BigDecimal("150.25").compareTo(answer.getAmount())),
                () -> assertNull(answer.getRelatedAccountId()),
                () -> assertNotNull(answer.getTimeStamp()),
                () -> assertTrue(answer.getTimeStamp().isAfter(before), "Время транзакции не проставлено базой")
        );
    }

    @Test
    @DisplayName("save + get: перевод сохраняет связанный счёт")
    void checkSaveAndGetTransfer() {
        Transaction transferOut = new Transaction(ACCOUNT_1, TransactionType.TRANSFER_OUT, BigDecimal.TEN, ACCOUNT_2);

        inTransaction(() -> transactionRepository.save(transferOut.getTransactionId(), transferOut));

        Transaction answer = inTransaction(() -> transactionRepository.get(transferOut.getTransactionId())).orElseThrow();
        assertAll(
                () -> assertEquals(TransactionType.TRANSFER_OUT, answer.getType()),
                () -> assertEquals(ACCOUNT_2, answer.getRelatedAccountId())
        );
    }

    @Test
    @DisplayName("get несуществующей транзакции — пустой Optional")
    void checkGetUnknown() {
        Optional<Transaction> answer = inTransaction(() -> transactionRepository.get(UUID.randomUUID()));

        assertTrue(answer.isEmpty());
    }

    @Test
    @DisplayName("Транзакция по несуществующему счёту — RepositoryException с причиной 23503")
    void checkSaveForUnknownAccount() {
        Transaction orphan = new Transaction(UUID.randomUUID(), TransactionType.DEPOSIT, BigDecimal.ONE);

        RepositoryException exception = assertThrows(RepositoryException.class,
                () -> inTransaction(() -> transactionRepository.save(orphan.getTransactionId(), orphan)));

        assertEquals("23503", assertInstanceOf(PSQLException.class, exception.getCause()).getSQLState());
    }

    @Test
    @DisplayName("getByAccountId, getByType, getByAccountIdAndType возвращают ровно подходящие транзакции")
    void checkFilters() {
        Transaction deposit1 = new Transaction(ACCOUNT_1, TransactionType.DEPOSIT, BigDecimal.ONE);
        Transaction withdraw1 = new Transaction(ACCOUNT_1, TransactionType.WITHDRAW, BigDecimal.ONE);
        Transaction deposit2 = new Transaction(ACCOUNT_2, TransactionType.DEPOSIT, BigDecimal.ONE);
        inTransaction(() -> {
            for (Transaction transaction : List.of(deposit1, withdraw1, deposit2)) {
                transactionRepository.save(transaction.getTransactionId(), transaction);
            }
        });

        List<Transaction> byAccount = inTransaction(() -> transactionRepository.getByAccountId(ACCOUNT_1));
        List<Transaction> byType = inTransaction(() -> transactionRepository.getByType(TransactionType.DEPOSIT));
        List<Transaction> byAccountAndType = inTransaction(() ->
                transactionRepository.getByAccountIdAndType(ACCOUNT_1, TransactionType.DEPOSIT));

        assertAll(
                () -> assertEquals(Set.of(deposit1.getTransactionId(), withdraw1.getTransactionId()), ids(byAccount)),
                () -> assertEquals(Set.of(deposit1.getTransactionId(), deposit2.getTransactionId()), ids(byType)),
                () -> assertEquals(Set.of(deposit1.getTransactionId()), ids(byAccountAndType))
        );
    }

    @Test
    @DisplayName("update, delete и getAll не поддерживаются: транзакции неизменяемы")
    void checkUnsupportedOperations() {
        Transaction transaction = new Transaction(ACCOUNT_1, TransactionType.DEPOSIT, BigDecimal.ONE);

        assertAll(
                () -> assertThrows(UnsupportedOperationException.class, () -> transactionRepository.update(transaction, transaction)),
                () -> assertThrows(UnsupportedOperationException.class, () -> transactionRepository.delete(transaction.getTransactionId())),
                () -> assertThrows(UnsupportedOperationException.class, transactionRepository::getAll)
        );
    }

    private static Set<UUID> ids(List<Transaction> transactions) {
        return transactions.stream().map(Transaction::getTransactionId).collect(Collectors.toSet());
    }
}
