package org.example.model;

import org.example.exceptions.BalanceLimitException;
import org.example.exceptions.BalanceNegativeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SavingAccountTest {
    BigDecimal startBalance;
    SavingAccount savingAccount;

    @BeforeEach
    public void setUp() {
        startBalance = BigDecimal.valueOf(100.00);
        savingAccount = new SavingAccount(UUID.randomUUID(), startBalance, 10, 10);
    }

    @Test
    @DisplayName("Проверка списания")
    public void checkSetBalanceNormalAmount() {
        BigDecimal amount = BigDecimal.valueOf(10.00);
        BigDecimal testBalance = startBalance.subtract(amount);
        int testWithdrawLimit = 9;
        int testMaxWithdrawLimit = 10;

        savingAccount.setBalance(testBalance);
        assertAll(
                () -> assertEquals(testBalance, savingAccount.getBalance()),
                () -> assertEquals(testWithdrawLimit, savingAccount.getWithdrawLimit()),
                () -> assertEquals(testMaxWithdrawLimit, savingAccount.getMaxWithdrawalLimit())
        );


    }

    @Test
    @DisplayName("Проверка списания при превышении лимита")
    public void checkSetBalanceOverWithdrawLimit() {
        savingAccount = new SavingAccount(UUID.randomUUID(), startBalance, 0, 0);
        BigDecimal amount = BigDecimal.valueOf(10.00);
        int testWithdrawLimit = 0;
        int testMaxWithdrawLimit = 0;

        BalanceLimitException exception = assertThrows(BalanceLimitException.class, () -> savingAccount.setBalance(amount));

        assertAll(
                () -> assertEquals("Превышен допустимый лимит снятий", exception.getMessage()),
                () -> assertEquals(testMaxWithdrawLimit, savingAccount.getMaxWithdrawalLimit()),
                () -> assertEquals(testWithdrawLimit, savingAccount.getWithdrawLimit())
        );

    }

    @Test
    @DisplayName("Проверка передачи отрицательного значения баланса")
    public void checkSetBalanceNegativeAmount() {
        BigDecimal amount = BigDecimal.valueOf(-10.00);
        int testWithdrawLimit = 10;
        int testMaxWithdrawLimit = 10;

        BalanceNegativeException exception = assertThrows(BalanceNegativeException.class, () -> savingAccount.setBalance(amount));

        assertAll(
                () -> assertEquals("Баланс не может быть отрицательным", exception.getMessage()),
                () -> assertEquals(testMaxWithdrawLimit, savingAccount.getMaxWithdrawalLimit()),
                () -> assertEquals(testWithdrawLimit, savingAccount.getWithdrawLimit())
        );

    }

    @Test
    @DisplayName("Проверка начисления")
    public void checkSetBalanceAmountPositive() {
        int testWithdrawLimit = 10;
        int testMaxWithdrawLimit = 10;
        BigDecimal testBalance = startBalance.add(BigDecimal.valueOf(10.00));

        savingAccount.setBalance(testBalance);

        assertAll(
                () -> assertEquals(testBalance, savingAccount.getBalance()),
                () -> assertEquals(testWithdrawLimit, savingAccount.getWithdrawLimit()),
                () -> assertEquals(testMaxWithdrawLimit, savingAccount.getMaxWithdrawalLimit())
        );
    }

    @Test
    @DisplayName("Присвоение одинакового баланса")
    public void checkBalanceSameAmount() {
        int testWithdrawLimit = 10;
        int testMaxWithdrawLimit = 10;
        BigDecimal testBalance = savingAccount.getBalance();

        savingAccount.setBalance(testBalance);

        assertAll(
                () -> assertEquals(testBalance, savingAccount.getBalance()),
                () -> assertEquals(testWithdrawLimit, savingAccount.getWithdrawLimit()),
                () -> assertEquals(testMaxWithdrawLimit, savingAccount.getMaxWithdrawalLimit())
        );
    }

    @Test
    @DisplayName("Проверка возможность снятия при положительном балансе и допустимом количестве списаний")
    public void checkBalanceLimitToPositiveBalanceAndWithdrawLimit() {
        assertTrue(savingAccount.canWithdraw(BigDecimal.valueOf(10.00)));
    }

    @Test
    @DisplayName("Проверка возможности снятия при граничащих значениях")
    public void checkBalanceLimitToSomeAmountAndWithdrawLimit() {
        savingAccount = new SavingAccount(UUID.randomUUID(), startBalance, 1, 1);
        assertTrue(savingAccount.canWithdraw(BigDecimal.valueOf(100.00)));
    }

    @Test
    @DisplayName("Проверка возможности списания суммы больше, чем есть на балансе")
    public void checkBalanceLimitToAmountOverBalance() {
        assertFalse(savingAccount.canWithdraw(BigDecimal.valueOf(200.00)));
    }

    @Test
    @DisplayName("Проверка возможности списания, при превышении лимита")
    public void checkBalanceLimitToOverWithdrawLimit() {
        savingAccount = new SavingAccount(UUID.randomUUID(), startBalance, 0, 0);
        assertFalse(savingAccount.canWithdraw(BigDecimal.valueOf(10.00)));
    }
}
