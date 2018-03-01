package com.alarislabs.invoice.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.system.ApplicationPidFileWriter;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Restman {

    public static void main(String[] args) {

        //Initialization
        java.util.Locale.setDefault(new java.util.Locale("en", "US"));

        SpringApplication app = new SpringApplication(Restman.class);

        if (args.length >= 1) {
            app.addListeners(new ApplicationPidFileWriter(args[0]));
        }

        if (args.length >= 2) Conf.setConfigPath(args[1]);

        app.run(args);

    }

}
