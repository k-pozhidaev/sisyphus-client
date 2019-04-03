package io.pozhidaev.sisyphusClient.domain;

public interface UploadFile {
    String getLastModified();
    void setLastModified(final String lastModified);

    String getContentType();
    void setContentType(final String contentType);

    Long getFileSize();
    void setFileSize(final Long fileSize);


}
