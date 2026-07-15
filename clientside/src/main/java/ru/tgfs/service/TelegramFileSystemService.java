package ru.tgfs.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import ru.tgfs.model.ChangeFile;
import ru.tgfs.model.NodeAttributes;
import ru.tgfs.model.NodeInfo;
import ru.tgfs.model.RenameRequest;
import ru.tgfs.model.ResponseMessage;
import ru.tgfs.model.TruncateRequest;

@Component
public class TelegramFileSystemService implements FileSystemService {
    private static final String BACKEND_URL = "http://winhost:8080/filesystem";
    // private static final String BACKEND_URL = "http://172.28.28.135:8080/filesystem";
    private final RestClient restClient;

    public TelegramFileSystemService() {
        restClient = RestClient.create(BACKEND_URL);
    }

    @Override
    public NodeAttributes getAttributes(String path) {
        try {
            return restClient.method(HttpMethod.GET)
                             .uri("/attributes")
                             .body(path)
                             .retrieve()
                             // .onStatus(HttpStatus.NOT_FOUND::equals, (request, response) -> {})
                             .body(NodeAttributes.class);
        } catch (Exception e) {
            System.out.println("Exception in getAttributes(): " + e.getMessage());
            return null;
        }
    }

    @Override
    public ResponseMessage createFile(String path) {
        byte[] fileContent = "\0".getBytes();
        String filename = path.substring(path.lastIndexOf("/"));

        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
        ContentDisposition contentDisposition = ContentDisposition
                                                    .builder("form-data")
                                                    .name("file")
                                                    .filename(filename)
                                                    .build();

        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        HttpEntity<byte[]> fileEntity = new HttpEntity<>(fileContent, fileMap);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileEntity);
        body.add("path", path);

        return restClient.post()
                         .uri("/upload")
                         .body(body)
                         .retrieve()
                         .body(ResponseMessage.class);
    }

    @Override
    public void rename(String path, String targetPath) {
        restClient.patch()
                  .uri("/file")
                  .body(new RenameRequest(path, targetPath))
                  .retrieve()
                  .toBodilessEntity();
    }

    @Override
    public void truncateFile(String path, long size) {
        restClient.patch()
                  .uri("/update")
                  .body(new TruncateRequest(path, size))
                  .retrieve()
                  .toBodilessEntity();
    }

    @Override
    public void writeFile(String path, byte[] bytes, long offset) {
        restClient.post()
                  .uri("/update")
                  .body(new ChangeFile(path, bytes, offset))
                  .retrieve()
                  .toBodilessEntity();
    }

    @Override
    public List<String> listDir(String path) {
        try {
            return Arrays.stream(
                             Optional.ofNullable(
                                 restClient.method(HttpMethod.GET)
                                           .uri("/list")
                                           .body(path)
                                           .retrieve()
                                           .body(NodeInfo[].class)
                             ).orElse(new NodeInfo[] {})
                         )
                         .map(NodeInfo::name)
                         .toList();
        } catch (Exception e) {
            System.out.println("Exception in listDir(): " + e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteDir(String path) {
        restClient.method(HttpMethod.DELETE)
                  .uri("/directory")
                  .body(path)
                  .retrieve()
                  .toBodilessEntity();
    }

    @Override
    public void deleteFile(String path) {
        restClient.method(HttpMethod.DELETE)
                  .uri("/file")
                  .body(path)
                  .retrieve()
                  .toBodilessEntity();
    }

    @Override
    public String readFile(String path) {
        return restClient.method(HttpMethod.GET)
                         .uri("/file")
                         .body(path)
                         .retrieve()
                         .body(String.class);
    }

    @Override
    public void openFile(String path) {
        restClient.post()
                  .uri("/file/temp")
                  .body(path)
                  .retrieve()
                  .toBodilessEntity();
    }

    @Override
    public void releaseFile(String path) {
        restClient.method(HttpMethod.DELETE)
                  .uri("/file/temp")
                  .body(path)
                  .retrieve()
                  .toBodilessEntity();
    }

    @Override
    public ResponseMessage createDir(String path) {
        try {
            return restClient.post()
                             .uri("/directory")
                             .body(path)
                             .retrieve()
                             //.onStatus(HttpStatus.BAD_REQUEST::equals, (request, response) -> {})
                             .body(ResponseMessage.class);
        } catch (Exception e) {
            System.out.println("Exception in createDir(): " + e.getMessage());
            return null;
        }
    }
}
