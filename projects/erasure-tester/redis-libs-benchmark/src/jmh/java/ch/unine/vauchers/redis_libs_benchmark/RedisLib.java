package ch.unine.vauchers.redis_libs_benchmark;

/**
 *
 */
public interface RedisLib {
    void set(String key, String value);

    String get(String key);

    void set(String key, int value);

    int getInt(String key);

    void set(int key, int value);

    int getInt(int key);
}
