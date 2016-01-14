package ch.unine.vauchers.erasuretester.backend;

public interface FailureGenerator {
    boolean isBlockFailed(String key);
}
