package org.example.web;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.example.model.BankAccount;
import org.example.model.Credentials;
import org.example.model.Person;
import org.example.repository.JDBCBankAccountRepository;
import org.example.repository.JDBCBankCredentialsRepository;
import org.example.repository.JDBCBankPersonRepository;
import org.example.repository.Repository;
import org.example.service.PersonService;
import org.example.util.ConnectionService;
import org.example.util.Dictionaries;
import org.example.util.JsonSupport;
import org.example.util.transaction_manager.JDBCTransactionManager;
import org.example.util.transaction_manager.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

public class BankContextListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(BankContextListener.class);
    private HikariDataSource ds;
    private ValidatorFactory factory;


    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Начало сборки контекста");
        ServletContext sc = sce.getServletContext();

        ds = ConnectionService.createDataSource();
        TransactionManager transactionManager = new JDBCTransactionManager(ds);
        Repository<UUID, Person> personRepository = new JDBCBankPersonRepository();
        Repository<UUID, BankAccount> bankAccountRepository = new JDBCBankAccountRepository();
        Repository<UUID, Credentials> credentialsRepository = new JDBCBankCredentialsRepository();
        PersonService personService = new PersonService(personRepository, bankAccountRepository, credentialsRepository, transactionManager);
        Dictionaries dictionaries = new Dictionaries(ds);
        JsonSupport json = new JsonSupport(new JsonMapper());
        factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        dictionaries.loadDictionary();

        sc.setAttribute("personService", personService);
        sc.setAttribute("json", json);
        sc.setAttribute("validator", validator);

        logger.info("Контекст собран");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (ds != null) ds.close();
        if (factory != null) factory.close();
    }
}
