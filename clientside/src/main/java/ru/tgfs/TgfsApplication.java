package ru.tgfs;

import java.nio.file.Paths;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.tgfs.filesystem.TelegramFS;

@SpringBootApplication
public class TgfsApplication {
    public static void main(String[] args) {
        var context = SpringApplication.run(TgfsApplication.class, args);
        TelegramFS fs = context.getBean(TelegramFS.class);

        try {
            if (args.length != 1) {
                System.err.println("Usage: TgfsApplication <MountPoint>");
                System.exit(1);
            }
            fs.mount(Paths.get(args[0]), true, true);
        } finally {
            fs.umount();
        }
    }
}
