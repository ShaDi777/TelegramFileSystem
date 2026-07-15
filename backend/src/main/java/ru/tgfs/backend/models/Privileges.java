package ru.tgfs.backend.models;

public record Privileges(
    boolean canRead,
    boolean canWrite,
    boolean canExecute
) {
    public static Privileges ALL() {
        return new Privileges(true, true, true);
    }

    public static Privileges RW() {
        return new Privileges(true, true, false);
    }
}
