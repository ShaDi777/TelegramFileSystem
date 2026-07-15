package ru.tgfs.backend.services.tdlight;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.tdlight.jni.TdApi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import ru.tgfs.backend.exceptions.StorageException;
import ru.tgfs.backend.exceptions.StorageFileNotFoundException;
import ru.tgfs.backend.exceptions.StorageNameAlreadyExistsException;
import ru.tgfs.backend.models.NodeInfo;

public class PinMessageUtils {
    private Map<String, Object> fileSystemMap;

    private final TgfsApplication app;
    private final Long pinMessageId;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public PinMessageUtils(TgfsApplication app) {
        var getPinMessageRequest = new TdApi.GetChatPinnedMessage(app.getChatId());
        try {
            app.getClient().send(getPinMessageRequest).get();
        } catch (Exception e) {
            createPinMessage(app, app.getChatId());
        }

        this.pinMessageId = app.getClient().send(getPinMessageRequest).get().id;
        this.app = app;
    }

    @SneakyThrows
    public String readPinText() {
        var readRequest = new TdApi.GetMessage(app.getChatId(), pinMessageId);
        TdApi.MessageContent oldContent = app.getClient().send(readRequest).get().content;
        String text = "";
        if (oldContent instanceof TdApi.MessageText messageText) {
            text = messageText.text.text;
        }

        fileSystemMap = objectMapper.readValue(text, Map.class);
        return text;
    }

    public NodeInfo getInfo(String path) {
        readPinText();

        var entry = traversePath(path);
        var currentMap = entry.getKey();
        var nodeName = entry.getValue();
        if (nodeName.isBlank()) {
            return new NodeInfo(path, true, null);
        }

        if (!currentMap.containsKey(nodeName)) {
            throw new StorageFileNotFoundException("Path not found: " + path);
        }

        var end = currentMap.get(nodeName);
        if (end instanceof Integer intValue) {
            return new NodeInfo(path, false, intValue.longValue());
        }
        if (end instanceof Long longValue) {
            return new NodeInfo(path, false, longValue);
        }
        return new NodeInfo(path, true, null);
    }

    public void addPath(String path, Long messageId) {
        String text = readPinText();

        var entry = traversePath(path);
        var currentMap = entry.getKey();
        var appendPath = entry.getValue();

        if (currentMap.containsKey(appendPath)) {
            throw new StorageNameAlreadyExistsException("Path already exists: " + path);
        }
        currentMap.put(appendPath, Objects.requireNonNullElseGet(messageId, HashMap::new));

        changePinText();
    }

    public List<String> listNamesByPath(String path) {
        String text = readPinText();

        var entry = traversePath(path);
        if (entry.getValue().isBlank()) {
            return entry.getKey().keySet().stream().toList();
        }
        if (entry.getKey().get(entry.getValue()) instanceof Map<?, ?> map) {
            return ((Map<String, Object>) map).keySet().stream().toList();
        }

        throw new StorageException("Can not list regular file: " + path);
    }

    public List<Long> listMessageIdsByPath(String path) {
        String text = readPinText();

        var entry = traversePath(path);
        var end = entry.getValue().isBlank() ? entry.getKey() : entry.getKey().get(entry.getValue());

        if (end instanceof Integer intValue) {
            return List.of(intValue.longValue());
        }
        if (end instanceof Long longValue) {
            return List.of(longValue);
        }

        return getIdsFromMap((Map<String, Object>) end);
    }

    private List<Long> getIdsFromMap(Map<String, Object> map) {
        List<Long> ids = new ArrayList<>();
        for (var entry : map.entrySet()) {
            if (entry.getValue() instanceof Integer intValue) {
                ids.add(intValue.longValue());
            } else if (entry.getValue() instanceof Long longValue) {
                ids.add(longValue);
            } else if (entry.getValue() instanceof Map m) {
                ids.addAll(getIdsFromMap(m));
            }
        }

        return ids;
    }

    public void deletePath(String path) {
        String text = readPinText();

        var entry = traversePath(path);
        var parentDir = entry.getKey();
        var nodeName = entry.getValue();

        if (nodeName.isBlank()) {
            throw new IllegalArgumentException("You can't delete filesystem root directory.");
        }

        parentDir.remove(nodeName);
        changePinText();
    }

    public void renameDir(String oldPath, String newPath) {
        readPinText();

        var oldEntry = traversePath(oldPath);
        var oldParentDir = oldEntry.getKey();
        var oldNodeName = oldEntry.getValue();

        var newEntry = traversePath(newPath);
        var newParentDir = newEntry.getKey();
        var newNodeName = newEntry.getValue();

        newParentDir.put(newNodeName, oldParentDir.remove(oldNodeName));

        changePinText();
    }

    @SneakyThrows
    private static long createPinMessage(TgfsApplication app, long chatId) {
        var req = new TdApi.SendMessage();
        req.chatId = chatId;
        var txt = new TdApi.InputMessageText();
        txt.text = new TdApi.FormattedText("{}", new TdApi.TextEntity[0]);
        req.inputMessageContent = txt;
        TdApi.Message result = app.getClient().sendMessage(req, true).get(1, TimeUnit.MINUTES);

        var pinMsgReq = new TdApi.PinChatMessage();
        pinMsgReq.chatId = chatId;
        pinMsgReq.messageId = result.id;
        pinMsgReq.disableNotification = true;
        app.getClient().send(pinMsgReq).get(1, TimeUnit.MINUTES);

        return result.id;
    }

    @SneakyThrows
    private void changePinText() {
        if (fileSystemMap == null) {
            fileSystemMap = new HashMap<>();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        String text = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fileSystemMap);

        var modifyRequest = new TdApi.EditMessageText();
        modifyRequest.chatId = app.getChatId();
        modifyRequest.messageId = pinMessageId;
        var content = new TdApi.InputMessageText();
        content.text = new TdApi.FormattedText(text, new TdApi.TextEntity[0]);
        modifyRequest.inputMessageContent = content;
        app.getClient().send(modifyRequest).get();
    }

    private Map.Entry<Map<String, Object>, String> traversePath(String path) {
        if(fileSystemMap == null) {
            throw new IllegalStateException("Filesystem is not initialized. Read it from pinned message.");
        }

        String[] parts = path.split("/");
        Map<String, Object> currentMap = fileSystemMap;

        int i = 0;
        for (; i < parts.length - 1; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            if (!currentMap.containsKey(part)) {
                throw new StorageFileNotFoundException("No such path while traversing: " + path);
            }

            if (currentMap.get(part) instanceof Map<?,?> map) {
                currentMap = (Map<String, Object>) map;
            } else {
                throw new StorageFileNotFoundException("No such directory while traversing: " + part);
            }
        }

        String lastPath = i < parts.length ? parts[i] : "";
        return Map.entry(currentMap, lastPath);
    }
}
