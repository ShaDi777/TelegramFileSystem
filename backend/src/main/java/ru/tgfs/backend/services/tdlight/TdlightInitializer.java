package ru.tgfs.backend.services.tdlight;

import it.tdlight.Init;
import it.tdlight.Log;
import it.tdlight.Slf4JLogMessageHandler;
import it.tdlight.client.APIToken;
import it.tdlight.client.AuthenticationSupplier;
import it.tdlight.client.SimpleAuthenticationSupplier;
import it.tdlight.client.SimpleTelegramClientBuilder;
import it.tdlight.client.SimpleTelegramClientFactory;
import it.tdlight.client.TDLibSettings;
import jakarta.annotation.PreDestroy;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import ru.tgfs.backend.configurations.TelegramConfig;

@Service
@RequiredArgsConstructor
public class TdlightInitializer {
    private final TelegramConfig telegramConfig;

    private SimpleTelegramClientFactory clientFactory;

    @SneakyThrows
    public TgfsApplication init() {
        Init.init();
        Log.setLogMessageHandler(1, new Slf4JLogMessageHandler());
        clientFactory = new SimpleTelegramClientFactory();

        var apiToken = new APIToken(telegramConfig.getApp().getApiId(), telegramConfig.getApp().getApiHash());
        TDLibSettings settings = TDLibSettings.create(apiToken);

        // Configure the session directory.
        Path sessionPath = Paths.get("tdlib-session-user-admin");
        settings.setDatabaseDirectoryPath(sessionPath.resolve("data"));
        settings.setDownloadedFilesDirectoryPath(sessionPath.resolve("downloads"));

        // Prepare a new client builder
        SimpleTelegramClientBuilder clientBuilder = clientFactory.builder(settings);

        // Configure the authentication info
        // Replace with AuthenticationSupplier.consoleLogin(), or .user(xxx), or .bot(xxx);
        // SimpleAuthenticationSupplier<?> authenticationData = AuthenticationSupplier.bot(telegramConfig.getBot().getToken());
        SimpleAuthenticationSupplier<?> authenticationData = AuthenticationSupplier.user(telegramConfig.getPhoneNumber());

        return new TgfsApplication(clientBuilder, authenticationData, telegramConfig.getAdminId());
    }

    @PreDestroy
    public void destroy() {
        if (clientFactory != null) {
            clientFactory.close();
        }
    }
}
