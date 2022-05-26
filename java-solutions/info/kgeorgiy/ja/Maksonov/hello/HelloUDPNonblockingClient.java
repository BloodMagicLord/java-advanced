package info.kgeorgiy.ja.Maksonov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;

import static info.kgeorgiy.ja.Maksonov.hello.GeneralCode.*;

public class HelloUDPNonblockingClient implements HelloClient {

    public static void main(String[] args) {
        try {
            final HelloUDPNonblockingClient client = new HelloUDPNonblockingClient();
            launchClient(client, args);
        } catch (NumberFormatException e) {
            System.out.println("Error: cannot convert arguments. " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error: invalid arguments. " + e.getMessage());
        }
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        if (!validateThread(threads)) {
            return;
        }
        try {
            final InetAddress inetAddress = InetAddress.getByName(host);
            final SocketAddress address = new InetSocketAddress(inetAddress, port);
            try (final Selector selector = Selector.open()) {
                for (int thread = 0; thread < threads; thread++) {
                    final DatagramChannel channel = openChannel();
                    channel.connect(address);
                    channel.register(selector, WRITE, new Client(prefix, thread, requests));
                }

                while (!selector.keys().isEmpty()) {
                    setKeys(selector, WRITE);
                    launchKeys(selector.selectedKeys().iterator(), false);
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Error: cannot find host by name. " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error: cannot open selector. " + e.getMessage());
        }
    }

    //===================================================================//

    public static class Client {
        public final String prefix;
        public final int colRequests;
        public int thread;
        public int request;

        public Client(String prefix, int thread, int requests) {
            this.prefix = prefix;
            this.thread = thread;
            this.request = 0;
            this.colRequests = requests;
        }

        public boolean isAll() {
            this.request++;
            return request == colRequests;
        }
    }
}
