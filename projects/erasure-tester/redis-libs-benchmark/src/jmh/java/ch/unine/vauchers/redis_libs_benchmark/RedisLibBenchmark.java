package ch.unine.vauchers.redis_libs_benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public abstract class RedisLibBenchmark {
    private RedisLib sut = getLib();
    private int counter_ss = 0;
    private int counter_si = Integer.MAX_VALUE / 3;
    private int counter_ii = (int) ((long) Integer.MAX_VALUE * 2L / 3L);
    private static final int theKey_i = Integer.MAX_VALUE;
    private static final String theKey_s = "well-known-key";

    protected abstract RedisLib getLib();

    @Setup
    public void setup() {
        sut.set(theKey_s, 974328956);
        sut.set(theKey_i, 974328956);
    }

    @Benchmark
    public void storeStringString() {
        sut.set(String.valueOf(++counter_ss), "value");
    }

    @Benchmark
    public String retrieveStringString() {
        return sut.get(theKey_s);
    }

    @Benchmark
    public void storeStringInt() {
        sut.set(String.valueOf(++counter_si), 4284732);
    }

    @Benchmark
    public int retrieveStringInt() {
        return sut.getInt(theKey_s);
    }

    @Benchmark
    public void storeIntInt() {
        sut.set(++counter_ii, 4284732);
    }

    @Benchmark
    public int retrieveIntInt() {
        return sut.getInt(theKey_i);
    }
}