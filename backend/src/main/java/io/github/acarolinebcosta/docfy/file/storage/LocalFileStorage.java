package io.github.acarolinebcosta.docfy.file.storage;

import io.github.acarolinebcosta.docfy.file.config.DocumentFileProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Component
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(DocumentFileProperties properties) {
        this.root = properties.storageLocation()
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public void store(String storageKey, byte[] content) {
        Path target = resolve(storageKey);

        try {
            Files.createDirectories(root);
            Files.write(
                    target,
                    content,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
        } catch (IOException exception) {
            throw new FileStorageException(
                    "Could not store document file",
                    exception
            );
        }
    }

    @Override
    public byte[] load(String storageKey) {
        try {
            return Files.readAllBytes(resolve(storageKey));
        } catch (IOException exception) {
            throw new FileStorageException(
                    "Could not load document file",
                    exception
            );
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            throw new FileStorageException(
                    "Could not remove document file",
                    exception
            );
        }
    }

    private Path resolve(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();

        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }

        return resolved;
    }
}

