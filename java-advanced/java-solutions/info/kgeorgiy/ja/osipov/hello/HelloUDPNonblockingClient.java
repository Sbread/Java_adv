package info.kgeorgiy.ja.osipov.hello;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class HelloUDPNonblockingClient extends AbstractHelloUDPClient {
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
        final List<DatagramChannel> channels = new ArrayList<>();
        try (final Selector selector = Selector.open()) {
            ByteBuffer[] byteBuffers = new ByteBuffer[threads];
            int[] countRequests = new int[threads];
            for (int i = 1; i <= threads; ++i) {
                final DatagramChannel channel = DatagramChannel.open();
                channels.add(channel);
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_WRITE, i);
                byteBuffers[i - 1] = ByteBuffer.allocate(channel.socket().getReceiveBufferSize());
            }
            int remainingThreads = threads;
            while (remainingThreads > 0 && !Thread.interrupted()) {
                final int selected = selector.select(UDPUtils.SELECTOR_TIMEOUT);
                if (selected == 0) {
                    final Set<SelectionKey> keys = selector.keys();
                    for (final SelectionKey selectionKey : keys) {
                        selectionKey.interestOps(SelectionKey.OP_WRITE);
                    }
                    continue;
                }
                for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                    final SelectionKey key = i.next();
                    final DatagramChannel channel = (DatagramChannel) key.channel();
                    final int threadId = (int) key.attachment();
                    final int requestId = countRequests[threadId - 1] + 1;
                    try {
                        if (key.isWritable()) {
                            sendMessage(prefix, threadId, requestId, address, channel, key);
                        } else {
                            remainingThreads = receiveMessage(threadId, requestId, channel, key,
                                    byteBuffers[threadId - 1], countRequests, requests, remainingThreads);
                        }
                    } finally {
                        i.remove();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Selector IOException occurred " + e.getMessage());
        } finally {
            channels.forEach(c -> {
                try {
                    c.close();
                } catch (IOException e) {
                    System.err.println("While closing channel IOException occurred " + e.getMessage());
                }
            });
        }

    }

    private void sendMessage(final String prefix, final int threadId, final int requestId, final SocketAddress address,
                             final DatagramChannel channel, final SelectionKey key) throws IOException {
        final String message = UDPUtils.makeRequestMessage(prefix, threadId, requestId);
        channel.send(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)), address);
        System.out.println("Sending: " + message);
        key.interestOps(SelectionKey.OP_READ);
    }

    private int receiveMessage(final int threadId, final int requestId,
                               final DatagramChannel channel, final SelectionKey key,
                               final ByteBuffer buffer, final int[] countRequests, final int requests,
                               int remainingThreads) throws IOException {
        buffer.clear();
        channel.receive(buffer);
        buffer.flip();
        key.interestOps(SelectionKey.OP_WRITE);
        final String receivedMessage = StandardCharsets.UTF_8.decode(buffer).toString();
        if (UDPUtils.checkReceivedMessage(receivedMessage, threadId, requestId)) {
            System.out.println("Received: " + receivedMessage);
            if (++countRequests[threadId - 1] == requests) {
                remainingThreads--;
                channel.close();
            }
        }
        return remainingThreads;
    }

    public static void main(final String[] args) {
        new HelloUDPNonblockingClient().runClient(args);
    }
}
