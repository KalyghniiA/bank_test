package org.example;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.example.model.BankAccount;
import org.example.model.Credentials;
import org.example.model.Person;
import org.example.repository.JDBCBankAccountRepository;
import org.example.repository.JDBCBankCredentialsRepository;
import org.example.repository.JDBCBankPersonRepository;
import org.example.repository.Repository;
import org.example.service.PersonService;
import org.example.servlet.HealthServlet;
import org.example.servlet.PersonServlet;
import org.example.util.ConnectionService;
import org.example.util.Dictionaries;
import org.example.util.transaction_manager.JDBCTransactionManager;
import org.example.util.transaction_manager.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.UUID;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws LifecycleException {
        HikariDataSource ds = ConnectionService.createDataSource();
        TransactionManager transactionManager = new JDBCTransactionManager(ds);
        Repository<UUID, Person> personRepository = new JDBCBankPersonRepository();
        Repository<UUID, BankAccount> bankAccountRepository = new JDBCBankAccountRepository();
        Repository<UUID, Credentials> credentialsRepository = new JDBCBankCredentialsRepository();
        PersonService personService = new PersonService(personRepository, bankAccountRepository, credentialsRepository, transactionManager);
        Dictionaries dictionaries = new Dictionaries(ds);

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir("target/tomcat");
        tomcat.setPort(8080);
        tomcat.getConnector();

        Context ctx = tomcat.addContext("", new File(".").getAbsolutePath());

        Tomcat.addServlet(ctx, "health", new HealthServlet());
        ctx.addServletMapping("/api/health", "health");

        Tomcat.addServlet(ctx, "person", new PersonServlet(personService));
        ctx.addServletMapping("/api/person/*", "person");

        dictionaries.loadDictionary();

        tomcat.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                tomcat.stop();
                tomcat.destroy();

            } catch (LifecycleException e) {
                logger.error(e.getMessage(), e);
            } finally {
                ds.close();
            }
        }));

        tomcat.getServer().await();


    }

}
