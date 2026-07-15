package ru.tgfs.backend.services.tdlight;

import it.tdlight.client.SimpleAuthenticationSupplier;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.client.SimpleTelegramClientBuilder;
import it.tdlight.jni.TdApi;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.web.multipart.MultipartFile;
import ru.tgfs.backend.exceptions.StorageException;
import ru.tgfs.backend.exceptions.StorageFileNotFoundException;
import ru.tgfs.backend.exceptions.StorageNameAlreadyExistsException;
import ru.tgfs.backend.models.NodeAttributes;
import ru.tgfs.backend.models.NodeInfo;
import ru.tgfs.backend.models.Privileges;

@Getter
public class TgfsApplication implements AutoCloseable {
    private final SimpleTelegramClient client;
    private final long adminId;
    private long chatId;
    private PinMessageUtils pinMessageUtils;

    public TgfsApplication(
        SimpleTelegramClientBuilder clientBuilder,
        SimpleAuthenticationSupplier<?> authenticationData,
        long adminId
    ) {
        this.adminId = adminId;
        this.client = clientBuilder.build(authenticationData);
        initApp();
    }

    @Override
    public void close() throws Exception {
        client.close();
    }

    @SneakyThrows
    public void saveFile(String path, MultipartFile file) {
        System.out.println(path);
        var req = new TdApi.SendMessage();
        req.chatId = chatId;
        var document = new TdApi.InputMessageDocument();

        var dirPath = Files.createTempDirectory(null);
        var fpath = Files.createFile(Path.of(dirPath.toString(), file.getOriginalFilename()));
        fpath.toFile().deleteOnExit();
        dirPath.toFile().deleteOnExit();
        Files.write(fpath, file.getBytes());

        document.document = new TdApi.InputFileLocal(fpath.toString());
        req.inputMessageContent = document;

        client.send(req).get();
        Thread.sleep(300);
        long messageId = getLastMessageId();
        pinMessageUtils.addPath(path, messageId);
    }

    public void rename(String oldPath, String newPath) {
        var oldInfo = pinMessageUtils.getInfo(oldPath);
        NodeInfo newInfo = null;
        try {
            newInfo = pinMessageUtils.getInfo(newPath);
        } catch (StorageFileNotFoundException ignored) {
        }
        if (oldInfo.isDirectory()) {
            if (newInfo != null && !newInfo.isDirectory()) {
                throw new StorageNameAlreadyExistsException("Couldn't move to: " + newPath);
            } else if (newInfo != null && newInfo.isDirectory()) {
                newPath = newPath + "/" + oldPath.substring(oldPath.lastIndexOf("/"));
            }

            pinMessageUtils.renameDir(oldPath, newPath);
            return;
        }

        pinMessageUtils.deletePath(newPath);
        pinMessageUtils.addPath(newPath, oldInfo.messageId());
        pinMessageUtils.deletePath(oldPath);
    }

    @SneakyThrows
    public void createDir(String path) {
        String text = pinMessageUtils.readPinText();

        System.out.println("Creating dir: " + path);
        pinMessageUtils.addPath(path, null);
    }

    @SneakyThrows
    public void deletePath(String path) {
        var req = new TdApi.DeleteMessages();
        req.chatId = chatId;
        req.messageIds = pinMessageUtils.listMessageIdsByPath(path).stream().mapToLong(Long::longValue).toArray();
        req.revoke = true;
        client.send(req).get();

        pinMessageUtils.deletePath(path);
    }

    public List<String> listDirectory(String path) {
        return pinMessageUtils.listNamesByPath(path);
    }

