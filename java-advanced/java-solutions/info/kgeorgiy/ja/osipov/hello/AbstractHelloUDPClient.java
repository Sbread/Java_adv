package info.kgeorgiy.ja.osipov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;

public abstract class AbstractHelloUDPClient implements HelloClient {
    protected void runClient(final String[] args) {
        if (Objects.isNull(args) || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Expected 5 non-null arguments");
        }
        try {
            final String host = args[0];
            final int port = Integer.parseInt(args[1]);
            final String prefix = args[2];
            final int threads = Integer.parseInt(args[3]);
            final int request = Integer.parseInt(args[4]);
            run(host, port, prefix, threads, request);
        } catch (NumberFormatException e) {
            System.err.println("Invalid arguments " + e.getMessage());
        }
    }

    protected SocketAddress getSocketAddress(final String host, final int port, final int threads, final int requests) {
        if (!UDPUtils.checkPortThreadsRequests(port, threads, requests)) {
            return null;
        }
        SocketAddress address;
        try {
            address = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + host + " " + e.getMessage());
            return null;
        } catch (SecurityException e) {
            System.err.println("Security manager is present and permission to resolve the host name:"
                    + host + " is denied " + e.getMessage());
            return null;
        }
        return address;
    }
}
