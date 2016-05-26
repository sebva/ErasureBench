package ch.unine.vauchers.erasuretester.backend;

import com.netflix.dyno.connectionpool.Host;
import com.netflix.dyno.connectionpool.HostSupplier;
import com.netflix.dyno.connectionpool.TokenMapSupplier;
import com.netflix.dyno.connectionpool.impl.lb.HostToken;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
final class DynoHostSupplier implements TokenMapSupplier, HostSupplier {
    private final OkHttpClient httpClient;
    private volatile Map<String, DynomiteNode> nodes = Collections.emptyMap();
    private volatile long lastFetch = -1;

    DynoHostSupplier() {
        httpClient = new OkHttpClient();
    }

    private synchronized void fetch() {
        if (System.nanoTime() - lastFetch < 10_000_000_000L) {
            return;
        }

        try {
            final Response response = httpClient.newCall(new Request.Builder()
                    .url("http://erasuretester_benchmark_1:4321/REST/v1/admin/get_seeds")
                    .build())
                    .execute();
            if (response.isSuccessful()) {
                // ec2-54-145-17-101.compute-1.amazonaws.com:8101:rack1:dc:1383429731
                final String florida = response.body().string();
                nodes = Arrays.stream(florida.split("\n")[0].split("[|]"))
                        .map(s -> {
                            String[] components = s.split(":");
                            return new DynomiteNode(components[0], Long.parseLong(components[4]), 8102);
                        })
                        .collect(Collectors.toMap(node -> node.hostname, node -> node));
                lastFetch = System.nanoTime();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Collection<Host> getHosts() {
        fetch();
        return nodes.values().parallelStream()
                .map(node -> new Host(node.hostname, node.port, Host.Status.Up).setRack("rack1"))
                .collect(Collectors.toList());
    }

    @Override
    public List<HostToken> getTokens(Set<Host> activeHosts) {
        fetch();
        return activeHosts.stream()
                .map(node -> new HostToken(nodes.get(node.getHostName()).token, node))
                .collect(Collectors.toList());
    }

    @Override
    public HostToken getTokenForHost(Host host, Set<Host> activeHosts) {
        fetch();
        return new HostToken(nodes.get(host.getHostName()).token, host);
    }

    private static final class DynomiteNode {
        final String hostname;
        final long token;
        final int port;

        DynomiteNode(String hostname, long token, int port) {
            this.hostname = hostname;
            this.token = token;
            this.port = port;
        }
    }
}
