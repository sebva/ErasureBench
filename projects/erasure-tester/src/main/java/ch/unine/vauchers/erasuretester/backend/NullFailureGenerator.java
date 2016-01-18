package ch.unine.vauchers.erasuretester.backend;

public class NullFailureGenerator implements FailureGenerator {

    @Override
    public boolean isBlockFailed(String key) {
        return false;
    }
}
