package ru.tgfs.backend.exceptions;

public class StorageNameAlreadyExistsException extends StorageException {
    public StorageNameAlreadyExistsException(String message) {
        super(message);
    }

    public StorageNameAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
