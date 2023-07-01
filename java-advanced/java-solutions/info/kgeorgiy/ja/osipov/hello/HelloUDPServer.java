package info.kgeorgiy.ja.osipov.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer extends AbstractHelloUPDServer {

    private DatagramSocket socket;

    /**
     * Starts a new Hello server.
     * This method should return immediately.
     *
     * @param port    server port.
     * @param threads number of working threads.
     */
    @Override
    public void start(final int port, final int threads) {
        if (!UDPUtils.checkPortAndThreads(port, threads)) {
            return;
        }
        try {
            socket = new DatagramSocket(port);
        } catch (final SocketException e) {
            System.err.println("socket could not be opened, or the socket could not be bound");
            return;
        } catch (final SecurityException e) {
            System.err.println("security manager exists and its checkListen method doesn't allow the operation");
            return;
        }
        service = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; ++i) {
            service.submit(request());
        }
    }

    private Runnable request() {
        return () -> {
            try {
                final int bufferSize = socket.getReceiveBufferSize();
                final DatagramPacket receivePacket = new DatagramPacket(new byte[bufferSize], bufferSize);
                final DatagramPacket sendPacket = new DatagramPacket(new byte[bufferSize], bufferSize);
                while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    socket.receive(receivePacket);
                    sendPacket.setSocketAddress(receivePacket.getSocketAddress());
                    final String received = UDPUtils.getReceivedMessage(receivePacket);
                    //System.out.println("Hello, " + received);
                    sendPacket.setData(("Hello, " + received).getBytes(StandardCharsets.UTF_8));
                    socket.send(sendPacket);
                }
            } catch (SocketException e) {
                System.err.println("an error in the underlying protocol " + e.getMessage());
            } catch (IOException ignored) {

            }
        };
    }

    /**
     * Stops server and deallocates all resources.
     */
    @Override
    public void close() {
        socket.close();
        service.close();
    }

    /**
     * Run {@link HelloUDPServer#start(int, int)} with given arguments
     *
     * @param args array with exactly 2 non-null {@link String}, which represent:
     *             1) port (must be {@link java.text.NumberFormat})
     *             2) Number of threads (must be {@link java.text.NumberFormat})
     */
    public static void main(String[] args) {
        try (HelloUDPServer server = new HelloUDPServer()) {
            server.runServer(args);
        }
    }
}
