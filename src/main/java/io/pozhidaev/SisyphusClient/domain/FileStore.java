package io.pozhidaev.SisyphusClient.domain;

import java.util.HashMap;
import java.util.Map;

public class FileStore {
    private Map<String, String> fileStore;

    public FileStore() {
        fileStore = new HashMap<>();
    }
}
