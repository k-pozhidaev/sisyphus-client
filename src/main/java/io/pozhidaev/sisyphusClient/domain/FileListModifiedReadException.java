package io.pozhidaev.sisyphusClient.domain;

import java.nio.file.Path;

public class FileListModifiedReadException extends RuntimeException {
    public FileListModifiedReadException (final Path path, final Throwable cause) {
        super(String.format("File size read exception: %s", path.toAbsolutePath()), cause);
    }
}
