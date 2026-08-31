package io.github.acarolinebcosta.docfy.file.storage;

public interface FileStorage {

    void store(String storageKey, byte[] content);

    byte[] load(String storageKey);

    void delete(String storageKey);
}
