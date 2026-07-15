package ru.tgfs.backend.models;

public record RenameRequest(
    String oldPath,
    String newPath
) {
}
