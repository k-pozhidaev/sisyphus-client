package io.pozhidaev.sisyphusClient.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class LocalFileStore {
    private Map<String, Item> fileStore;

    public LocalFileStore() {
        fileStore = new ConcurrentHashMap<>();
    }

    public LocalFileStore addUpload(final String fingerprint, final Item data) {
        if (fileStore.containsKey(fingerprint))
        fileStore.putIfAbsent(fingerprint, data);
        return this;
    }

    public LocalFileStore addUpload(
        final String fingerprint,
        final String lastModified,
        final String contentType,
        final Long fileSize
    ) {
        fileStore.putIfAbsent(fingerprint, new Item(lastModified, contentType, fileSize));
        return this;
    }

    public Boolean fileExists(final String fingerprint){
        return fileStore.containsKey(fingerprint);
    }

    public Optional<Item> getUpload(String fingerprint){
        if (fileStore.containsKey(fingerprint)) {
            return Optional.of(fileStore.get(fingerprint));
        }
        return Optional.empty();
    }

    @Data
    @AllArgsConstructor
    static class Item implements UploadFile {
        private String lastModified;
        private String contentType;
        private Long fileSize;
    }
}
