package ru.tgfs.backend.services.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.tgfs.backend.exceptions.StorageException;
import ru.tgfs.backend.exceptions.StorageFileNotFoundException;
import ru.tgfs.backend.exceptions.StorageNameAlreadyExistsException;
import ru.tgfs.backend.models.NodeAttributes;
import ru.tgfs.backend.models.Privileges;

// @Component
public class LocalStorageService implements StorageService {
    @Value("${application.root-directory}")
    private Path rootLocation;

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }

    @Override
    public void store(String path, MultipartFile file) {
        System.out.println("SAVING FILE: " + path);
        try {
            Path destinationFile = Path.of(rootLocation.toString(), file.getOriginalFilename()).normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile,
                           StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    @SneakyThrows
    @Override
    public void updateFile(String path, byte[] bytes, long offset) {
        Path absPath = Path.of(rootLocation.toString(), path);
        System.out.println("ABS PATH: " + absPath);
        ByteBuffer contents = ByteBuffer.wrap(Files.readAllBytes(absPath));

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

        Files.write(absPath, contents.array());
    }

    @Override
    public void truncateFile(String path, long size) {

    }

    @Override
    public void openFile(String path) {

    }

    @Override
    public void releaseFile(String path) {

    }

    @Override
    public List<Path> loadDirectory(String directoryPath) {
        Path absPath = Path.of(rootLocation.toString(), directoryPath);
        if (Files.notExists(absPath)) {
            throw new StorageFileNotFoundException("No such dir: " + directoryPath);
        }
        try (var pathStream = Files.walk(absPath, 1)) {
            return pathStream.filter(path -> !path.equals(absPath))
                             .map(absPath::relativize)
                             .toList();
        } catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }
    }

    @Override
    public Resource loadFileAsResource(String filePath) {
        // Path absPath = rootLocation.resolve(filePath);
        Path absPath = Path.of(rootLocation.toString(), filePath);
        Resource resource = new FileSystemResource(new File(absPath.toString()));
        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new StorageFileNotFoundException("Could not read file: " + filePath);
        }
    }

    // TODO check if this is valid
    @Override
    @SneakyThrows
    public NodeAttributes loadAttributes(String filePath) {
        Path resolvedPath = Path.of(rootLocation.toString(), filePath);

        if (!resolvedPath.startsWith(rootLocation)) {
            throw new StorageException("Path outside ROOT directory!");
        }

        if (Files.notExists(resolvedPath)) {
            throw new StorageFileNotFoundException("No such file or directory: " + resolvedPath);
        }

        return new NodeAttributes(
            Files.isDirectory(resolvedPath),
            Files.size(resolvedPath),
            (int) Files.getLastModifiedTime(resolvedPath).to(TimeUnit.SECONDS),
            Privileges.ALL(),
            Privileges.ALL(),
            Privileges.ALL()
        );
    }

    @SneakyThrows
    @Override
    public void rename(String oldPath, String newPath) {
        throw new NoSuchMethodException();
    }

    @Override
    @SneakyThrows
    public void deleteFile(String path) {
        Path absolutePath = Path.of(rootLocation.toString(), path);
        System.out.println("ABSPATH: " + absolutePath);
        if (absolutePath.startsWith(rootLocation)) {
            Files.delete(absolutePath);
        }
    }

    @Override
    @SneakyThrows
    public void createDirectory(String path) {
        Path absolutePath = Path.of(rootLocation.toString(), path);
        if (Files.exists(absolutePath)) {
            throw new StorageNameAlreadyExistsException("Node with such name already exists: " + path);
        }

        Files.createDirectories(absolutePath);
    }

    @Override
    public void deleteDirectory(String directoryPath) {
        Path absolutePath = Path.of(rootLocation.toString(), directoryPath);
        if (absolutePath.startsWith(rootLocation)) {
            FileSystemUtils.deleteRecursively(absolutePath.toFile());
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }
}
