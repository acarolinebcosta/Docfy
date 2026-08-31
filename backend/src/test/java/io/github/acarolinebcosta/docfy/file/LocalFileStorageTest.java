package io.github.acarolinebcosta.docfy.file;

import io.github.acarolinebcosta.docfy.file.config.DocumentFileProperties;
import io.github.acarolinebcosta.docfy.file.storage.FileStorageException;
import io.github.acarolinebcosta.docfy.file.storage.LocalFileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void shouldStoreLoadAndDeleteContent() {
        LocalFileStorage storage = storage();
        byte[] content = "Docfy evidence".getBytes();

        storage.store("safe-key.txt", content);

        assertTrue(Files.exists(temporaryDirectory.resolve("safe-key.txt")));
        assertArrayEquals(content, storage.load("safe-key.txt"));

        storage.delete("safe-key.txt");

        assertFalse(Files.exists(temporaryDirectory.resolve("safe-key.txt")));
    }

    @Test
    void shouldRejectTraversalOutsideConfiguredRoot() {
        LocalFileStorage storage = storage();

        assertThrows(
                IllegalArgumentException.class,
                () -> storage.store("../escape.txt", "unsafe".getBytes())
        );
    }

    @Test
    void shouldNeverOverwriteAnExistingStorageKey() {
        LocalFileStorage storage = storage();
        storage.store("collision.txt", "first".getBytes());

        assertThrows(
                FileStorageException.class,
                () -> storage.store("collision.txt", "second".getBytes())
        );
        assertArrayEquals(
                "first".getBytes(),
                storage.load("collision.txt")
        );
    }

    private LocalFileStorage storage() {
        return new LocalFileStorage(
                new DocumentFileProperties(
                        temporaryDirectory,
                        DataSize.ofMegabytes(10)
                )
        );
    }
}
