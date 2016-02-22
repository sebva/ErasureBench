package ch.unine.vauchers.erasuretester.backend;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Storage backend implementation backed by a plain old Java Map object.
 * Asynchronous operations are done synchronously.
 */
public class MemoryStorageBackend extends StorageBackend {
    protected Map<Long, String> blocksStorage;
    protected Map<String, FileMetadata> metadataStorage;

    public MemoryStorageBackend() {
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
    public Optional<String> retrieveAggregatedBlocks(long key) {
        return Optional.ofNullable(blocksStorage.get(key));
    }

    @Override
    protected void storeAggregatedBlocks(long key, String blockData) {
        blocksStorage.put(key, blockData);
    }

    @Override
    public boolean isAggregatedBlockAvailable(long key) {
        return blocksStorage.containsKey(key);
    }

    @Override
    public void disconnect() {}
}
