package ru.tgfs.backend.models;

public record FileUpdate(
    String path,
    byte[] bytes,
    Long offset
) {
}
