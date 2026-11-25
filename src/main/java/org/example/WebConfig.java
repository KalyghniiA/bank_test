package org.example;

import jakarta.validation.Validator;
import org.example.filter.AuthFilter;
import org.example.filter.RequestIdFilter;
import org.example.service.PersonService;
import org.example.servlet.AuthServlet;
import org.example.servlet.MeServlet;
import org.example.servlet.PersonServlet;
import org.example.util.JsonSupport;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan("org.example")
public class WebConfig {
    @Bean
    public ServletRegistrationBean<AuthServlet> authServlet(JsonSupport json, Validator validator, PersonService personService) {
        return new ServletRegistrationBean<>(new AuthServlet(json, validator, personService), "/api/auth/*");
    }

    @Bean
    public ServletRegistrationBean<MeServlet> meServlet(JsonSupport json, PersonService personService) {
        return new ServletRegistrationBean<>(new MeServlet(json, personService), "/api/me");
    }

    @Bean
    public ServletRegistrationBean<PersonServlet>  personServlet(JsonSupport json, PersonService personService, Validator validator) {
        return new ServletRegistrationBean<>(new PersonServlet(personService, json, validator), "/api/persons/*");
    }

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        FilterRegistrationBean<RequestIdFilter> registrationBean = new FilterRegistrationBean<>(new  RequestIdFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilter(JsonSupport json) {
        FilterRegistrationBean<AuthFilter> registrationBean = new FilterRegistrationBean<>(new  AuthFilter(json));
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(2);
        return registrationBean;
    }
}
