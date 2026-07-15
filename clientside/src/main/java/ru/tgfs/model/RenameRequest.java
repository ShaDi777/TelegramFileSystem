package ru.tgfs.model;

public record RenameRequest(
    String oldPath,
    String newPath
) {
}
