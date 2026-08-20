package org.example.service;

import org.example.exceptions.*;
import org.example.model.BankAccount;
import org.example.repository.BankAccountRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public class BankAccountService {
    private final BankAccountRepository bankAccountRepository;

    public BankAccountService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public void transfer(UUID fromId, UUID toId, BigDecimal amount) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Значение не может быть отрицательным или равно нулю");
        }
        if (fromId.equals(toId)) {
            throw new DataAccountException("Нельзя переводить на один и тот же счет");
        }

        BankAccount accountFrom = Optional.ofNullable(bankAccountRepository.findById(fromId)).orElseThrow(() -> new EmptyAccountException(fromId.toString()));
        BigDecimal accountFromBalance = accountFrom.getBalance();
        if (accountFromBalance.compareTo(amount) < 0) throw new BalanceLimitException("Сумма списания больше баланса счета списания");

        BankAccount accountTo = Optional.ofNullable(bankAccountRepository.findById(toId)).orElseThrow(() -> new EmptyAccountException(toId.toString()));

        accountFrom.setBalance(accountFromBalance.subtract(amount));
        accountTo.setBalance(accountTo.getBalance().add(amount));
    }

    public void deposit(UUID accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Значение не может быть отрицательным или равно нулю");
        }

        Optional
                .ofNullable(bankAccountRepository.findById(accountId))
                .ifPresentOrElse(
                        account -> account.setBalance(account.getBalance().add(amount)),
                        () -> {
                            throw new EmptyAccountException(accountId.toString());
                        });

    }

    public void withdraw(UUID accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Значение не может быть отрицательным или равно нулю");
        }

        BankAccount account = Optional.ofNullable(bankAccountRepository.findById(accountId)).orElseThrow(() -> new EmptyAccountException(accountId.toString()));
        if (account.getBalance().compareTo(amount) < 0) throw new BalanceLimitException("Баланс меньше суммы списания");
        account.setBalance(account.getBalance().subtract(amount));
    }
}
