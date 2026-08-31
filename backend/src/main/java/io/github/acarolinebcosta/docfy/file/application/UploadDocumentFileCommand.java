package io.github.acarolinebcosta.docfy.file.application;

public record UploadDocumentFileCommand(
        String originalFilename,
        String contentType,
        byte[] content
) {
}
