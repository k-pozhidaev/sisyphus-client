package io.pozhidaev.sisyphusClient.domain;

import java.util.Optional;

public interface FileStorage {

    Optional<UploadFile> getUpload(String fingerprint);

    FileStorage addUpload(
            final String fingerprint,
            final Long lastModified,
            final String contentType,
            final Long fileSize
    );
}
