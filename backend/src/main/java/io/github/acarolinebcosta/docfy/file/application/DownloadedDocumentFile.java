package io.github.acarolinebcosta.docfy.file.application;

import io.github.acarolinebcosta.docfy.file.domain.DocumentFile;

public record DownloadedDocumentFile(
        DocumentFile metadata,
        byte[] content
) {
}
