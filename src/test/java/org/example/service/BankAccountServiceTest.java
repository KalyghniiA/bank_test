package org.example.service;

import org.example.exceptions.BalanceLimitException;
import org.example.exceptions.DataAccountException;
import org.example.exceptions.EmptyAccountException;
import org.example.exceptions.InvalidAmountException;
import org.example.model.BankAccount;
import org.example.model.Transaction;
import org.example.repository.Repository;
import org.example.util.transaction_manager.TransactionManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BankAccountServiceTest {
    @Mock
    private Repository<UUID,BankAccount> bankAccountRepository;
    @Mock
    private Repository<UUID, Transaction> transactionRepository;
    @Mock
    private TransactionManager  transactionManager;

    private BankAccountService bankAccountService;

    @BeforeEach
    public void injectMocks(TestInfo testInfo) {
        bankAccountService = new BankAccountService(bankAccountRepository, transactionRepository, transactionManager);
    }

    @Nested
    @DisplayName("Проверки на начальные исключения")
    class ValidationException {

        @Test
        @DisplayName("Попытка начислить нулевое значение")
        public void checkDepositFromDefaultBankAccountZeroAmount() {
            Exception exception = assertThrows(InvalidAmountException.class, () -> {bankAccountService.deposit(UUID.randomUUID(), BigDecimal.ZERO);});

            assertEquals("Значение не может быть отрицательным или равно нулю", exception.getMessage());
        }

        @Test
        @DisplayName("Попытка списать нулевое значение")
        public void checkWithdrawFromDefaultBankAccountZeroAmount() {
            Exception exception = assertThrows(InvalidAmountException.class, () -> {bankAccountService.withdraw(UUID.randomUUID(), BigDecimal.ZERO);});

            assertEquals("Значение не может быть отрицательным или равно нулю", exception.getMessage());
        }

        @Test
        @DisplayName("Попытка перевести нулевое значение")
        public void checkTransferFromDefaultBankAccountZeroAmount() {
            Exception exception = assertThrows(InvalidAmountException.class, () -> {bankAccountService.transfer(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO);});

            assertEquals("Значение не может быть отрицательным или равно нулю", exception.getMessage());
        }

        @Test
        @DisplayName("Попытка начислить самому себе")
        public void checkTransferFromDefaultBankAccountToOneself() {
            UUID accountId = UUID.randomUUID();
            Exception exception = assertThrows(DataAccountException.class, () -> bankAccountService.transfer(accountId, accountId, BigDecimal.valueOf(10.00)));

            assertEquals("Нельзя переводить на один и тот же счет", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Основные тесты")
    class SuccessfulOperations {
        @BeforeEach
        void setUp() {
            doAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(transactionManager).runInTransaction(any(Runnable.class));
        }

        @Test
        @DisplayName("Проверка успешности начисления")
        public void checkSuccessDepositFromDefaultBankAccount() {
            UUID accountId = UUID.randomUUID();
            BigDecimal amount = BigDecimal.ZERO;
            BigDecimal newBalance = amount.add(BigDecimal.valueOf(20.00));
            BankAccount acc = new BankAccount(accountId, amount);

            when(bankAccountRepository.get(eq(accountId))).thenReturn(Optional.of(acc));

            bankAccountService.deposit(accountId, BigDecimal.valueOf(20.00));

            ArgumentCaptor<BankAccount> captorAcc = ArgumentCaptor.forClass(BankAccount.class);
            ArgumentCaptor<Transaction> captorTransaction = ArgumentCaptor.forClass(Transaction.class);


            verify(bankAccountRepository, times(1)).get(eq(accountId));
            verify(bankAccountRepository, times(1)).update(any(), captorAcc.capture());
            verify(transactionRepository, times(1)).save(any(UUID.class), captorTransaction.capture());

            assertEquals(newBalance, captorAcc.getValue().getBalance());
        }

        @Test
        @DisplayName("Проверка успешности списания")
        public void checkSuccessWithdrawFromDefaultBankAccount() {
            UUID accountId = UUID.randomUUID();
            BigDecimal amount = BigDecimal.valueOf(20.00);
            BigDecimal newBalance = amount.subtract(BigDecimal.valueOf(20.00));
            BankAccount acc = new BankAccount(accountId, amount);

            when(bankAccountRepository.get(eq(accountId))).thenReturn(Optional.of(acc));

            bankAccountService.withdraw(accountId, BigDecimal.valueOf(20.00));

            ArgumentCaptor<BankAccount> captorAcc = ArgumentCaptor.forClass(BankAccount.class);
            ArgumentCaptor<Transaction> captorTransaction = ArgumentCaptor.forClass(Transaction.class);
            verify(bankAccountRepository, times(1)).get(eq(accountId));
            verify(bankAccountRepository, times(1)).update(any(), captorAcc.capture());
            verify(transactionRepository, times(1)).save(any(UUID.class), captorTransaction.capture());

            assertEquals(newBalance, captorAcc.getValue().getBalance());
        }

        @Test
        @DisplayName("Проверка успешности перевода")
        public void checkTransferFromDefaultBankAccount() {
            UUID accountId1 = UUID.randomUUID();
            UUID accountId2 = UUID.randomUUID();
            BigDecimal amount1 = BigDecimal.valueOf(20.00);
            BigDecimal amount2 = BigDecimal.valueOf(20.00);
            BigDecimal newBalance1 = amount1.subtract(BigDecimal.valueOf(10.00));
            BigDecimal newBalance2 = amount2.add(BigDecimal.valueOf(10.00));

            BankAccount acc1 = new BankAccount(accountId1, amount1);
            BankAccount acc2 = new BankAccount(accountId2, amount2);

            when(bankAccountRepository.get(eq(accountId1))).thenReturn(Optional.of(acc1));
            when(bankAccountRepository.get(eq(accountId2))).thenReturn(Optional.of(acc2));

            bankAccountService.transfer(accountId1, accountId2, BigDecimal.valueOf(10.00));

            ArgumentCaptor<BankAccount> captorAcc = ArgumentCaptor.forClass(BankAccount.class);
            ArgumentCaptor<Transaction> captorTransaction = ArgumentCaptor.forClass(Transaction.class);


            verify(bankAccountRepository, times(1)).get(eq(accountId1));
            verify(bankAccountRepository, times(1)).get(eq(accountId2));
            verify(bankAccountRepository, times(2)).update(any(), captorAcc.capture());
            verify(transactionRepository, times(2)).save(any(), captorTransaction.capture());

            List<BankAccount> bankAccounts = captorAcc.getAllValues();

            assertAll(
                    () -> assertEquals(newBalance1, bankAccounts.get(0).getBalance()),
                    () -> assertEquals(newBalance2, bankAccounts.get(1).getBalance())
            );
        }
    }


    @Nested
    @DisplayName("Негативные сценарии")
    class NegativeScenarios {
        @BeforeEach
        void setUp() {
            doAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(transactionManager).runInTransaction(any(Runnable.class));
        }

        @Test
        @DisplayName("Попытка начисления несуществующему аккаунту")
        public void checkDepositMissingAccount() {
            UUID accountId = UUID.randomUUID();

            when(bankAccountRepository.get(eq(accountId))).thenReturn(Optional.empty());
            EmptyAccountException exception = assertThrows(EmptyAccountException.class, () -> {
                bankAccountService.deposit(accountId, BigDecimal.valueOf(10.00));
            });

            assertEquals(String.format("Счета с id %s не существует", accountId), exception.getMessage());
        }

        @Test
        @DisplayName("Попытка списания с несуществующего аккаунта")
        public void checkWithdrawMissingAccount() {
            UUID accountId = UUID.randomUUID();

            when(bankAccountRepository.get(eq(accountId))).thenReturn(Optional.empty());
            EmptyAccountException exception = assertThrows(EmptyAccountException.class, () -> {
                bankAccountService.withdraw(accountId, BigDecimal.valueOf(10.00));
            });

            assertEquals(String.format("Счета с id %s не существует", accountId), exception.getMessage());
        }

        @Test
        @DisplayName("Попытка перевода, когда один из аккаунтов не существует")
        public void checkTransferMissingAccount() {
            UUID accountId1 = UUID.randomUUID();
            UUID accountId2 = UUID.randomUUID();
            BigDecimal amount1 = BigDecimal.valueOf(10.00);

            BankAccount acc1 = new BankAccount(accountId1, amount1);

            when(bankAccountRepository.get(eq(accountId1))).thenReturn(Optional.of(acc1));
            when(bankAccountRepository.get(eq(accountId2))).thenReturn(Optional.empty());

            EmptyAccountException exception = assertThrows(EmptyAccountException.class, () -> {
                bankAccountService.transfer(accountId1, accountId2, amount1);
            });

            assertEquals(String.format("Счета с id %s не существует", accountId2), exception.getMessage());
        }

        @Test
        @DisplayName("Попытка списания, где баланс меньше суммы списания")
        public void checkWithdrawAmountExceedsBalance() {
            UUID accountId = UUID.randomUUID();
            BigDecimal balance = BigDecimal.valueOf(10.00);
            BigDecimal amount = BigDecimal.valueOf(20.00);
            BankAccount acc = new BankAccount(accountId, balance);

            when(bankAccountRepository.get(eq(accountId))).thenReturn(Optional.of(acc));

            BalanceLimitException exception = assertThrows(BalanceLimitException.class, () -> {
                bankAccountService.withdraw(accountId, amount);
            });

            verify(bankAccountRepository, never()).update(any(), any());
            verify(transactionRepository, never()).save(any(), any());

            assertEquals("Баланс меньше суммы списания", exception.getMessage());
        }

        @Test
        @DisplayName("Попытка перевода средств, когда баланс меньше суммы списания")
        public void checkTransferAmountExceedsBalance() {
            UUID accountId1 = UUID.randomUUID();
            UUID accountId2 = UUID.randomUUID();
            BigDecimal balance1 = BigDecimal.valueOf(10.00);
            BigDecimal balance2 = BigDecimal.valueOf(20.00);
            BankAccount acc1 = new BankAccount(accountId1, balance1);
            BankAccount acc2 = new BankAccount(accountId2, balance2);
            BigDecimal amount = BigDecimal.valueOf(20.00);

            when(bankAccountRepository.get(eq(accountId1))).thenReturn(Optional.of(acc1));
            when(bankAccountRepository.get(eq(accountId2))).thenReturn(Optional.of(acc2));

            BalanceLimitException exception = assertThrows(BalanceLimitException.class, () -> {
                bankAccountService.transfer(accountId1, accountId2, amount);
            });

            verify(bankAccountRepository, never()).update(any(), any());
            verify(transactionRepository, never()).save(any(), any());

            assertEquals("Сумма списания больше баланса счета списания", exception.getMessage());

        }
    }

}
