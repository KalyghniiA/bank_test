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
import org.example.repository.BankAccountRepository;
import org.example.repository.BankCredentialsRepository;
import org.example.repository.BankPersonRepository;

import org.example.util.ConnectionService;
import org.example.util.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public class PersonService {
    private final static Logger logger = LoggerFactory.getLogger(PersonService.class);
    private final BankPersonRepository bankPersonRepository;
    private final BankAccountRepository bankAccountRepository;
    private final BankCredentialsRepository bankCredentialsRepository;

    public PersonService(BankPersonRepository bankPersonRepository,
                         BankAccountRepository bankAccountRepository,
                         BankCredentialsRepository bankCredentialsRepository) {
        this.bankPersonRepository = bankPersonRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.bankCredentialsRepository = bankCredentialsRepository;
    }

    public void createPerson(PersonRequestDTO personDTO, CredentialsRequestDTO credDto) {
        logger.info("Начата операция по регистрации пользователя");
        try (Connection conn = ConnectionService.getConnection()) {
                conn.setAutoCommit(false);
                Person person = null;
                try {
                    person = PersonMapper.toPerson(personDTO);
                    bankPersonRepository.save(person.getId(), person, conn);
                }  catch (SQLException e) {
                    conn.rollback();
                    switch (e.getSQLState()) {
                        case "23505" : {
                            logger.error("Возникло дублирование id {}", person.getId() ,e);
                            throw new RepositoryParamException("Пользователь с таким id уже есть");
                        }
                        default: {
                            logger.error("Другая ошибка базы", e);
                            throw new RuntimeException("Другая ошибка базы", e);
                        }
                    }
                }

                try {
                    logger.info("Начата операция по регистрации данных для входа");
                    Credentials cred = CredentialsMapper.generateCredentials(credDto, person.getId());
                    bankCredentialsRepository.save(cred.id(), cred, conn);
                } catch (SQLException e) {
                    conn.rollback();
                    switch (e.getSQLState()) {
                        case "23505": {
                            logger.error("Логин уже существует", e);
                            throw new CredentialsUniqueException("Либо данный пользователь уже регистрировался, либо такой логин уже есть. Попробуйте другие данные");
                        }
                        case "23502": {
                            logger.error("Передан пустой параметр", e);
                            throw new RepositoryParamException("Передано пустое значение");
                        }
                        default: {
                            logger.error("Другая ошибка базы", e);
                            throw new RuntimeException("Другая ошибка базы", e);
                        }
                    }
                }
                logger.info("Пользователь {} {} успешно зарегистрирован", personDTO.subName(), personDTO.firstName());
                conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Другая ошибка базы", e);
        }

    }

    public void createAccountToPerson(UUID personId, AccountRequestDTO accountDTO) {
        logger.info("Начата операция создания счета для пользователя {}", personId);
        try (Connection conn = ConnectionService.getConnection()) {
            try {
                conn.setAutoCommit(false);

                Optional<Person> person = bankPersonRepository.get(personId, conn);
                if (person.isPresent()) {
                    BankAccount account = AccountMapper.toBankAccount(accountDTO);

                    bankAccountRepository.save(account.getId(), account, conn);
                } else {
                    logger.warn("Пользователь с id {} не найден в базе", personId);
                    throw new RepositoryItemExistsException("Данного пользователя нет в базе");
                }
                conn.commit();
                logger.info("Счет успешно зарегистрирован");
            } catch (SQLException | RepositoryItemExistsException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            switch (e.getSQLState()) {
                case "23503": {
                    logger.error("Неверно указанный параметр", e);
                    throw new RepositoryParamException("Неверно указанный параметр");
                }
                case "23505": {
                    logger.error("Счет таким id уже есть в базе",e);
                    throw new RepositoryParamException("Данный счет уже есть в базе");
                }
                default: {
                    logger.error("Другая ошибка базы", e);
                    throw new RuntimeException("Другая ошибка базы", e);
                }
            }
        }
    }

    public void deleteAccountToPerson(UUID personId, UUID accountId) {
        logger.info("Начата операция удаления счета");
        try (Connection conn = ConnectionService.getConnection()) {
            try {
                conn.setAutoCommit(false);
                Optional<Person> person = bankPersonRepository.get(personId, conn);
                if (person.isPresent()) {
                    bankAccountRepository.delete(accountId, conn);
                } else {
                    logger.warn("Пользователь id {} отсутствует в базе", personId);
                    throw new RepositoryItemExistsException("Данного пользователя нет в базе");
                }
                logger.info("Счет id {} успешно удален", accountId);
                conn.commit();
            } catch (SQLException | RepositoryItemExistsException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            switch (e.getSQLState()) {
                case "23503": {
                    logger.error("Неверно указанный параметр", e);
                    throw new RepositoryParamException("Неверно указанный параметр");
                }
                default: {
                    logger.error("Другая ошибка базы", e);
                    throw new RuntimeException("Другая ошибка базы", e);
                }
            }
        }
    }

    public Person login(CredentialsRequestDTO credDto) {
        logger.info("Начата операция по авторизации пользователя {}", credDto.login());
        try (Connection conn = ConnectionService.getConnection()) {
             try {
                 conn.setAutoCommit(false);
                 Credentials cred = bankCredentialsRepository.getByLogin(credDto.login(), conn).orElseThrow(() -> new PersonAuthException("Данный пользователь не найден"));
                 byte[] salt = cred.salt();
                 int iterations = cred.iterations();

                 byte[] passwordHash = PasswordService.hashPassword(credDto.password(), salt, iterations);
                 if (!MessageDigest.isEqual(cred.passwordHash(), passwordHash)) {
                     logger.warn("Пользователь ввел не верный пароль");
                     throw new PersonAuthException("Пароль не верен");
                 }
                 logger.info("Операция по авторизации закончена");

                 return bankPersonRepository.get(cred.personId(), conn).orElseThrow(() -> new RepositoryItemExistsException("Пользователя нет в базе"));
             } catch (SQLException | RepositoryItemExistsException | PersonAuthException e) {
                 conn.rollback();
                 throw e;
             }
        } catch (SQLException e) {
            switch (e.getSQLState()) {
                case "23503": {
                    logger.error("Неверно указанный параметр", e);
                    throw new RepositoryParamException("Неверно указанный параметр");
                }
                default: {
                    logger.error("Другая ошибка базы", e);
                    throw new RuntimeException("Другая ошибка базы", e);
                }
            }
        }
    }

    //пока отложу, нужно продумать как менять статус, без потери предыдущего состояния

//    public void changeStatusToPerson(UUID personId, String status) {
//        try (Connection conn = ConnectionService.getConnection()) {
//            conn.setAutoCommit(false);
//            try {
//                Person person = bankPersonRepository.get(personId, conn).orElseThrow(() -> new RepositoryItemExistsException("Данного пользователя нет"));
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
