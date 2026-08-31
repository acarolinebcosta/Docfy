package io.github.acarolinebcosta.docfy.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

@ConfigurationProperties("docfy.files")
public record DocumentFileProperties(
        Path storageLocation,
        DataSize maxSize
) {

    public DocumentFileProperties {
        if (storageLocation == null) {
            throw new IllegalArgumentException(
                    "Document file storage location is required"
            );
        }

        if (maxSize == null || maxSize.toBytes() <= 0) {
            throw new IllegalArgumentException(
                    "Document file maximum size must be positive"
            );
        }
    }
}
