package info.kgeorgiy.ja.osipov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

/**
 * Class for parallel producing tasks
 *
 * @author Osipov Daniil
 */
public class ParallelMapperImpl implements ParallelMapper {

    /**
     * {@link List} {@link Thread} to produce tasks.
     */
    private final List<Thread> threads;

    /**
     * Thread-safe queue {@link BlockingQueue} for task to run
     */
    private final BlockingQueue<Runnable> tasks;

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

    /**
     * Construct {@link ParallelMapperImpl} with given number of threads
     * <p>
     * Initialize {@code threads} as {@link ArrayList}
     * and {@code tasks} as {@link BlockingQueue}
     * add in {@code threads} {@code numberOfThreads} threads
     * which waiting for tasks and running them while not interrupted
     * when start them
     *
     * @param numberOfThreads - number of threads for doing tasks
     */
    public ParallelMapperImpl(final int numberOfThreads) {
        this.threads = new ArrayList<>();
        this.tasks = new BlockingQueue<>();
        Runnable threadSettings = () -> {
            try {
                while (!Thread.interrupted()) {
                    final Runnable task = tasks.poll();
                    task.run();
                }
            } catch (InterruptedException ignored) {

            } finally {
                Thread.currentThread().interrupt();
            }
        };
        for (int i = 0; i < numberOfThreads; ++i) {
            threads.add(new Thread(threadSettings));
            threads.get(i).start();
        }
    }

    /**
     * Class with result of mapping function
     *
     * @param <T> type of result
     */
    private static class ListOfMappingResults<T> {

        /**
         * {@link List} contains results
         */
        private final List<T> list;

        /**
         * number of non-null elements in {@code list}
         */
        private int contains;

        /**
         * Construct {@link ListOfMappingResults} with given size
         * by initialize {@code list} this {@code size} null elements
         *
         * @param size - size of results list
         */
        private ListOfMappingResults(final int size) {
            list = new ArrayList<>(Collections.nCopies(size, null));
            contains = 0;
        }

        /**
         * Thread-safe setting element on index. After setting element notify
         *
         * @param index - index on that element will be set
         * @param res   - element that will be set
         */
        private synchronized void set(final int index, final T res) {
            list.set(index, res);
            contains++;
            if (contains == list.size()) {
                notify();
            }
        }

        /**
         * Thread-safe getting full-filled {@code list} of results method
         * <p>
         * Waiting for list will be full-filled, then return
         *
         * @return Full-filled {@code list} of results
         * @throws InterruptedException if thread was interrupted
         */
        private synchronized List<T> getList() throws InterruptedException {
            while (contains != list.size()) {
                wait();
            }
            return list;
        }
    }

    /**
     * Maps function {@code function} over specified {@code list}.
     * Mapping for each element performed in parallel.
     *
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> function,
                              final List<? extends T> list) throws InterruptedException {
        final ListOfMappingResults<R> results = new ListOfMappingResults<>(list.size());
        for (int i = 0; i < list.size(); ++i) {
            final int finalI = i;
            tasks.add(() -> results.set(finalI, function.apply(list.get(finalI))));
        }
        return results.getList();
    }

    /**
     * Stop single thread
     *
     * @param thread - thread to stop
     */
    private void stopThread(final Thread thread) {
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException ignored) {

        }
    }

    /**
     * Stops all threads. All unfinished mappings are left in undefined state.
     */
    @Override
    public void close() {
        threads.forEach(this::stopThread);
    }
}
