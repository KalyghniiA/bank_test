package org.example.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceErrorAspect {
    private static final Logger log = LoggerFactory.getLogger(ServiceErrorAspect.class);

    @AfterThrowing(pointcut =  "execution(* org.example.service..*(..))", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Throwable ex) {
        log.warn("Возникло исключение - класс: {};метод : {}; исключение : {};сообщение : {};",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                ex.getClass().getName(),
                ex.getMessage());
    }

}
