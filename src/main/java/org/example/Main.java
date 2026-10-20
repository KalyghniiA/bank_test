package org.example;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.ErrorReportValve;
import org.example.web.BankWebInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir("target/tomcat");
        tomcat.setPort(8080);
        tomcat.getConnector();

        Context ctx = tomcat.addContext("", new File(".").getAbsolutePath());

        ctx.addServletContainerInitializer(new BankWebInitializer(), null);
        ErrorReportValve valve = new ErrorReportValve();
        valve.setShowReport(false);       // без стектрейса и описания
        valve.setShowServerInfo(false);   // без версии Tomcat
        tomcat.getHost().getPipeline().addValve(valve);

        tomcat.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                tomcat.stop();
                tomcat.destroy();

            } catch (LifecycleException e) {
                logger.error(e.getMessage(), e);
            }
        }));

        tomcat.getServer().await();
    }

}
