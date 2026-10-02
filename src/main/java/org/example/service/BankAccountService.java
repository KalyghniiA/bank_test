package org.example.service;

import org.example.exceptions.*;
import org.example.model.BankAccount;
import org.example.model.CheckingAccount;
import org.example.model.SavingAccount;
import org.example.model.Transaction;
import org.example.repository.Repository;
import org.example.util.AccountType;
import org.example.util.TransactionType;
import org.example.util.transaction_manager.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;


public class BankAccountService {
    private static final Logger logger = LoggerFactory.getLogger(BankAccountService.class);
    private final Repository<UUID, BankAccount> bankAccountRepository;
    private final Repository<UUID, Transaction> transactionRepository;
    private final TransactionManager transactionManager;

    public BankAccountService(Repository<UUID, BankAccount> bankAccountRepository, Repository<UUID, Transaction> transactionRepository, TransactionManager transactionManager) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionManager = transactionManager;
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

        try {
            transactionManager.runInTransaction(() -> {
                BankAccount accountFrom = bankAccountRepository.get(fromId).orElseThrow(() -> new EmptyAccountException(fromId.toString()));;
                BankAccount accountTo = bankAccountRepository.get(toId).orElseThrow(() -> new EmptyAccountException(toId.toString()));

                if (!accountFrom.canWithdraw(amount)) {
                    logger.warn("Превышение возможной суммы списания");
                    throw new BalanceLimitException("Сумма списания больше баланса счета списания");
                }

                BigDecimal newBalanceFrom = accountFrom.getBalance().subtract(amount);
                BankAccount newAccountFrom = getNewAcc(newBalanceFrom, accountFrom);
                bankAccountRepository.update(accountFrom, newAccountFrom);

                Transaction transactionFrom = new Transaction(fromId, TransactionType.TRANSFER_OUT, amount, toId);
                transactionRepository.save(transactionFrom.getTransactionId(), transactionFrom);

                BigDecimal newBalanceTo = accountTo.getBalance().add(amount);
                BankAccount newAccountTo = getNewAcc(newBalanceTo, accountTo);
                bankAccountRepository.update(accountTo, newAccountTo);

                Transaction transactionTo = new Transaction(toId, TransactionType.TRANSFER_IN, amount, fromId);
                transactionRepository.save(transactionTo.getTransactionId(), transactionTo);
                logger.info("Операция по переводу средств {} со счета {} на счет {} завершена", amount, fromId, toId);
            });
        } catch (RepositoryException e) {
            logger.error("Произошла ошибка работы с данными: {}", e.getCause().getMessage(), e);
            throw e;
        } catch (TransactionManagerSQLException e) {
            logger.error("Произошла ошибка менеджера транзакции", e);
            throw e;
        } catch (SQLTransactionException e) {
            logger.error("Ошибка транзакции", e);
            throw e;
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

        try {
            transactionManager.runInTransaction(() -> {
                BankAccount oldAcc = bankAccountRepository.get(accountId).orElseThrow(() -> new EmptyAccountException(accountId.toString()));
                BigDecimal newBalance = oldAcc.getBalance().add(amount);
                BankAccount newAcc = getNewAcc(newBalance, oldAcc);
                bankAccountRepository.update(oldAcc, newAcc);
                Transaction transaction = new Transaction(accountId, TransactionType.DEPOSIT, amount);
                transactionRepository.save(transaction.getTransactionId(), transaction);
                logger.info("Денежные средства {} зачислены на счет {}", amount, accountId);
            });
        } catch (RepositoryException e) {
            logger.error("Произошла ошибка работы с данными: {}", e.getCause().getMessage(), e);
            throw e;
        } catch (TransactionManagerSQLException e) {
            logger.error("Произошла ошибка менеджера транзакции", e);
            throw e;
        } catch (SQLTransactionException e) {
            logger.error("Ошибка транзакции", e);
            throw e;
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

        try {
            transactionManager.runInTransaction(() -> {
                BankAccount oldAcc =  bankAccountRepository.get(accountId).orElseThrow(() -> new EmptyAccountException(accountId.toString()));
                if (!oldAcc.canWithdraw(amount)) {
                    logger.warn("Сумма {} превышает баланс", amount);
                    throw new BalanceLimitException("Баланс меньше суммы списания");
                }
                BigDecimal newBalance = oldAcc.getBalance().subtract(amount);
                BankAccount newAcc = getNewAcc(newBalance, oldAcc);

                bankAccountRepository.update(oldAcc, newAcc);

                Transaction transaction = new Transaction(accountId, TransactionType.WITHDRAW, amount);
                transactionRepository.save(transaction.getTransactionId(), transaction);
                logger.info("Завершена операция списания {} со счета {}", amount, accountId);
            });
        } catch (RepositoryException e) {
            logger.error("Произошла ошибка работы с данными: {}", e.getCause().getMessage(), e);
            throw e;
        } catch (TransactionManagerSQLException e) {
            logger.error("Произошла ошибка менеджера транзакции", e);
            throw e;
        } catch (SQLTransactionException e) {
            logger.error("Ошибка транзакции", e);
            throw e;
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
