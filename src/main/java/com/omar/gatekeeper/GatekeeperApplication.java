package com.omar.gatekeeper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

// @SpringBootApplication
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class GatekeeperApplication {

    public static void main(String[] args) {

        SpringApplication.run(GatekeeperApplication.class, args);
    }

}
