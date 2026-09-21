package org.example;

import org.example.model.SavingAccount;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

/**
 * Ручная (без JUnit — в проекте тестового фреймворка нет) проверка accrueInterestIfDue()
 * с зафиксированным Clock и явно заданной датой создания счёта, без реального ожидания.
 */
public class SavingAccountManualCheck {

    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final Instant START_INSTANT = Instant.parse("2026-01-01T00:00:00Z");

    public static void main(String[] args) {
        scenario1_lessThanMonth();
        scenario2_exactlyOneMonth();
        scenario3_threeMonthsWithRemainder();
        scenario4_yearBoundary();
    }

    private static Clock fixedClockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static SavingAccount newAccountCreatedAtStart(BigDecimal balance, int withdrawLimit) {
        return new SavingAccount(UUID.randomUUID(), UUID.randomUUID(), balance, withdrawLimit, START_DATE.atStartOfDay());
    }

    private static void scenario1_lessThanMonth() {
        System.out.println("=== Сценарий 1: меньше месяца (15 дней) — ничего не должно измениться ===");
        SavingAccount acc = newAccountCreatedAtStart(new BigDecimal("1000"), 5);
        BigDecimal balanceBefore = acc.getBalance();

        Instant after15Days = START_INSTANT.plusSeconds(15L * 24 * 3600);
        acc.accrueInterestIfDue(fixedClockAt(after15Days));
        BigDecimal balanceAfter = acc.getBalance();

        System.out.println("Баланс до: " + balanceBefore + ", после 15 дней: " + balanceAfter);
        check(balanceBefore.compareTo(balanceAfter) == 0, "Баланс не должен был измениться");
        System.out.println();
    }

    private static void scenario2_exactlyOneMonth() {
        System.out.println("=== Сценарий 2: ровно 30 дней — один период 16%, лимит снятий сброшен ===");
        SavingAccount acc = newAccountCreatedAtStart(new BigDecimal("1000"), 5);

        // тратим лимит снятий (имитация withdraw через прямой setBalance, т.к. это модельный тест)
        acc.setBalance(new BigDecimal("900"));
        acc.setBalance(new BigDecimal("800"));
        System.out.println("Лимит снятий после двух списаний: " + acc.getWithdrawLimit() + " (ожидается 3)");

        Instant after30Days = START_INSTANT.plusSeconds(30L * 24 * 3600);
        acc.accrueInterestIfDue(fixedClockAt(after30Days));

        BigDecimal expected = new BigDecimal("800").multiply(new BigDecimal("1.16"));
        System.out.println("Баланс: " + acc.getBalance() + " (ожидается " + expected + ")");
        System.out.println("Лимит снятий после начисления: " + acc.getWithdrawLimit() + " (ожидается 5, сброшен)");
        check(acc.getBalance().compareTo(expected) == 0, "Баланс должен вырасти ровно на 16%");
        check(acc.getWithdrawLimit() == 5, "Лимит снятий должен сброситься до максимума");
        System.out.println();
    }

    private static void scenario3_threeMonthsWithRemainder() {
        System.out.println("=== Сценарий 3: 95 дней (3 периода по 30 + остаток 5 дней) — сложный процент ===");
        SavingAccount acc = newAccountCreatedAtStart(new BigDecimal("1000"), 5);

        Instant after95Days = START_INSTANT.plusSeconds(95L * 24 * 3600);
        acc.accrueInterestIfDue(fixedClockAt(after95Days));

        BigDecimal expected = new BigDecimal("1000");
        for (int i = 0; i < 3; i++) {
            expected = expected.multiply(new BigDecimal("1.16"));
        }
        System.out.println("Баланс: " + acc.getBalance() + " (ожидается сложный процент x3: " + expected + ")");
        check(acc.getBalance().compareTo(expected) == 0, "Баланс должен вырасти на 16% три раза подряд (compound)");

        // проверка, что остаток в 5 дней перенесён, а не потерян:
        // ещё через 25 дней (5 + 25 = 30) должен сработать ЕЩЁ один период
        Instant after25MoreDays = after95Days.plusSeconds(25L * 24 * 3600);
        BigDecimal balanceBeforeFourth = acc.getBalance();
        acc.accrueInterestIfDue(fixedClockAt(after25MoreDays));
        BigDecimal balanceAfterFourth = acc.getBalance();
        System.out.println("Баланс после ещё +25 дней (итого +120 от старта): " + balanceAfterFourth
                + " (ожидается рост ещё на 16% от " + balanceBeforeFourth + ")");
        check(balanceAfterFourth.compareTo(balanceBeforeFourth.multiply(new BigDecimal("1.16"))) == 0,
                "Остаток в 5 дней должен был перенестись, а не потеряться");
        System.out.println();
    }

    private static void scenario4_yearBoundary() {
        System.out.println("=== Сценарий 4: переход через границу года (365 дней, ~12 периодов) ===");
        SavingAccount acc = newAccountCreatedAtStart(new BigDecimal("1000"), 5);

        Instant afterOneYear = START_INSTANT.plusSeconds(365L * 24 * 3600);
        acc.accrueInterestIfDue(fixedClockAt(afterOneYear));

        BigDecimal expected = new BigDecimal("1000");
        int periods = 365 / 30; // 12
        for (int i = 0; i < periods; i++) {
            expected = expected.multiply(new BigDecimal("1.16"));
        }
        System.out.println("Баланс: " + acc.getBalance() + " (ожидается " + periods + " периодов: " + expected + ")");
        check(acc.getBalance().compareTo(expected) == 0, "Должно быть применено " + periods + " периодов даже через границу года");
        System.out.println();
    }

    private static void check(boolean condition, String message) {
        System.out.println((condition ? "OK: " : "FAIL: ") + message);
    }
}
