package ru.tgfs.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.tgfs.backend.services.storage.StorageService;

@SpringBootApplication
public class TelegramFsBackendApplication {

    public static void main(String[] args) throws Exception {
        var ctx = SpringApplication.run(TelegramFsBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner initCLR(StorageService storageService) {
        return (args) -> {
            storageService.init();
        };
    }
}
