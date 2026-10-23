package org.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.example.util.JsonSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@ComponentScan("org.example")
@PropertySource("classpath:bank.properties")
public class BankConfig {

    @Bean
    public HikariDataSource dataSource(Environment env) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s",
                env.getRequiredProperty("DB_HOST"),
                env.getRequiredProperty("DB_PORT"),
                env.getRequiredProperty("DB_NAME")));
        config.setUsername(env.getRequiredProperty("DB_USER"));
        config.setPassword(env.getRequiredProperty("DB_PASSWORD"));
        config.setMaximumPoolSize(env.getProperty("db.pool.size", Integer.class, 10));
        config.setConnectionTimeout(env.getProperty("db.connection.timeout", Integer.class, 10000));
        return new HikariDataSource(config);
    }

    @Bean
    public JsonSupport jsonSupport() {
        return new JsonSupport(new JsonMapper());
    }

    @Bean
    public ValidatorFactory validatorFactory() {
        return Validation.buildDefaultValidatorFactory();
    }

    @Bean
    public Validator validator(ValidatorFactory validatorFactory) {
        return validatorFactory.getValidator();
    }
}
