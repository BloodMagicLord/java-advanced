package info.kgeorgiy.ja.Maksonov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static info.kgeorgiy.ja.Maksonov.hello.GeneralCode.*;

public class HelloUDPClient implements HelloClient {
    // :NOTE: не стоит использовать константный размер
    // :FIXED:
    /**
     * Runs HelloClient with given {@code args}.
     * <p>
     * @param args arguments for running from terminal.
     */
    public static void main(String[] args) {
        try {
            HelloClient client = new HelloUDPClient();
            launchClient(client, args);
        } catch (NumberFormatException e) {
            System.out.println("Error: cannot convert arguments. " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error: invalid arguments. " + e.getMessage());
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
        if (validateThread(threads)) {
            return;
        }

        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            // :NOTE: Thread
            // :FIXED:
            List<Thread> threadList = new ArrayList<>();
            IntStream.range(0, threads).forEach(i -> {
                final Runnable runnable = () -> {
                    try (DatagramSocket socket = new DatagramSocket()) {
                        socket.setSoTimeout(TIMEOUT);
                        IntStream.range(0, requests).forEach(j -> {
                            final String requestMessage = buildMessage(prefix, i, j);
                            sendAndReceive(socket, port, inetAddress, requestMessage);
                        });
                    } catch (SocketException e) {
                        System.err.println("Error: cannot open socket. " + e);
                    }
                };
                final Thread thread = new Thread(runnable);
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

    //========================================================================//

    /**
     * Sends request and gets response using {@code socket}.
     *
     * @param socket socket
     * @param port port
     * @param inetAddress internet address
     * @param requestMessage message for request
     */
    private static void sendAndReceive(DatagramSocket socket, int port, InetAddress inetAddress, String requestMessage) {
        final int size;
        int temp;
        try {
            temp = socket.getReceiveBufferSize();
        } catch (SocketException e) {
            temp = ("Hello, " + requestMessage).length();
        }
        size = temp;
        String responseMessage = "";
        byte[] bytes = requestMessage.getBytes(UTF_8);
        DatagramPacket request = new DatagramPacket(bytes, bytes.length, inetAddress, port);

        while (!responseMessage.contains(requestMessage)) {
            try {
                socket.send(request);
                DatagramPacket response = new DatagramPacket(new byte[size], size);
                socket.receive(response);
                responseMessage = new String(response.getData(), response.getOffset(), response.getLength());
            } catch (SocketTimeoutException e) {
                System.err.println("Error: socket timed out. " + e);
            } catch (IOException e) {
                System.err.println("Error: something gone wrong while sending. " + e);
            }
        }

        System.out.println(requestMessage + " " + responseMessage);
    }
}