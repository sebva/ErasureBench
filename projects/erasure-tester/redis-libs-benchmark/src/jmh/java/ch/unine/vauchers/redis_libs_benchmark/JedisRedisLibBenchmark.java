package ch.unine.vauchers.redis_libs_benchmark;


import redis.clients.jedis.Jedis;

public class JedisRedisLibBenchmark extends RedisLibBenchmark {

    @Override
    protected RedisLib getLib() {
        return new RedisLib() {
            Jedis redis = new Jedis();

            @Override
            public void set(String key, String value) {
                redis.set(key, value);
            }

            @Override
            public String get(String key) {
                return redis.get(key);
            }

            @Override
            public void set(String key, int value) {
                redis.set(key, String.valueOf(value));
            }

            @Override
            public int getInt(String key) {
                return Integer.parseInt(redis.get(key));
            }

            @Override
            public void set(int key, int value) {
                redis.set(String.valueOf(key), String.valueOf(value));
            }

            @Override
            public int getInt(int key) {
                return Integer.parseInt(redis.get(String.valueOf(key)));
            }
        };
    }
}