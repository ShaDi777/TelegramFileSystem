package ru.tgfs.model;

public record TruncateRequest(
    String path,
    Long size
) {
}
