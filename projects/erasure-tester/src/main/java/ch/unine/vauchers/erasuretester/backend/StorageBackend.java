package ch.unine.vauchers.erasuretester.backend;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;

public abstract class StorageBackend {
    public abstract Optional<FileMetadata> getFileMetadata(@NotNull String path);

    public abstract void setFileMetadata(@NotNull String path, @NotNull FileMetadata metadata);

    public abstract Optional<Integer> retrieveBlock(@NotNull String key);

    public abstract Future<Integer> retrieveBlockAsync(@NotNull String key);

    public abstract Future<Map<String, Integer>> retrieveAllBlocksAsync(@NotNull Set<String> keys);

    public abstract void storeBlock(@NotNull String key, int blockData);

    public abstract Future<Boolean> storeBlockAsync(@NotNull String key, int blockData);

    public abstract Future<Boolean> isBlockAvailableAsync(@NotNull String key);

    public abstract void disconnect();
}
