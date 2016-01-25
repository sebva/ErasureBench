package ch.unine.vauchers.erasuretester.backend;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Storage backend implementation backed by a plain old Java Map object.
 * Asynchronous operations are done synchronously.
 */
public class MemoryStorageBackend extends StorageBackend {
    protected Map<Long, Integer> blocksStorage;
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
    public Optional<Integer> retrieveBlock(long key) {
        return Optional.ofNullable(blocksStorage.get(key));
    }

    @Override
    public Future<Integer> retrieveBlockAsync(long key) {
        return CompletableFuture.completedFuture(retrieveBlock(key).get());
    }

    @Override
    public Future<Map<Long, Integer>> retrieveAllBlocksAsync(@NotNull Set<Long> keys) {
        Map<Long, Integer> ret = new HashMap<>(keys.size());
        keys.forEach(key -> retrieveBlock(key).ifPresent((value) -> ret.put(key, value)));
        return CompletableFuture.completedFuture(ret);
    }

    @Override
    public void storeBlock(long key, int blockData) {
        blocksStorage.put(key, blockData);
    }

    @Override
    public Future<Boolean> storeBlockAsync(long key, int blockData) {
        storeBlock(key, blockData);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Future<Boolean> isBlockAvailableAsync(long key) {
        return CompletableFuture.completedFuture(blocksStorage.containsKey(key));
    }

    @Override
    public void disconnect() {}
}
