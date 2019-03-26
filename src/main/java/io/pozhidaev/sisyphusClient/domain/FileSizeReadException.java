package io.pozhidaev.sisyphusClient.domain;


import java.nio.file.Path;

public class FileSizeReadException extends RuntimeException {

    public FileSizeReadException(Path path, Throwable cause) {
        super(String.format("File size read exception: %s", path.toAbsolutePath()), cause);
    }
}