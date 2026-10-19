package com.zincoid.nullbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NullBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(NullBotApplication.class, args);
    }

}
