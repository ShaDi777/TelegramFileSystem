package ru.tgfs.model;

public record ChangeFile(
    String path,
    byte[] bytes,
    Long offset
) {
}
