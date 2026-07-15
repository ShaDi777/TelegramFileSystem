package ru.tgfs.backend.controllers;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.tgfs.backend.exceptions.StorageFileNotFoundException;
import ru.tgfs.backend.exceptions.StorageNameAlreadyExistsException;
import ru.tgfs.backend.models.FileInfo;
import ru.tgfs.backend.models.FileUpdate;
import ru.tgfs.backend.models.NodeAttributes;
import ru.tgfs.backend.models.RenameRequest;
import ru.tgfs.backend.models.ResponseMessage;
import ru.tgfs.backend.models.TruncateRequest;
import ru.tgfs.backend.services.storage.StorageService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/filesystem")
public class FileUploadController {
    private final StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<ResponseMessage> uploadFile(
        @RequestParam("path") String path, @RequestParam("file") MultipartFile file
    ) {
        System.out.println("===ATTENTION=== UPLOAD");
        String message = "";
        try {
            storageService.store(path, file);
            message = "Uploaded the file successfully: " + file.getOriginalFilename();
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
        } catch (Exception e) {
            System.out.println("GOT ERROR" + e.getMessage());
            message = "Could not upload the file: " + file.getOriginalFilename() + ". Error: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
        }
    }

    @PostMapping("/update")
    public ResponseEntity<Void> updateFile(@RequestBody FileUpdate fileUpdateInfo) {
        System.out.println("===ATTENTION=== UPDATING");
        storageService.updateFile(fileUpdateInfo.path(), fileUpdateInfo.bytes(), fileUpdateInfo.offset());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/update")
    public ResponseEntity<Void> truncateFile(@RequestBody TruncateRequest fileInfo) {
        System.out.println("===ATTENTION=== TRUNCATING");
        storageService.truncateFile(fileInfo.path(), fileInfo.size());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/attributes")
    public ResponseEntity<NodeAttributes> getAttributes(@RequestBody String nodePath) {
        return ResponseEntity.ok().body(storageService.loadAttributes(nodePath));
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileInfo>> getListFiles(@RequestBody String nodePath) {
        List<FileInfo> fileInfos = storageService
                                       .loadDirectory(nodePath)
                                       .stream()
                                       .map(path -> {
                                           String filename = path.getFileName().toString();
                                           Long size = new File(filename).length();
                                           return new FileInfo(filename, size);
                                       }).collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(fileInfos);
    }

    @GetMapping("/file")
    @ResponseBody
    @SneakyThrows
    public ResponseEntity<Resource> getFile(@RequestBody String filename) {
        Resource file = storageService.loadFileAsResource(filename);
        return ResponseEntity.ok()
                             .header(
                                 HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                             .body(file);
    }

    @PatchMapping("/file")
    public ResponseEntity<Void> rename(@RequestBody RenameRequest renameRequest) {
        storageService.rename(renameRequest.oldPath(), renameRequest.newPath());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/file/temp")
    public ResponseEntity<Void> openFile(@RequestBody String filename) {
        storageService.openFile(filename);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/file/temp")
    public ResponseEntity<Void> releaseFile(@RequestBody String filename) {
        storageService.releaseFile(filename);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/directory")
    public ResponseEntity<ResponseMessage> createDir(@RequestBody String directoryPath) {
        String message = "";
        try {
            storageService.createDirectory(directoryPath);
            message = "Created dir successfully: " + directoryPath;
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
        } catch (Exception e) {
            message = "Could not create dir: " + directoryPath + ". Error: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
        }
    }

    @DeleteMapping("/file")
    public void removeFile(@RequestBody String path) {
        System.out.println("REMOVING FILE: " + path);
        storageService.deleteFile(path);
    }

    @DeleteMapping("/directory")
    public void removeDirectory(@RequestBody String path) {
        storageService.deleteDirectory(path);
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException ignored) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(StorageNameAlreadyExistsException.class)
    public ResponseEntity<?> handleStorageNameAlreadyExists(StorageNameAlreadyExistsException ignored) {
        return ResponseEntity.badRequest().build();
    }
}
