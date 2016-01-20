package ch.unine.vauchers.erasuretester.backend;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 *
 */
public class MemoryStorageBackend extends StorageBackend {
    protected Map<String, Integer> blocksStorage;
    protected Map<String, FileMetadata> metadataStorage;

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
    public Future<Integer> retrieveBlockAsync(@NotNull String key) {
        return CompletableFuture.completedFuture(retrieveBlock(key).get());
    }

    @Override
    public Future<Map<String, Integer>> retrieveAllBlocksAsync(@NotNull Set<String> keys) {
        Map<String, Integer> ret = new HashMap<>(keys.size());
        keys.forEach(key -> retrieveBlock(key).ifPresent((value) -> ret.put(key, value)));
        return CompletableFuture.completedFuture(ret);
    }

    @Override
    public void storeBlock(@NotNull String key, int blockData) {
        blocksStorage.put(key, blockData);
    }

    @Override
    public Future<Boolean> storeBlockAsync(@NotNull String key, int blockData) {
        storeBlock(key, blockData);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    protected boolean isBlockFailed(@NotNull String key) {
        return !blocksStorage.containsKey(key);
    }
}
