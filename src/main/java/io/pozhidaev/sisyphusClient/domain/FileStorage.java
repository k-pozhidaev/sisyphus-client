package io.pozhidaev.sisyphusClient.domain;

import java.util.Optional;

public interface FileStorage {

    Optional<UploadFile> getProcessUpload(final String fingerprint);

    FileStorage addUpload(
            final String fingerprint,
            final Long lastModified,
            final String contentType,
            final Long fileSize,
            final Long uploadLength
    );

    FileStorage updateLength(
        final String fingerprint,
        final Long uploadLength
    );

    FileStorage failUpload(
        final String fingerprint
    );

    FileStorage completeUpload(
        final String fingerprint
    );



}
