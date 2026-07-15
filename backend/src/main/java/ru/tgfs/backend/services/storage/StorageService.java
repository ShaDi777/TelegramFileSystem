package ru.tgfs.backend.services.storage;

import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import ru.tgfs.backend.models.NodeAttributes;

public interface StorageService {

    void init();

    List<Path> loadDirectory(String directoryPath);

    Resource loadFileAsResource(String filePath);

    NodeAttributes loadAttributes(String filePath);

    void rename(String oldPath, String newPath);

    void store(String path, MultipartFile file);

    void updateFile(String path, byte[] bytes, long offset);

    void truncateFile(String path, long size);

    void openFile(String path);

    void releaseFile(String path);

    void deleteFile(String path);

    void createDirectory(String path);

    void deleteDirectory(String directoryPath);

    void deleteAll();
}
