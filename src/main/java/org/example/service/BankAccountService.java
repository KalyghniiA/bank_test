package org.example.service;

import org.example.exceptions.*;
import org.example.model.BankAccount;
import org.example.model.Transaction;
import org.example.repository.Repository;
import org.example.util.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public class BankAccountService {
    private final Repository<UUID, BankAccount> bankAccountRepository;
    private final Repository<UUID, Transaction> transactionRepository;

    public BankAccountService(Repository<UUID, BankAccount> bankAccountRepository, Repository<UUID, Transaction> transactionRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
    }

    public void transfer(UUID fromId, UUID toId, BigDecimal amount) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Значение не может быть отрицательным или равно нулю");
        }
        if (fromId.equals(toId)) {
            throw new DataAccountException("Нельзя переводить на один и тот же счет");
        }

        BankAccount accountFrom = bankAccountRepository.get(fromId).orElseThrow(() -> new EmptyAccountException(fromId.toString()));;
        BankAccount accountTo = bankAccountRepository.get(toId).orElseThrow(() -> new EmptyAccountException(toId.toString()));

        try {
            int first = fromId.compareTo(toId);
            if (first < 0) {
                accountFrom.lock();
                accountTo.lock();
            } else {
                accountTo.lock();
                accountFrom.lock();
            }


            if (accountFrom.checkBalanceLimit(amount)) throw new BalanceLimitException("Сумма списания больше баланса счета списания");
            BigDecimal accountFromBalance = accountFrom.getBalance();
            accountFrom.setBalance(accountFromBalance.subtract(amount));
            Transaction transactionFrom = new Transaction(fromId, TransactionType.TRANSFER_IN, amount, toId);
            transactionRepository.save(transactionFrom.getTransactionId(), transactionFrom);

            accountTo.setBalance(accountTo.getBalance().add(amount));
            Transaction transactionTo = new Transaction(toId, TransactionType.TRANSFER_OUT, amount, fromId);
            transactionRepository.save(transactionTo.getTransactionId(), transactionTo);
        } finally {
            accountFrom.unlock();
            accountTo.unlock();
        }
    }

    public void deposit(UUID accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Значение не может быть отрицательным или равно нулю");
        }

        bankAccountRepository.get(accountId)
                .ifPresentOrElse(
                        account ->{
                            try {
                                account.lock();
                                account.setBalance(account.getBalance().add(amount));
                                Transaction transaction = new Transaction(accountId, TransactionType.DEPOSIT, amount);
                                transactionRepository.save(transaction.getTransactionId(), transaction);
                            } finally {
                                account.unlock();
                            }
                        },
                        () -> {
                            throw new EmptyAccountException(accountId.toString());
                        });

    }

    public void withdraw(UUID accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Значение не может быть отрицательным или равно нулю");
        }

        BankAccount account = bankAccountRepository.get(accountId).orElseThrow(() -> new EmptyAccountException(accountId.toString()));
        try {
            account.lock();
            if (account.checkBalanceLimit(amount)) throw new BalanceLimitException("Баланс меньше суммы списания");
            account.setBalance(account.getBalance().subtract(amount));
            Transaction transaction = new Transaction(accountId, TransactionType.WITHDRAW, amount);
            transactionRepository.save(transaction.getTransactionId(), transaction);
        } finally {
            account.unlock();
        }
    }
}
