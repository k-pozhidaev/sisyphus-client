package io.pozhidaev.sisyphusClient.domain;

import org.junit.Test;

import static org.junit.Assert.*;

public class LocalFileStoreTest {

    @Test
    public void addUpload() {
        final LocalFileStore store = new LocalFileStore();
        store.addUpload("test", 149L, "test/test", 547L);
        store.getUpload("test")
            .map(item -> {
                assertEquals(item.getContentType(), "test/test");
                assertEquals(item.getFileSize(), Long.valueOf(547));
                assertEquals(item.getLastModified(), Long.valueOf(149));
                return "";
            })
            .orElseGet(() -> {
                fail();
                return "";
            });
        store.getUpload("none")
            .map(item -> {
                fail();
                return "";
            })
            .orElseGet(() -> {
            assertTrue(true);
            return "";
        });
    }
}