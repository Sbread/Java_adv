package info.kgeorgiy.ja.osipov.hello;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;

public class HelloUDPClient extends AbstractHelloUDPClient {

    /**
     * Runs Hello client.
     * This method should return when all requests are completed.
     *
     * @param host     server host
     * @param port     server port
     * @param prefix   request prefix
     * @param threads  number of request threads
     * @param requests number of requests per thread.
     */
    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final SocketAddress address = getSocketAddress(host, port, threads, requests);
        if (address == null) {
            return;
        }
        ExecutorService service = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; ++i) {
            service.submit(request(i, address, prefix, requests));
        }
        service.close();
    }

    private Runnable request(final int threadId, final SocketAddress address, final String prefix, final int requests) {
        return () -> {
            try (final DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(UDPUtils.DATAGRAM_SOCKET_TIMEOUT);
                final int bufferSize = socket.getReceiveBufferSize();
                DatagramPacket requestPacket = new DatagramPacket(new byte[bufferSize], bufferSize, address);
                DatagramPacket responsePacket = new DatagramPacket(new byte[bufferSize], bufferSize);
                for (int i = 0; i < requests; ++i) {
                    sendAndReceiveMessage(socket, prefix, threadId + 1,
                            i + 1, requestPacket, responsePacket);
                }
            } catch (final SocketException e) {
                System.err.println("socket could not be opened, or the socket could not be bound" + e.getMessage());
            } catch (final SecurityException e) {
                System.err.println("security manager exists and its checkListen method doesn't allow the operation"
                        + e.getMessage());
            }
        };
    }

    private void sendAndReceiveMessage(final DatagramSocket socket,
                                       final String prefix,
                                       final int threadId,
                                       final int requestId,
                                       final DatagramPacket requestPacket,
                                       final DatagramPacket responsePacket) {
        final String requestMessage = UDPUtils.makeRequestMessage(prefix, threadId, requestId);
        while (!socket.isClosed()) {
            System.out.println("Sending: " + requestMessage);
            requestPacket.setData(requestMessage.getBytes(StandardCharsets.UTF_8));
            try {
                socket.send(requestPacket);
            } catch (IOException e) {
                continue;
            }
            try {
                socket.receive(responsePacket);
            } catch (IOException e) {
                continue;
            }
            String receivedMessage = UDPUtils.getReceivedMessage(responsePacket);
            if (UDPUtils.checkReceivedMessage(receivedMessage, threadId, requestId)) {
            //if (checkResponse(receivedMessage, threadId, requestId)) {
                System.out.println("Received: " + receivedMessage);
                break;
            }
        }
        if (socket.isClosed()) {
            System.err.println("request failed due to closed socket");
        }
    }



    /**
     * Run {@link HelloUDPClient#run(String, int, String, int, int)} with given args
     *
     * @param args - array with exactly 5 non-null {@link String}, which represent:
     *             1) host
     *             2) port (must be {@link java.text.NumberFormat})
     *             3) prefix of requested message
     *             4) Number of threads (must be {@link java.text.NumberFormat})
     *             5) Number of requests (must be {@link java.text.NumberFormat})
     */

    public static void main(final String[] args) {
        new HelloUDPClient().runClient(args);
    }
}
