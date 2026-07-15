package ru.tgfs.backend.models;

public record TruncateRequest(
    String path,
    Long size
) {
}