    @SneakyThrows
    public NodeAttributes getPathInfo(String path) {
        NodeInfo nodeInfo = pinMessageUtils.getInfo(path);

        if (nodeInfo.isDirectory()) {
            return new NodeAttributes(true, 0, 0,
                                      Privileges.ALL(), Privileges.ALL(), Privileges.ALL()
            );
        }

        var req = new TdApi.GetMessage();
        req.chatId = chatId;
        req.messageId = nodeInfo.messageId();
        var message = client.send(req).get(1, TimeUnit.MINUTES);

        long size = switch (message.content) {
            case TdApi.MessageDocument mDocument -> mDocument.document.document.size;
            case TdApi.MessageAudio mAudio -> mAudio.audio.audio.size;
            case TdApi.MessageVideo mVideo -> mVideo.video.video.size;
            case TdApi.MessageVideoNote mVideoNote -> mVideoNote.videoNote.video.size;
            case TdApi.MessagePhoto mPhoto -> mPhoto.photo.sizes[0].photo.size;
            case TdApi.MessageSticker mSticker -> mSticker.sticker.sticker.size;
            case TdApi.MessageAnimation mAnimation -> mAnimation.animation.animation.size;
            case TdApi.MessageAnimatedEmoji mAnimEmoji ->
                mAnimEmoji.animatedEmoji.sound.size + mAnimEmoji.animatedEmoji.sticker.sticker.size;
            default -> 0;
        };

        return new NodeAttributes(false, size, Math.max(message.editDate, message.date),
                                  Privileges.ALL(), Privileges.ALL(), Privileges.ALL()
        );
    }

    @SneakyThrows
    public File readFile(String path) {
        List<Long> messageIds = pinMessageUtils.listMessageIdsByPath(path);
        if (messageIds.isEmpty()) {
            throw new StorageFileNotFoundException("Unknown path: " + path);
        }
        if (messageIds.size() > 1) {
            throw new StorageException("Unknown file: " + path);
        }

        Long msgId = messageIds.get(0);
        var req = new TdApi.GetMessage();
        req.chatId = chatId;
        req.messageId = msgId;
        var message = client.send(req).get();
        if (message.content instanceof TdApi.MessageDocument messageDocument) {
            var downloadRequest = new TdApi.DownloadFile();
            downloadRequest.fileId = messageDocument.document.document.id;
            downloadRequest.priority = 1;
            downloadRequest.offset = 0;
            downloadRequest.limit = 0;
            return new File(client.send(downloadRequest).get().local.path);
        }
        return null;
    }

    @SneakyThrows
    public void writeFile(String path, byte[] bytes, long offset) {
        updateFileData(path, bytes);
    }

    @SneakyThrows
    private void initApp() {
        System.out.println("INIT in TGFS APP");
        TdApi.User me = client.getMeAsync().get(1, TimeUnit.MINUTES);
        var savedMessagesChat = client.send(new TdApi.CreatePrivateChat(me.id, true)).get(1, TimeUnit.MINUTES);
        chatId = savedMessagesChat.id;
        System.out.println("PRIVATE CHAT: " + chatId);

        pinMessageUtils = new PinMessageUtils(this);
    }

    @SneakyThrows
    private long getLastMessageId() {
        Thread.sleep(300);
        var req = new TdApi.GetChatHistory();
        req.chatId = chatId;
        req.limit = 1;
        var history = client.send(req).get();
        return history.messages[0].id;
    }

    @SneakyThrows
    private void updateFileData(String path, byte[] data) {
        var tempDirPath = Files.createTempDirectory(null);
        var tempFile = new File(
            Path.of(
                tempDirPath.toString(),
                path.substring(path.lastIndexOf("/"))
            ).toString()
        );

        tempFile.deleteOnExit();
        tempDirPath.toFile().deleteOnExit();

        Files.write(tempFile.toPath(), data);

        var req = new TdApi.EditMessageMedia();
        var document = new TdApi.InputMessageDocument();
        document.document = new TdApi.InputFileLocal(tempFile.getPath());
        req.inputMessageContent = document;
        req.chatId = chatId;
        req.messageId = pinMessageUtils.listMessageIdsByPath(path).get(0);
        client.send(req).get();
    }
}
