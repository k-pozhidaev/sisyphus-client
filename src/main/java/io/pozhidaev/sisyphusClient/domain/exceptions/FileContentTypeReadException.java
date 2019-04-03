package io.pozhidaev.sisyphusClient.domain.exceptions;

import java.nio.file.Path;

public class FileContentTypeReadException extends RuntimeException{
    public FileContentTypeReadException(Path path, Throwable cause) {
        super(String.format("File content type read exception: %s", path.toAbsolutePath()), cause);
    }
}
