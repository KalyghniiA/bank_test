package org.example.service;

import org.example.exceptions.*;
import org.example.model.BankAccount;
import org.example.model.CheckingAccount;
import org.example.model.SavingAccount;
import org.example.model.Transaction;
import org.example.repository.Repository;
import org.example.util.AccountType;
import org.example.util.ConnectionService;
import org.example.util.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;


public class BankAccountService {
    private final static Logger logger = LoggerFactory.getLogger(BankAccountService.class);
    private final Repository<UUID, BankAccount> bankAccountRepository;
    private final Repository<UUID, Transaction> transactionRepository;

    public BankAccountService(Repository<UUID, BankAccount> bankAccountRepository, Repository<UUID, Transaction> transactionRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
    }

    public void transfer(UUID fromId, UUID toId, BigDecimal amount)
            throws InvalidAmountException,
            DataAccountException,
            EmptyAccountException,
            BalanceLimitException,
            RepositoryParamException,
            SQLTransactionException {
        logger.info("Начата операция трансфера между {} и {} суммы {}",  fromId, toId, amount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Передано отрицательное значение суммы. Сумма {}, id отправляемого счета {}", amount, fromId);
            throw new InvalidAmountException("Значение не может быть отрицательным или равно нулю");
        }
        if (fromId.equals(toId)) {
            logger.warn("Попытка отправки на один и тот же счет id {}", fromId);
            throw new DataAccountException("Нельзя переводить на один и тот же счет");
        }

        try (Connection conn = ConnectionService.getConnection()) {
            try {
                conn.setAutoCommit(false);

                BankAccount accountFrom = bankAccountRepository.get(fromId, conn).orElseThrow(() -> new EmptyAccountException(fromId.toString()));;
                BankAccount accountTo = bankAccountRepository.get(toId, conn).orElseThrow(() -> new EmptyAccountException(toId.toString()));

                if (accountFrom.checkBalanceLimit(amount)) {
                    logger.warn("Превышение возможной суммы списания");
                    throw new BalanceLimitException("Сумма списания больше баланса счета списания");
                }

                BigDecimal newBalanceFrom = accountFrom.getBalance().subtract(amount);
                BankAccount newAccountFrom = getNewAcc(newBalanceFrom, accountFrom);
                bankAccountRepository.update(accountFrom, newAccountFrom, conn);

                Transaction transactionFrom = new Transaction(fromId, TransactionType.TRANSFER_OUT, amount, toId);
                transactionRepository.save(transactionFrom.getTransactionId(), transactionFrom, conn);

                BigDecimal newBalanceTo = accountTo.getBalance().add(amount);
                BankAccount newAccountTo = getNewAcc(newBalanceTo, accountTo);
                bankAccountRepository.update(accountTo, newAccountTo, conn);

                Transaction transactionTo = new Transaction(toId, TransactionType.TRANSFER_IN, amount, fromId);
                transactionRepository.save(transactionTo.getTransactionId(), transactionTo, conn);
                conn.commit();
                logger.info("Операция по переводу средств {} со счета {} на счет {} завершена", amount, fromId, toId);
            } catch (SQLException e){
                conn.rollback();
                switch (e.getSQLState()) {
                    case "23503": {
                        logger.error("Передан не верный параметр", e);
                        throw new RepositoryParamException("Один из параметров указан не верно");
                    }
                    default: {
                        logger.error("Ошибка базы данных", e);
                        throw new RuntimeException("Другая ошибка базы", e);
                    }
                }
            } catch (SQLTransactionException e) {
                conn.rollback();
                logger.error("Ошибка транзакции", e);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void deposit(UUID accountId, BigDecimal amount)
            throws EmptyAccountException,
            InvalidAmountException,
            SQLTransactionException,
            RepositoryParamException {
        logger.info("Начата работа по начислению денежных средств {} на счет {}",  amount, accountId);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Передано отрицательное значение суммы. Сумма: {}, счет: {}", amount, accountId);
            throw new InvalidAmountException("Значение не может быть отрицательным или равно нулю");
        }
        try (Connection conn = ConnectionService.getConnection()) {
            try  {
                conn.setAutoCommit(false);
                BankAccount oldAcc = bankAccountRepository.get(accountId, conn).orElseThrow(() -> new EmptyAccountException(accountId.toString()));
                BigDecimal newBalance = oldAcc.getBalance().add(amount);
                BankAccount newAcc = getNewAcc(newBalance, oldAcc);
                bankAccountRepository.update(oldAcc, newAcc, conn);
                Transaction transaction = new Transaction(accountId, TransactionType.DEPOSIT, amount);
                transactionRepository.save(transaction.getTransactionId(), transaction, conn);
                conn.commit();
                logger.info("Денежные средства {} зачислены на счет {}", amount, accountId);
            } catch (SQLException e) {
                conn.rollback();
                switch (e.getSQLState()) {
                    case "23503":
                        logger.error("Неверно указанный параметр", e);
                        throw new RepositoryParamException("Какой то из параметров указан с ошибкой(возможно id пользователя)");
                    case "23502":
                        logger.error("Один из параметров пустой", e);
                        throw new RepositoryParamException("Один из обязательных параметров пустой");
                    default:
                        logger.error("Ошибка работы с базой", e);
                        throw new RuntimeException("Другая ошибка базы", e);
                }
            } catch (SQLTransactionException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void withdraw(UUID accountId, BigDecimal amount)
            throws InvalidAmountException,
            SQLTransactionException,
            RepositoryParamException,
            EmptyAccountException,
            BalanceLimitException {
        logger.info("Начата операция списания денежных средств {} со счета {}",  amount, accountId);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Передана не валидная сумма: {}", amount);
            throw new InvalidAmountException("Значение не может быть отрицательным или равно нулю");
        }

        try (Connection conn = ConnectionService.getConnection()) {
            try {
                conn.setAutoCommit(false);
                BankAccount oldAcc =  bankAccountRepository.get(accountId, conn).orElseThrow(() -> new EmptyAccountException(accountId.toString()));
                if (oldAcc.checkBalanceLimit(amount)) {
                    logger.warn("Сумма {} превышает баланс", amount);
                    throw new BalanceLimitException("Баланс меньше суммы списания");
                }
                BigDecimal newBalance = oldAcc.getBalance().subtract(amount);
                BankAccount newAcc = getNewAcc(newBalance, oldAcc);

                bankAccountRepository.update(oldAcc, newAcc, conn);

                Transaction transaction = new Transaction(accountId, TransactionType.WITHDRAW, amount);
                transactionRepository.save(transaction.getTransactionId(), transaction, conn);
                conn.commit();
                logger.info("Завершена операция списания {} со счета {}", amount, accountId);
            } catch (SQLException e) {
                conn.rollback();
                switch (e.getSQLState()) {
                    case "23503":
                        logger.error("Неверно указанный параметр", e);
                        throw new RepositoryParamException("Какой то из параметров указан с ошибкой(возможно id пользователя)");
                    case "23502":
                        logger.error("Один из параметров пустой", e);
                        throw new RepositoryParamException("Один из обязательных параметров пустой");
                    default:
                        logger.error("Ошибка работы с базой", e);
                        throw new RuntimeException("Другая ошибка базы", e);
                }
            } catch (SQLTransactionException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static BankAccount getNewAcc(BigDecimal newBalance, BankAccount oldAcc) {
        return switch (oldAcc.getAccountType()) {
            case SAVING -> {
                yield new SavingAccount(
                        oldAcc.getId(),
                        oldAcc.getUserId(),
                        newBalance,
                        ((SavingAccount) oldAcc).getWithdrawLimit(),
                        ((SavingAccount) oldAcc).getMaxWithdrawalLimit(),
                        ((SavingAccount) oldAcc).getDateLastAccrual(),
                        oldAcc.getStatus()
                );
            }
            case CHECKING -> {
                yield new CheckingAccount(
                        oldAcc.getId(),
                        oldAcc.getUserId(),
                        newBalance,
                        ((CheckingAccount) oldAcc).getOverdraftLimit(),
                        oldAcc.getStatus()

                );
            }
            default -> {
                yield new BankAccount(
                        oldAcc.getId(),
                        oldAcc.getUserId(),
                        newBalance,
                        AccountType.DEFAULT,
                        oldAcc.getStatus()
                );
            }
        };
    }
}
