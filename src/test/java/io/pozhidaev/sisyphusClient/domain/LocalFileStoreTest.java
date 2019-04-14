package io.pozhidaev.sisyphusClient.domain;

import org.junit.Test;

import static org.junit.Assert.*;

public class LocalFileStoreTest {

    @Test
    public void addUpload() {
        final MemoryFileStore store = new MemoryFileStore();
        store.addUpload("test", 149L, "test/test", 547L, 111L);
        store.getProcessUpload("test")
            .map(item -> {
                assertEquals(item.getContentType(), "test/test");
                assertEquals(item.getFileSize(), Long.valueOf(547));
                assertEquals(item.getLastModified(), Long.valueOf(149));
                assertEquals(item.getUploadLength(), Long.valueOf(111));
                return "";
            })
            .orElseGet(() -> {
                fail();
                return "";
            });
        store.getProcessUpload("none")
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