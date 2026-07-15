package ru.tgfs.backend.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "application.telegram", ignoreInvalidFields = false)
public @Data class TelegramConfig {
    private App app;
    private Bot bot;
    private Long adminId;
    private String phoneNumber;

    public static @Data class App {
        private Integer apiId;
        private String apiHash;
    }

    public static @Data class Bot {
        private String token;
    }
}
