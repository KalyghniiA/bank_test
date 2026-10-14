package org.example.web;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.example.servlet.HealthServlet;
import org.example.servlet.PersonServlet;

import java.util.Set;

public class BankWebInitializer implements ServletContainerInitializer {
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        ctx.addListener(new BankContextListener());

        ServletRegistration.Dynamic health = ctx.addServlet("health", new HealthServlet());
        health.addMapping("/api/health");

        ServletRegistration.Dynamic person = ctx.addServlet("person", new PersonServlet());
        person.addMapping("/api/person/*");
    }
}
