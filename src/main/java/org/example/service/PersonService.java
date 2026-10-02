package org.example.service;

import org.example.dto.AccountRequestDTO;
import org.example.dto.CredentialsRequestDTO;
import org.example.dto.PersonRequestDTO;
import org.example.exceptions.*;
import org.example.mapper.AccountMapper;
import org.example.mapper.CredentialsMapper;
import org.example.mapper.PersonMapper;
import org.example.model.BankAccount;
import org.example.model.Credentials;
import org.example.model.Person;
import org.example.repository.*;

import org.example.util.PasswordService;
import org.example.util.transaction_manager.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;

public class PersonService {
    private static final Logger logger = LoggerFactory.getLogger(PersonService.class);
    private final Repository<UUID, Person> bankPersonRepository;
    private final Repository<UUID, BankAccount> bankAccountRepository;
    private final Repository<UUID, Credentials> bankCredentialsRepository;
    private final TransactionManager transactionManager;

    public PersonService(Repository<UUID, Person> bankPersonRepository,
                         Repository<UUID, BankAccount> bankAccountRepository,
                         Repository<UUID, Credentials> bankCredentialsRepository,
                         TransactionManager transactionManager) {
        this.bankPersonRepository = bankPersonRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.bankCredentialsRepository = bankCredentialsRepository;
        this.transactionManager = transactionManager;
    }

    public void createPerson(PersonRequestDTO personDTO, CredentialsRequestDTO credDto) {
        logger.info("Начата операция по регистрации пользователя");
        try {
            transactionManager.runInTransaction(() -> {
                Person person = PersonMapper.toPerson(personDTO);
                bankPersonRepository.save(person.getId(), person);


                logger.info("Начата операция по регистрации данных для входа");
                Credentials cred = CredentialsMapper.generateCredentials(credDto, person.getId());
                bankCredentialsRepository.save(cred.id(), cred);

                logger.info("Пользователь успешно зарегистрирован");
            });
        } catch (RepositoryException e) {
            logger.error("Произошла ошибка работы с данными: {}", e.getCause().getMessage(), e);
            throw e;
        } catch (TransactionManagerSQLException e) {
            logger.error("Произошла ошибка менеджера транзакции", e);
            throw e;
        }

    }

    public void createAccountToPerson(UUID personId, AccountRequestDTO accountDTO) {
        logger.info("Начата операция создания счета для пользователя {}", personId);
        try {
            transactionManager.runInTransaction(() -> {
                Optional<Person> person = bankPersonRepository.get(personId);
                if (person.isPresent()) {
                    BankAccount account = AccountMapper.toBankAccount(accountDTO);

                    bankAccountRepository.save(account.getId(), account);
                } else {
                    logger.warn("Пользователь с id {} не найден в базе", personId);
                    throw new RepositoryItemExistsException("Данного пользователя нет в базе");
                }
                logger.info("Счет успешно зарегистрирован");
            });
        } catch (RepositoryException e) {
            logger.error("Произошла ошибка работы с данными: {}", e.getCause().getMessage(), e);
            throw e;
        } catch (TransactionManagerSQLException e) {
            logger.error("Произошла ошибка менеджера транзакции", e);
            throw e;
        }

    }

    public void deleteAccountToPerson(UUID personId, UUID accountId) {
        logger.info("Начата операция удаления счета");
        try {
            transactionManager.runInTransaction(() -> {
                Optional<Person> person = bankPersonRepository.get(personId);
                if (person.isPresent()) {
                    bankAccountRepository.delete(accountId);
                } else {
                    logger.warn("Пользователь id {} отсутствует в базе", personId);
                    throw new RepositoryItemExistsException("Данного пользователя нет в базе");
                }
                logger.info("Счет id {} успешно удален", accountId);
            });
        } catch (RepositoryException e) {
            logger.error("Произошла ошибка работы с данными: {}", e.getCause().getMessage(), e);
            throw e;
        } catch (TransactionManagerSQLException e) {
            logger.error("Произошла ошибка менеджера транзакции", e);
            throw e;
        }

    }

    public Person login(CredentialsRequestDTO credDto) {
        logger.info("Начата операция по авторизации пользователя {}", credDto.login());
        try  {
           return transactionManager.runInTransaction(() -> {
                Credentials cred = ((CredentialsRepository<UUID, Credentials>) bankCredentialsRepository).getByLogin(credDto.login()).orElseThrow(() -> new PersonAuthException("Данный пользователь не найден"));
                byte[] salt = cred.salt();
                int iterations = cred.iterations();

                byte[] passwordHash = PasswordService.hashPassword(credDto.password(), salt, iterations);
                if (!MessageDigest.isEqual(cred.passwordHash(), passwordHash)) {
                    logger.warn("Пользователь ввел не верный пароль");
                    throw new PersonAuthException("Пароль не верен");
                }
                logger.info("Операция по авторизации закончена");

                return bankPersonRepository.get(cred.personId()).orElseThrow(() -> new RepositoryItemExistsException("Пользователя нет в базе"));
            });
        } catch (RepositoryException e) {
            logger.error("Произошла ошибка работы с данными: {}", e.getCause().getMessage(), e);
            throw e;
        } catch (TransactionManagerSQLException e) {
            logger.error("Произошла ошибка менеджера транзакции", e);
            throw e;
        }
    }

    //пока отложу, нужно продумать как менять статус, без потери предыдущего состояния

//    public void changeStatusToPerson(UUID personId, String status) {
//        try (Connection conn = ConnectionService.getConnection()) {
//            conn.setAutoCommit(false);
//            try {
//                Person person = bankPersonRepository.get(personId).orElseThrow(() -> new RepositoryItemExistsException("Данного пользователя нет"));
//
//
//
//
//            } catch (SQLException | IllegalArgumentException | SQLTransactionException e) {
//                conn.rollback();
//            }
//        } catch (SQLException e) {
//            switch (e.getSQLState()) {
//                case "23503": throw new RepositoryItemExistsException("Данного пользователя нет в базе");
//                default: throw new RuntimeException("Другая ошибка базы", e);
//            }
//        } catch (IllegalArgumentException e) {
//            throw new RepositoryParamException("Передан не верный параметр статуса");
//        }
//    }


}
