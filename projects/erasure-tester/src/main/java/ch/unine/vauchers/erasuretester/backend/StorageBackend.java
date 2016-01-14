package ch.unine.vauchers.erasuretester.backend;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class StorageBackend {
    @NotNull
    private FailureGenerator failureGenerator;

    protected StorageBackend(@NotNull FailureGenerator failureGenerator) {
        this.failureGenerator = failureGenerator;
    }

    public abstract Optional<FileMetadata> getFileMetadata(@NotNull String path);

    public abstract void setFileMetadata(@NotNull String path, @NotNull FileMetadata metadata);

    public abstract Optional<Integer> retrieveBlock(@NotNull String key);

    public abstract void storeBlock(@NotNull String key, int blockData);

    public final boolean isBlockAvailable(@NotNull String key) {
        return !(failureGenerator.isBlockFailed(key) || this.isBlockFailed(key));
    }

    protected abstract boolean isBlockFailed(@NotNull String key);

}
