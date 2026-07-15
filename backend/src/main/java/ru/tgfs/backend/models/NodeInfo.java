package ru.tgfs.backend.models;

public record NodeInfo(
    String path,
    Boolean isDirectory,
    Long messageId
) {
}
