package io.pozhidaev.sisyphusClient.domain.exceptions;


import java.nio.file.Path;

public class FileSizeReadException extends RuntimeException {

    public FileSizeReadException(final Path path, final Throwable cause) {
        super(String.format("File size read exception: %s", path.toAbsolutePath()), cause);
    }
}