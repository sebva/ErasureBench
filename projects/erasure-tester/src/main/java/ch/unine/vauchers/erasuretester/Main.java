package ch.unine.vauchers.erasuretester;

import ch.unine.vauchers.erasuretester.backend.JedisStorageBackend;
import ch.unine.vauchers.erasuretester.backend.MemoryStorageBackend;
import ch.unine.vauchers.erasuretester.backend.RedissonStorageBackend;
import ch.unine.vauchers.erasuretester.backend.StorageBackend;
import ch.unine.vauchers.erasuretester.erasure.FileEncoderDecoder;
import ch.unine.vauchers.erasuretester.erasure.SimpleRegeneratingFileEncoderDecoder;
import ch.unine.vauchers.erasuretester.erasure.codes.ErasureCode;
import ch.unine.vauchers.erasuretester.erasure.codes.NullErasureCode;
import ch.unine.vauchers.erasuretester.erasure.codes.ReedSolomonCode;
import ch.unine.vauchers.erasuretester.erasure.codes.SimpleRegeneratingCode;
import ch.unine.vauchers.erasuretester.frontend.FuseMemoryFrontend;
import ch.unine.vauchers.erasuretester.utils.Utils;
import net.fusejna.FuseException;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entry-point of the application
 */
public class Main {

    public static void main(String[] argv) throws FuseException {
        System.out.println("Erasure tester started");

        ArgumentParser parser = ArgumentParsers.newArgumentParser("Erasure tester");
        parser.addArgument("-c", "--erasure-code")
                .choices("Null", "ReedSolomon", "SimpleRegenerating")
                .setDefault("Null");
        parser.addArgument("-s", "--storage")
                .choices("Memory", "Jedis", "Redisson")
                .setDefault("Memory");
        parser.addArgument("-r", "--stripe")
                .help("Stripe size")
                .type(Integer.TYPE)
                .setDefault(10);
        parser.addArgument("-p", "--parity")
                .help("Parity size")
                .type(Integer.TYPE)
                .setDefault(4);
        parser.addArgument("--src")
                .help("Parity size SRC, only used for Simple regenerating code")
                .type(Integer.TYPE)
                .setDefault(2);
        parser.addArgument("--redis-cluster")
                .help("Flag the Redis server in use as part of a cluster")
                .action(Arguments.storeTrue());
        parser.addArgument("-q", "--quiet")
                .help("Disable logging")
                .action(Arguments.storeTrue());
        parser.addArgument("mountpoint").setDefault("/mnt/erasure");

        try {
            final Namespace namespace = parser.parseArgs(argv);
            startProgram(namespace);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
        }
    }

    /**
     * Start the components and mount the filesystem using the parameters passed on the command-line
     * @param namespace The parameters returned by argparse4j
     * @throws FuseException
     */
    private static void startProgram(Namespace namespace) throws FuseException {
        if (namespace.getBoolean("quiet")) {
            // Disable logging completely for faster performance
            Utils.disableLogging();
        }

        final ErasureCode erasureCode;
        final int stripe = namespace.getInt("stripe");
        final int parity = namespace.getInt("parity");
        final int src = namespace.getInt("src");

        switch (namespace.getString("erasure_code")) {
            case "Null":
            default:
                erasureCode = new NullErasureCode(stripe);
                break;
            case "ReedSolomon":
                erasureCode = new ReedSolomonCode(stripe, parity);
                break;
            case "SimpleRegenerating":
                erasureCode = new SimpleRegeneratingCode(stripe, parity, src);
                break;
        }

        final StorageBackend storageBackend;
        switch (namespace.getString("storage")) {
            case "Memory":
            default:
                storageBackend = new MemoryStorageBackend();
                break;
            case "Jedis":
                storageBackend = new JedisStorageBackend(namespace.getBoolean("redis_cluster"));
                break;
            case "Redisson":
                storageBackend = new RedissonStorageBackend();
                break;
        }

        final FileEncoderDecoder encdec;
        switch (namespace.getString("erasure_code")) {
            case "SimpleRegenerating":
                encdec = new SimpleRegeneratingFileEncoderDecoder((SimpleRegeneratingCode) erasureCode, storageBackend);
                break;
            default:
                encdec = new FileEncoderDecoder(erasureCode, storageBackend);
                break;
        }

        final FuseMemoryFrontend fuse = new FuseMemoryFrontend(encdec, !namespace.getBoolean("quiet"));
        // Gracefully quit on Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Gracefully exiting Erasure tester");
                try {
                    storageBackend.disconnect();
                    fuse.unmount();
                } catch (IOException | FuseException e) {
                    e.printStackTrace();
                }
            }
        });

        fuse.mount(new File(namespace.getString("mountpoint")), false);

        final Scanner reader = new Scanner(System.in);
        final Pattern pattern = Pattern.compile("^repair (.+)$");
        while (true) {
            final String line = reader.nextLine();
            if ("repairAll".equals(line)) {
                encdec.repairAllFiles();
                System.out.println("Done");
            } else if ("clearCache".equals(line)) {
                storageBackend.clearReadCache();
                System.out.println("Done");
            } else {
                final Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    encdec.repairFile(matcher.group(1));
                    System.out.println("Done");
                } else {
                    System.out.println("Unknown command");
                }
            }
        }
    }

}
