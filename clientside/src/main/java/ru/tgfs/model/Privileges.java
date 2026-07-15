package ru.tgfs.model;

public record Privileges(
    boolean canRead,
    boolean canWrite,
    boolean canExecute
) {
}
