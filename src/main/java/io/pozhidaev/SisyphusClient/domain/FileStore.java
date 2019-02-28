package io.pozhidaev.SisyphusClient.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FileStore<T> {
    private Map<T, String> fileStore;

    public FileStore() {
        fileStore = new HashMap<>();
    }

    public FileStore<T> addUpload(T fingerprint, String data) {
        fileStore.putIfAbsent(fingerprint, data);
        return this;
    }

    public Optional<String> getUpload(T fingerprint){
        if (fileStore.containsKey(fingerprint)) {
            return Optional.of(fileStore.get(fingerprint));
        }
        return Optional.empty();
    }
}
