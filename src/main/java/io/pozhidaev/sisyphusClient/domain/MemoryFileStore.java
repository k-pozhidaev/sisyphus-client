package io.pozhidaev.sisyphusClient.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryFileStore implements FileStorage {
    private ConcurrentHashMap<String, UploadFile> processFileStore;
    private ConcurrentHashMap<String, UploadFile> failedFileStore;
    private ConcurrentHashMap<String, UploadFile> completedFileStore;

    public MemoryFileStore() {
        processFileStore = new ConcurrentHashMap<>();
        failedFileStore = new ConcurrentHashMap<>();
        completedFileStore = new ConcurrentHashMap<>();
    }

    @Override
    public MemoryFileStore addUpload(
        final String fingerprint,
        final Long lastModified,
        final String contentType,
        final Long fileSize,
        final Long uploadLength
    ) {
        processFileStore.putIfAbsent(fingerprint, new Item(lastModified, contentType, fileSize, uploadLength));
        return this;
    }

    @Override
    public Optional<UploadFile> getProcessUpload(final String fingerprint){
        if (processFileStore.containsKey(fingerprint)) {
            return Optional.of(processFileStore.get(fingerprint));
        }
        return Optional.empty();
    }

    @Override
    public MemoryFileStore updateLength(final String fingerprint, final Long uploadLength) {
        final UploadFile item = processFileStore.get(fingerprint);
        if(item == null) {
            throw new NullPointerException(String.format("Item %s not exists in process file store.", fingerprint));
        }
        item.setUploadLength(uploadLength);
        return this;
    }

    @Override
    public FileStorage failUpload(final String fingerprint) {
        failedFileStore.computeIfAbsent(fingerprint, this::moveElement);
        return this;
    }

    @Override
    public MemoryFileStore completeUpload(final String fingerprint) {
        completedFileStore.computeIfAbsent(fingerprint, this::moveElement);
        return this;
    }

    private UploadFile moveElement(final String fingerprint) {
        final UploadFile file = processFileStore.get(fingerprint);
        processFileStore.remove(fingerprint);
        return file;
    }

    @Data
    @AllArgsConstructor
    static class Item implements UploadFile {
        private Long lastModified;
        private String contentType;
        private Long fileSize;
        private Long uploadLength;
    }
}
