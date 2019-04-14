package io.pozhidaev.sisyphusClient.domain;

public interface UploadFile {
    Long getLastModified();
    void setLastModified(final Long lastModified);

    String getContentType();
    void setContentType(final String contentType);

    Long getFileSize();
    void setFileSize(final Long fileSize);

    Long getUploadLength();
    void setUploadLength(final Long uploadLength);


}
