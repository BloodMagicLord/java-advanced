package info.kgeorgiy.ja.Maksonov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.*;
import java.util.List;
import java.util.ArrayList;

import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {
    private static final int TIMEOUT = 100;
    private static final int SIZE = 1024;
    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final String expectedArgs = "Expected <host> <port> <prefix> <threads> <requests>.";

    /**
     * Runs HelloClient with given {@code args}.
     * <p>
     * @param args arguments for running from terminal.
     */
    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("Error: invalid arguments. " + expectedArgs);
            return;
        }

        try {
            HelloClient client = new HelloUDPClient();

            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String prefix = args[2];
            int threads = Integer.parseInt(args[3]);
            int requests = Integer.parseInt(args[4]);

            client.run(host, port, prefix, threads, requests);
        } catch (NumberFormatException e) {
            System.out.println("Error: cannot convert arguments. " + expectedArgs);
        }
    }

    /**
     * Sending requests to {@code host} with given {@code prefix}.
     * <p>
     * @param host host
     * @param port port
     * @param prefix prefix for requests
     * @param threads number of working threads
     * @param requests number of requests
     */
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        if (threads <= 0) {
            System.err.println("Error: number of threads must be 1 or greater.");
            return;
        }

        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            List<Thread> threadList = new ArrayList<>();

            IntStream
                    .range(0, threads)
                    .forEach(i -> {
                        final Thread thread = new Thread(() -> {
                            try (DatagramSocket socket = new DatagramSocket()) {
                                socket.setSoTimeout(TIMEOUT);
                                IntStream
                                        .range(0, requests)
                                        .forEach(j -> {
                                            String requestMessage = prefix + i + "_" + j;
                                            String responseMessage = "";
                                            String expectedResponseMessage = "Hello, " + requestMessage;
                                            byte[] bytes = requestMessage.getBytes(UTF_8);
                                            DatagramPacket request = new DatagramPacket(bytes, bytes.length, inetAddress, port);

                                            while (!responseMessage.equals(expectedResponseMessage)) {
                                                try {
                                                    socket.send(request);
                                                    DatagramPacket response = new DatagramPacket(new byte[SIZE], SIZE);
                                                    socket.receive(response);
                                                    responseMessage = new String(response.getData(), response.getOffset(), response.getLength());
                                                } catch (SocketTimeoutException e) {
                                                    System.err.println("Error: socket timed out. " + e);
                                                } catch (IOException e) {
                                                    System.err.println("Error: something gone wrong while sending. " + e);
                                                }
                                            }

                                            System.out.println(requestMessage + " " + responseMessage);
                                        });
                            } catch (SocketException e) {
                                System.err.println("Error: cannot open socket. " + e);
                            }
                        });
                        threadList.add(thread);
                        thread.start();
                    });

            for (final Thread thread : threadList) {
                try {
                    thread.join();
                } catch (final InterruptedException ignored) {
                    // nothing to do
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Error: cannot find host by name. " + e);
        }
    }
}
