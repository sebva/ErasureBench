package ch.unine.vauchers.erasuretester.backend;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 *
 */
public class MemoryStorageBackend extends StorageBackend {
    private Map<String, Integer> blocksStorage;
    private Map<String, FileMetadata> metadataStorage;

    public MemoryStorageBackend(@NotNull FailureGenerator failureGenerator) {
        super(failureGenerator);

        blocksStorage = new HashMap<>();
        metadataStorage = new HashMap<>();
    }

    @Override
    public Optional<FileMetadata> getFileMetadata(@NotNull String path) {
        return Optional.ofNullable(metadataStorage.get(path));
    }

    @Override
    public void setFileMetadata(@NotNull String path, @NotNull FileMetadata metadata) {
        metadataStorage.put(path, metadata);
    }

    @Override
    public Optional<Integer> retrieveBlock(@NotNull String key) {
        return Optional.ofNullable(blocksStorage.get(key));
    }

    @Override
    public void storeBlock(@NotNull String key, int blockData) {
        blocksStorage.put(key, blockData);
    }

    @Override
    protected boolean isBlockFailed(@NotNull String key) {
        return false;
    }
}
