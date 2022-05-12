package info.kgeorgiy.ja.Maksonov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import java.io.IOException;

import java.net.*;
import java.nio.charset.*;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.IntStream;

public class HelloUDPServer implements HelloServer {
    private static final int TIMEOUT = 100;
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    private static final String expectedArgs = "Expected <port> <threads>.";
    private static boolean isStarted = false;
    private static DatagramSocket socket;
    private static List<Thread> threadList;

    /**
     * Runs HelloServer with given {@code args}.
     * <p>
     * @param args arguments for running from terminal.
     */
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Error: invalid arguments. " + expectedArgs);
            return;
        }

        try {
            try (HelloServer server = new HelloUDPServer()) {

                int port = Integer.parseInt(args[0]);
                int threads = Integer.parseInt(args[1]);

                server.start(port, threads);
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: cannot convert arguments. " + expectedArgs);
        }
    }

    /**
     * Starts HelloServer for receiving requests.
     * <p>
     * @param port port.
     * @param threads number of working threads.
     */
    @Override
    public void start(int port, int threads) {
        if (isStarted) {
            System.err.println("Error: server is already started.");
            return;
        }

        try {
            socket = new DatagramSocket(port);
            threadList = new ArrayList<>();
            isStarted = true;
            socket.setSoTimeout(TIMEOUT);

            IntStream.range(0, threads).forEach(i -> {
                final Thread thread = new Thread(() -> {
                    try {
                        while (!socket.isClosed()) {
                            final int size = socket.getReceiveBufferSize();
                            DatagramPacket request = new DatagramPacket(new byte[size], size);
                            socket.receive(request);

                            String responseMessage = "Hello, " + new String(request.getData(), request.getOffset(), request.getLength());
                            byte[] bytes = responseMessage.getBytes(UTF_8);
                            DatagramPacket response = new DatagramPacket(bytes, bytes.length, request.getAddress(), request.getPort());

                            socket.send(response);
                        }
                    } catch (SocketTimeoutException e) {
                        System.err.println("Error: socket timed out. " + e);
                    } catch (IOException e) {
                        System.err.println("Error: something gone wrong while sending. " + e);
                    }
                });

                threadList.add(thread);
                thread.start();
            });
        } catch (SocketException e) {
            System.err.println("Error: cannot open socket. " + e);
        }
    }

    /**
     * Shutdown HelloServer.
     */
    @Override
    public void close() {
        socket.close();
        threadList.forEach(Thread::interrupt);
        isStarted = false;
    }
}
