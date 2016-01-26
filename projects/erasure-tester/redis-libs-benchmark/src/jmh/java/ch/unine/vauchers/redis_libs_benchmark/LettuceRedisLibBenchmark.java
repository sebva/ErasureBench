package ch.unine.vauchers.redis_libs_benchmark;


import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.LettuceCharsets;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.CoderResult.OVERFLOW;

public class LettuceRedisLibBenchmark extends RedisLibBenchmark {

    @Override
    protected RedisLib getLib() {
        return new RedisLib() {
            private RedisURI uri = new RedisURI("localhost", 6379, 2, TimeUnit.SECONDS);
            RedisCommands<String, String> redis_ss = RedisClient.create(uri).connect().sync();
            RedisCommands<String, Integer> redis_si = RedisClient.create(uri).connect(new RedisCodec<String, Integer>() {

                private final byte[] EMPTY = new byte[0];

                private Charset charset = LettuceCharsets.UTF8;
                private CharsetDecoder decoder = charset.newDecoder();
                private CharBuffer chars = CharBuffer.allocate(1024);

                @Override
                public synchronized String decodeKey(ByteBuffer bytes) {
                    chars.clear();
                    bytes.mark();

                    decoder.reset();
                    while (decoder.decode(bytes, chars, true) == OVERFLOW || decoder.flush(chars) == OVERFLOW) {
                        chars = CharBuffer.allocate(chars.capacity() * 2);
                        bytes.reset();
                    }

                    return chars.flip().toString();
                }

                @Override
                public ByteBuffer encodeKey(String string) {
                    if (string == null) {
                        return ByteBuffer.wrap(EMPTY);
                    }

                    return charset.encode(string);
                }

                @Override
                public Integer decodeValue(ByteBuffer bytes) {
                    return bytes.getInt();
                }

                @Override
                public ByteBuffer encodeValue(Integer value) {
                    return ByteBuffer.allocate(Integer.BYTES).putInt(value);
                }
            }).sync();
            RedisCommands<Integer, Integer> redis_ii = RedisClient.create(uri).connect(new RedisCodec<Integer, Integer>() {
                @Override
                public Integer decodeKey(ByteBuffer bytes) {
                    return bytes.getInt();
                }

                @Override
                public Integer decodeValue(ByteBuffer bytes) {
                    return bytes.getInt();
                }

                @Override
                public ByteBuffer encodeKey(Integer key) {
                    return ByteBuffer.allocate(Integer.BYTES).putInt(key);
                }

                @Override
                public ByteBuffer encodeValue(Integer value) {
                    return ByteBuffer.allocate(Integer.BYTES).putInt(value);
                }
            }).sync();

            @Override
            public void set(String key, String value) {
                redis_ss.set(key, value);
            }

            @Override
            public String get(String key) {
                return redis_ss.get(key);
            }

            @Override
            public void set(String key, int value) {
                redis_si.set(key, value);
            }

            @Override
            public int getInt(String key) {
                return redis_si.get(key);
            }

            @Override
            public void set(int key, int value) {
                redis_ii.set(key, value);
            }

            @Override
            public int getInt(int key) {
                return redis_ii.get(key);
            }
        };
    }
}