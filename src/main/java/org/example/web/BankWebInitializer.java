package org.example.web;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletRegistration;
import org.example.filter.RequestIdFilter;
import org.example.servlet.HealthServlet;
import org.example.servlet.PersonServlet;
import org.example.filter.EncodingFilter;

import java.util.EnumSet;
import java.util.Set;

public class BankWebInitializer implements ServletContainerInitializer {
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        ctx.addListener(new BankContextListener());

        FilterRegistration.Dynamic encoding = ctx.addFilter("encoding", new EncodingFilter());
        encoding.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
        FilterRegistration.Dynamic requestId = ctx.addFilter("requestId", new RequestIdFilter());
        requestId.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

        ServletRegistration.Dynamic health = ctx.addServlet("health", new HealthServlet());
        health.addMapping("/api/health");

        ServletRegistration.Dynamic person = ctx.addServlet("person", new PersonServlet());
        person.addMapping("/api/persons/*");
    }
}
