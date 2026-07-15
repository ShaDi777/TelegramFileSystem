package ru.tgfs.filesystem;

import java.util.List;
import jnr.ffi.Pointer;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;
import ru.serce.jnrfuse.struct.Timespec;
import ru.tgfs.model.NodeAttributes;
import ru.tgfs.model.Privileges;
import ru.tgfs.model.ResponseMessage;
import ru.tgfs.service.FileSystemService;

@Component
@RequiredArgsConstructor
public class TelegramFS extends FuseStubFS {
    private final FileSystemService fileSystemService;

    @Override
    public int getattr(String path, FileStat stat) {
        NodeAttributes nodeAttributes = fileSystemService.getAttributes(path);
        if (nodeAttributes == null) {
            return -ErrorCodes.ENOENT();
        }

        if (nodeAttributes.isDirectory()) {
            // setPermissions(stat, nodeAttributes);
            stat.st_mode.set(FileStat.S_IFDIR | FileStat.ALL_READ | FileStat.S_IXUGO | FileStat.S_IRUSR);
            stat.st_nlink.set(2);
            stat.st_mtim.tv_sec.set(nodeAttributes.modificationTime());
        } else {
            if (path.endsWith(".sh") || path.endsWith(".bash")) {
                nodeAttributes = nodeAttributes.withOwnerPrivileges(new Privileges(true, true, true))
                                               .withGroupPrivileges(new Privileges(true, true, true))
                                               .withOthersPrivileges(new Privileges(true, true, true));
            }
            setPermissions(stat, nodeAttributes);
            stat.st_nlink.set(1);
            stat.st_size.set(nodeAttributes.size());
            stat.st_mtim.tv_sec.set(nodeAttributes.modificationTime());
        }

        return 0;
    }

    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filler, @off_t long offset, FuseFileInfo fi) {
        List<String> nodes = fileSystemService.listDir(path);
        if (nodes == null) {
            return -ErrorCodes.ENOENT();
        }

        filler.apply(buf, ".", null, 0);
        filler.apply(buf, "..", null, 0);

        for (String node : nodes) {
            filler.apply(buf, node, null, 0);
        }
        return 0;
    }

    @Override
    public int open(String path, FuseFileInfo fi) {
        if (fileSystemService.getAttributes(path) == null) {
            return -ErrorCodes.ENOENT();
        }

        fileSystemService.openFile(path);
        return 0;
    }

    @Override
    public int release(String path, FuseFileInfo fi) {
        fileSystemService.releaseFile(path);
        return 0;
    }

    @Override
    public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
        final String content = fileSystemService.readFile(path);
        byte[] bytes = content.getBytes();
        int length = bytes.length;

        if (offset < length) {
            if (offset + size > length) {
                size = length - offset;
            }
            buf.put(0, bytes, (int) offset, (int) size);
        } else {
            size = 0;
        }

        return (int) size;
    }

    @Override
    public int mkdir(String path, long mode) {
        ResponseMessage message = fileSystemService.createDir(path);
        if (message == null) {
            return -ErrorCodes.EEXIST();
        }

        return 0;
    }

    @Override
    public int create(String path, long mode, FuseFileInfo fi) {
        ResponseMessage message = fileSystemService.createFile(path);
        if (message == null) {
            return -ErrorCodes.EEXIST();
        }

        return 0;
    }

    @Override
    public int mknod(String path, long mode, long rdev) {
        if (!FileStat.S_ISREG((int) mode)) {
            return -ErrorCodes.EINVAL();
        }

        return create(path, mode, null);
    }

    @Override
    public int unlink(String path) {
        fileSystemService.deleteFile(path);
        return 0;
    }

    @Override
    public int rmdir(String path) {
        fileSystemService.deleteDir(path);
        return 0;
    }

    @Override
    public int write(String path, Pointer buf, long size, long offset, FuseFileInfo fi) {
        byte[] data = new byte[(int) size];
        buf.get(0, data, 0, (int) size);
        fileSystemService.writeFile(path, data, offset);
        return (int) size;
    }

    @Override
    public int truncate(String path, long size) {
        fileSystemService.truncateFile(path, size);
        return 0;
    }

    @Override
    public int rename(String oldpath, String newpath) {
        fileSystemService.rename(oldpath, newpath);
        return 0;
    }

    // TODO either fix or ignore
    @Override
    public int chmod(String path, long mode) {
        return 0;
    }

    @Override
    public int flush(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int utimens(String path, Timespec[] timespec) {
        return 0;
    }

    private void setPermissions(FileStat stat, NodeAttributes nodeAttributes) {
        stat.st_mode.set(
            (nodeAttributes.isDirectory() ? FileStat.S_IFDIR : FileStat.S_IFREG) |
            (nodeAttributes.ownerPrivileges().canRead() ? FileStat.S_IRUSR : 0) |
            (nodeAttributes.ownerPrivileges().canWrite() ? FileStat.S_IWUSR : 0) |
            (nodeAttributes.ownerPrivileges().canExecute() ? FileStat.S_IXUSR : 0) |
            (nodeAttributes.groupPrivileges().canRead() ? FileStat.S_IRGRP : 0) |
            (nodeAttributes.groupPrivileges().canWrite() ? FileStat.S_IWGRP : 0) |
            (nodeAttributes.groupPrivileges().canExecute() ? FileStat.S_IXGRP : 0) |
            (nodeAttributes.othersPrivileges().canRead() ? FileStat.S_IROTH : 0) |
            (nodeAttributes.othersPrivileges().canWrite() ? FileStat.S_IWOTH : 0) |
            (nodeAttributes.othersPrivileges().canExecute() ? FileStat.S_IXOTH : 0)
        );
    }
}