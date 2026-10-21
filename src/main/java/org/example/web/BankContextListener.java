package org.example.web;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.validation.Validator;
import org.example.BankConfig;
import org.example.service.PersonService;
import org.example.util.Dictionaries;
import org.example.util.JsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class BankContextListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(BankContextListener.class);
    private AnnotationConfigApplicationContext context;


    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Начало сборки контекста");
        context = new AnnotationConfigApplicationContext(BankConfig.class);
        ServletContext sc = sce.getServletContext();

        context.getBean(Dictionaries.class).loadDictionary();

        sc.setAttribute("personService", context.getBean(PersonService.class));
        sc.setAttribute("json", context.getBean(JsonSupport.class));
        sc.setAttribute("validator", context.getBean(Validator.class));

        logger.info("Контекст собран");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        context.close();
    }
}
