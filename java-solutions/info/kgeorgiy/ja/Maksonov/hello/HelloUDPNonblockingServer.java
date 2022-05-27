package info.kgeorgiy.ja.Maksonov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static info.kgeorgiy.ja.Maksonov.hello.GeneralCode.*;

public class HelloUDPNonblockingServer implements HelloServer {
    private static ExecutorService executorService;
    private static Selector selector;
    private static DatagramChannel datagramChannel;

    //===================================================================//

    public static void main(String[] args) {
        try (HelloServer server = new HelloUDPNonblockingServer()) {
            launchServer(server, args);
        } catch (NumberFormatException e) {
            System.out.println("Error: cannot convert arguments. " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error: invalid arguments. " + e.getMessage());
        }
    }

    @Override
    public void start(int port, int threads) {
        if (!validateThread(threads)) {
            return;
        }

        try {
            final InetSocketAddress socketAddress = new InetSocketAddress(port);

            selector = Selector.open();

            datagramChannel = openChannel();
            datagramChannel.bind(socketAddress);
            datagramChannel.register(selector, READ, new Server());

            executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                try {
                    while (datagramChannel.isOpen()) {
                        setKeys(selector, READ);
                        launchKeys(selector.selectedKeys().iterator(), true);
                    }
                } catch (IOException ignored) {
                    // nothing to do
                }
            });
        } catch (UnknownHostException e) {
            System.err.println("Error: cannot find host by name. " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error: something gone wrong while starting the server." + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            selector.close();
        } catch (IOException e) {
            System.err.println("Error: cannot close selector." + e.getMessage());
        }

        try {
            datagramChannel.close();
        } catch (IOException e) {
            System.err.println("Error: cannot close channel." + e.getMessage());
        }

        try {
            executorService.shutdown();
            executorService.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS);
            // :NOTE: а если не завершимся за такой таймаут?
        } catch (InterruptedException ignored) {
            // nothing to do
        }
    }

    //===================================================================//

    public static class Server {
        public String message;
        public SocketAddress socketAddress;

        public Server() {
            message = null;
            socketAddress = null;
        }
    }
}