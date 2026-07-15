package ru.tgfs.model;

import lombok.With;

@With
public record NodeAttributes(
    boolean isDirectory,
    long size,
    int modificationTime,
    Privileges ownerPrivileges,
    Privileges groupPrivileges,
    Privileges othersPrivileges
) {
}
