package ch.unine.vauchers.redis_libs_benchmark;


import org.redisson.Redisson;
import org.redisson.core.RMap;

public class RedissonRedisLibBenchmark extends RedisLibBenchmark {

    @Override
    protected RedisLib getLib() {
        return new RedisLib() {
            private RMap<String, String> redis_ss = Redisson.create().getMap("the_bench_map_ss");
            private RMap<String, Integer> redis_si = Redisson.create().getMap("the_bench_map_si");
            private RMap<Integer, Integer> redis_ii = Redisson.create().getMap("the_bench_map_ii");

            @Override
            public void set(String key, String value) {
                redis_ss.put(key, value);
            }

            @Override
            public String get(String key) {
                return redis_ss.get(key);
            }

            @Override
            public void set(String key, int value) {
                redis_si.put(key, value);
            }

            @Override
            public int getInt(String key) {
                return redis_si.get(key);
            }

            @Override
            public void set(int key, int value) {
                redis_ii.put(key, value);
            }

            @Override
            public int getInt(int key) {
                return redis_ii.get(key);
            }
        };
    }
}