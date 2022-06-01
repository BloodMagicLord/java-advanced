package info.kgeorgiy.ja.Maksonov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class GeneralCode {
    public static final int TIMEOUT = 20;
    public static final Charset UTF_8 = StandardCharsets.UTF_8;
    public static final int WRITE = SelectionKey.OP_WRITE;
    public static final int READ = SelectionKey.OP_READ;
    private static final String ServerExpectedArgs = "Expected <port> <threads>.";
    private static final String ClientExpectedArgs = "Expected <host> <port> <prefix> <threads> <requests>.";

    //===================================================================//

    public static void launchClient(HelloClient client, String[] args) {
        if (args == null || args.length != 5) {
            throw new IllegalArgumentException(ClientExpectedArgs);
        }

        try {
            final String host = args[0];
            final int port = Integer.parseInt(args[1]);
            final String prefix = args[2];
            final int threads = Integer.parseInt(args[3]);
            final int requests = Integer.parseInt(args[4]);

            client.run(host, port, prefix, threads, requests);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(ClientExpectedArgs);
        }
    }

    public static void launchServer(HelloServer server, String[] args) throws IllegalArgumentException {
        if (args == null || args.length != 2) {
            throw new IllegalArgumentException(ServerExpectedArgs);
        }

        try {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);
            server.start(port, threads);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(ServerExpectedArgs);
        }
    }

    public static DatagramChannel openChannel() throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        return channel;
    }

    public static void setKeys(Selector selector, int ops) throws IOException {
        if (selector.select(TIMEOUT) == 0) {
            for (SelectionKey selectionKey : selector.keys()) {
                setKey(selectionKey, ops);
            }
        }
    }

    public static void launchKeys(Iterator<SelectionKey> iterator, final boolean isForServer) throws IOException {
        while (iterator.hasNext()) {
            final SelectionKey key = iterator.next();
            iterator.remove();
            if (key.isValid()) {
                final DatagramChannel channel = (DatagramChannel) key.channel();
                if (isForServer) {
                    launchKeysForServer(key, channel);
                } else {
                    launchKeyForClient(key, channel);
                }
            }
        }
    }

    public static String buildMessage(String prefix, int thread, int request) {
        return prefix + thread + "_" + request;
    }

    public static String buildHelloMessage(String message) {
        return "Hello, " + message;
    }

    // :NOTE: Boolean method 'validateThread' is always inverted
    // :FIXED:
    public static boolean validateThread(int threads) {
        if (threads < 1) {
            System.err.println("Error: number of threads must be 1 or greater.");
            return true;
        }
        return false;
    }

    public static void shutdownAndAwaitTermination(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    System.err.println("Error: something gon wrong while shutdown executor service.");
                }
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    //===================================================================//

    private static void setKey(SelectionKey key, int ops) {
        key.interestOps(ops);
    }

    private static void launchKeyForClient(SelectionKey key, DatagramChannel channel) {
        HelloUDPNonblockingClient.Client  client = (HelloUDPNonblockingClient.Client) key.attachment();
        String requestMessage = buildMessage(client.prefix, client.thread, client.request);
        ByteBuffer buffer;
        try {
            buffer = ByteBuffer.allocate(channel.socket().getReceiveBufferSize());
        } catch (SocketException e) {
            buffer = ByteBuffer.allocate(buildHelloMessage(requestMessage).length());
        }

        if (key.isReadable()) {
            try {
                buffer.clear();
                final SocketAddress socketAddress = channel.receive(buffer);
                if (socketAddress != null) {
                    String responseMessage = new String(buffer.array(), UTF_8);
                    if (responseMessage.contains(requestMessage)) {
                        setKey(key, WRITE);
                        if (client.isAll()) {
                            channel.close();
                        }
                    } else {
                        System.err.println("Error: wrong message received.");
                    }
                } else {
                    System.err.println("Error: message was not received.");
                }
            } catch (IOException e) {
                System.err.println("Error: something gone wrong while receiving message." + e.getMessage());
            }
            if (channel.isOpen()) {
                setKey(key, WRITE);
            }
        } else if (key.isWritable()) {
            buffer = ByteBuffer.wrap(Objects.requireNonNull(requestMessage).getBytes(UTF_8));
            try {
                channel.write(buffer);
            } catch (IOException e) {
                System.err.println("Error: something gone wrong while sending message." + e.getMessage());
            }
            setKey(key, READ);
        }
    }

    private static void launchKeysForServer(SelectionKey key, DatagramChannel channel) throws IOException {
        HelloUDPNonblockingServer.Server server = (HelloUDPNonblockingServer.Server) key.attachment();
        final String message = server.message;
        ByteBuffer buffer = ByteBuffer.allocate(channel.socket().getReceiveBufferSize());

        if (key.isReadable()) {
            server.socketAddress = channel.receive(buffer);
            server.message = buildHelloMessage(new String(buffer.array(), UTF_8).trim());
            if (channel.isOpen()) {
                setKey(key, WRITE);
            }
        } else if (key.isWritable()) {
            buffer = ByteBuffer.wrap(Objects.requireNonNull(message).getBytes(UTF_8));
            channel.send(buffer, server.socketAddress);
            setKey(key, READ);
        }
    }
}
