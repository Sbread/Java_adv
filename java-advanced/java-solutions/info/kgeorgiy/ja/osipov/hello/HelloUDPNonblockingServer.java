package info.kgeorgiy.ja.osipov.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Executors;

public class HelloUDPNonblockingServer extends AbstractHelloUPDServer {

    private Selector selector;
    private DatagramChannel channel;

    private final BlockingQueue<ClientInfo> clients = new BlockingQueue<>();

    private int bufSize;


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
            selector = Selector.open();
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            channel.bind(new InetSocketAddress(port));
            service = Executors.newFixedThreadPool(threads);
            bufSize = channel.socket().getReceiveBufferSize();
            Executors.newSingleThreadExecutor().submit(this::executeServer);
        } catch (IOException e) {
            System.err.println("Exception while starting server. " + e.getMessage());
        }
    }

    private void executeServer() {
        while (selector.isOpen() && !Thread.interrupted() && !channel.socket().isClosed()) {
            try {
                selector.select();
                for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext();) {
                    final SelectionKey key = i.next();
                    try {
                        if (key.isReadable()) {
                            receive(key);
                        }
                        if (key.isWritable()) {
                            send(key);
                        }
                    } finally {
                        i.remove();
                    }
                }
            } catch (IOException e) {
                System.err.println("selector exception. " + e.getMessage());
            }
        }
    }

    private void send(final SelectionKey key) {
        if (!clients.isEmpty()) {
            try {
                final ClientInfo clientInfo = clients.poll();
                final ByteBuffer byteBuffer = ByteBuffer.wrap(clientInfo.sendMessage.getBytes(StandardCharsets.UTF_8));
                channel.send(byteBuffer, clientInfo.address);
            } catch (InterruptedException e) {
                System.err.println("Thread was interrupted. " + e.getMessage());
            } catch (IOException e) {
                System.err.println("IO exception raised while response was sending. " + e.getMessage());
            }
            key.interestOpsOr(SelectionKey.OP_READ);
        } else {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void receive(final SelectionKey key) {
        try {
            final ByteBuffer byteBuffer = ByteBuffer.allocate(bufSize);
            final SocketAddress address = channel.receive(byteBuffer);
            service.submit(response(byteBuffer, address, key));
        } catch (IOException e) {
            System.err.println("IO exception raised while receiving message. " + e.getMessage());
        }
    }

    private Runnable response(final ByteBuffer buffer, final SocketAddress address, final SelectionKey key) {
        return () -> {
            buffer.flip();
            final String received = StandardCharsets.UTF_8.decode(buffer).toString();
            final String sendMessage = "Hello, " + received;
            key.interestOps(SelectionKey.OP_WRITE);
            clients.add(new ClientInfo(sendMessage, address));
            selector.wakeup();
        };
    }

    /**
     * Thread-safe queue
     *
     * @param <T> type of queuing elements
     */
    private static class BlockingQueue<T> {

        /**
         * {@link Queue} of elements
         */
        private final Queue<T> queue;

        /**
         * Default constructor initialization queue as {@link ArrayDeque}
         */
        public BlockingQueue() {
            queue = new ArrayDeque<>();
        }

        public synchronized boolean isEmpty() {
            return queue.isEmpty();
        }

        /**
         * Thread-safe add element and then notify
         *
         * @param element - element to add
         */
        public synchronized void add(final T element) {
            queue.add(element);
            notify();
        }

        /**
         * Thread-safe poll first element.
         * Wait for queue is not empty, then poll element
         *
         * @return first element of the queue
         * @throws InterruptedException if thread was interrupted
         */
        public synchronized T poll() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();
            }
            return queue.poll();
        }
    }

    private record ClientInfo(String sendMessage, SocketAddress address) {
    }

    /**
     * Stops server and deallocates all resources.
     */
    @Override
    public void close() {
        try {
            if (Objects.nonNull(selector)) {
                selector.close();
            }
            if (Objects.nonNull(channel)) {
                channel.close();
            }
            if (Objects.nonNull(service)) {
                service.close();
            }
        } catch (IOException e) {
            System.err.println("Exception while resource closing. " + e.getMessage());
        }
    }
}
