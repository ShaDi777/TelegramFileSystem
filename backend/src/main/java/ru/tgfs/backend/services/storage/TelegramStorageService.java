package ru.tgfs.backend.services.storage;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.tgfs.backend.models.NodeAttributes;
import ru.tgfs.backend.models.Privileges;
import ru.tgfs.backend.services.tdlight.TdlightInitializer;
import ru.tgfs.backend.services.tdlight.TgfsApplication;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramStorageService implements StorageService {
    private final TdlightInitializer initializer;
    private TgfsApplication app;

    private final Map<String, byte[]> cache = new HashMap<>();

    @Override
    public void init() {
        app = initializer.init();
    }

    @SneakyThrows
    @Override
    public void store(String path, MultipartFile file) {
        app.saveFile(path, file);
    }

    @SneakyThrows
    @Override
    public void updateFile(String path, byte[] bytes, long offset) {
        byte[] data = cache.get(path);

        ByteBuffer contents = data == null ? ByteBuffer.allocate(1) : ByteBuffer.wrap(data);

        int maxWriteIndex = (int) (offset + bytes.length);
        synchronized (this) {
            if (maxWriteIndex > contents.capacity()) {
                // Need to create a new, larger buffer
                ByteBuffer newContents = ByteBuffer.allocate(maxWriteIndex);
                newContents.put(contents);
                contents = newContents;
            }
            contents.position((int) offset);
            contents.put(bytes);
            contents.position(0);
        }

        cache.put(path, contents.array());
    }

    @Override
    public void truncateFile(String path, long size) {
        byte[] data = cache.get(path);
        if (data != null) {
            cache.put(path, Arrays.copyOf(data, (int) size));
        }
    }

    @SneakyThrows
    @Override
    public void openFile(String path) {
        System.out.println("Load file into cache: " + path);
        cache.put(path, loadFileAsResource(path).getContentAsByteArray());
    }

    @SneakyThrows
    @Override
    public void releaseFile(String path) {
        byte[] fileData = cache.remove(path);
        if (fileData != null) {
            System.out.println("Releasing cache of size: " + fileData.length);
            app.writeFile(path, fileData, 0);
        }
    }

    @Override
    public List<Path> loadDirectory(String directoryPath) {
        return app.listDirectory(directoryPath).stream().map(Path::of).toList();
    }

    @Override
    public Resource loadFileAsResource(String filePath) {
        return new FileSystemResource(app.readFile(filePath));
    }

    @Override
    public NodeAttributes loadAttributes(String filePath) {
        System.out.println("Loading attributes for: " + filePath);

        return app.getPathInfo(filePath)
                  .withOwnerPrivileges(Privileges.RW())
                  .withGroupPrivileges(Privileges.RW())
                  .withOthersPrivileges(Privileges.RW());
    }

    @Override
    public void rename(String oldPath, String newPath) {
        app.rename(oldPath, newPath);
    }

    @Override
    public void deleteFile(String path) {
        app.deletePath(path);
    }

    @Override
    public void createDirectory(String path) {
        app.createDir(path);
    }

    @Override
    public void deleteDirectory(String directoryPath) {
        app.deletePath(directoryPath);
    }

    @Override
    public void deleteAll() {
        app.deletePath("/");
    }
}
