package ru.tgfs.service;

import java.util.List;
import ru.tgfs.model.NodeAttributes;
import ru.tgfs.model.ResponseMessage;

public interface FileSystemService {
    NodeAttributes getAttributes(String path);

    ResponseMessage createFile(String path);

    void rename(String path, String targetPath);

    void truncateFile(String path, long size);

    void writeFile(String path, byte[] bytes, long offset);

    String readFile(String path);

    void openFile(String path);

    void releaseFile(String path);

    ResponseMessage createDir(String path);

    List<String> listDir(String path);

    void deleteDir(String path);

    void deleteFile(String path);
}
