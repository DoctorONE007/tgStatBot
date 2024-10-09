package org.drone.tgstatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties
@EnableAsync
public class TgStatBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TgStatBotApplication.class, args);
    }

}
