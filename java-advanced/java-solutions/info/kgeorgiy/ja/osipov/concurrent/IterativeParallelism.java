package info.kgeorgiy.ja.osipov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class that optimize mapping function on list by parallel mapping
 *
 * @author Osipov Daniil
 */
public class IterativeParallelism implements AdvancedIP {

    /**
     * Token of ParallelMapper
     * {@link ParallelMapperImpl}
     * {@link ParallelMapper}
     */
    private final ParallelMapper parallelMapper;

    /**
     * Default constructor
     * {@code parallelMapper = null}
     */
    public IterativeParallelism() {
        parallelMapper = null;
    }

    /**
     * Constructor for setting {@code parallelMapper} from given instance
     *
     * @param parallelMapper given instance of {@code parallelMapper}
     */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }


    /**
     * Return split list values on given number of parts.
     *
     * @param numOfParts number of parts given {@link List} {@code values} to split
     * @param values     {@link List} to split
     * @param <T>        value type.
     * @return {@link List} split parts represented by {@link List}
     */
    private <T> List<List<T>> splitList(final int numOfParts, final List<T> values) {
        final int partsSize = values.size() / numOfParts;
        final List<List<T>> parts = new ArrayList<>();
        int remainder = values.size() % numOfParts;
        int left = 0;
        for (int i = 0; i < numOfParts; ++i) {
            final int right = left + partsSize + (i < remainder ? 1 : 0);
            parts.add(values.subList(left, right));
            left = right;
        }
        return parts;
    }

    /**
     * Split given {@link List} values on min {@code numOfThreads} and {@link List#size()}
     * Parts produced by {@code parallelMapper} if that non-null or
     * In each {@link Thread} apply given {@code function} to split parts
     * return reduced parts result by given {@code reduceFunction}
     *
     * @param numOfThreads   number of concurrent threads.
     * @param values         {@link List} values
     * @param function       {@link Function} applying to each part
     * @param reduceFunction {@link Function} to reduce parts
     * @param <T>            value type.
     * @param <R>            value type of {@code function} and {@code reduceFunction}
     * @return result of type {@code <R>} by splitting in parts
     * than applying {@code Function} than {@code reduceFunction}
     * @throws InterruptedException if one of executing thread was interrupted
     */
    private <T, R> R request(int numOfThreads,
                             final List<T> values,
                             final Function<Stream<T>, R> function,
                             final Function<Stream<R>, R> reduceFunction) throws InterruptedException {
        final int numOfParts = Math.min(numOfThreads, values.size());
        final List<List<T>> parts = splitList(numOfParts, values);
        if (Objects.isNull(parallelMapper)) {
            final List<Thread> threads = new ArrayList<>();
            final List<R> partResults = new ArrayList<>(Collections.nCopies(numOfParts, null));
            for (int i = 0; i < numOfParts; ++i) {
                final int index = i;
                Thread thread = new Thread(() -> partResults.set(index, function.apply(parts.get(index).stream())));
                thread.start();
                threads.add(thread);
            }
            for (final Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    for (final Thread th : threads) {
                        th.interrupt();
                        try {
                            th.join();
                        } catch (InterruptedException exception) {
                            e.addSuppressed(exception);
                        }
                    }
                    throw e;
                }
            }
            return reduceFunction.apply(partResults.stream());
        } else {
            List<R> partsResults = parallelMapper.map(list -> function.apply(list.stream()), parts);
            return reduceFunction.apply(partsResults.stream());
        }
    }

    /**
     * Join values to string.
     *
     * @param numOfThreads number of concurrent threads.
     * @param values       values to join.
     * @return list of joined results of {@link #toString()} call on each value.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public String join(final int numOfThreads, final List<?> values) throws InterruptedException {
        return request(numOfThreads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    /**
     * Filters values by predicate.
     *
     * @param numOfThreads number of concurrent threads.
     * @param values       values to filter.
     * @param predicate    filter predicate.
     * @return list of values satisfying given predicate. Order of values is preserved.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> List<T> filter(final int numOfThreads,
                              final List<? extends T> values,
                              final Predicate<? super T> predicate) throws InterruptedException {
        return request(numOfThreads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }

    /**
     * Maps values.
     *
     * @param numOfThreads number of concurrent threads.
     * @param values       values to map.
     * @param function     mapper function.
     * @return list of values mapped by given function.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, U> List<U> map(final int numOfThreads,
                              final List<? extends T> values,
                              final Function<? super T, ? extends U> function) throws InterruptedException {
        return request(numOfThreads, values,
                stream -> stream.map(function).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }

    /**
     * Returns maximum value.
     *
     * @param numOfThreads number of concurrent threads.
     * @param values       values to get maximum of.
     * @param comparator   value comparator.
     * @param <T>          value type.
     * @return maximum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if no values are given.
     */

    @Override
    public <T> T maximum(final int numOfThreads,
                         final List<? extends T> values,
                         final Comparator<? super T> comparator) throws InterruptedException {
        return request(numOfThreads, values,
                stream -> stream.max(comparator).orElseThrow(NoSuchElementException::new),
                stream -> stream.max(comparator).orElseThrow(NoSuchElementException::new));
    }

    /**
     * Returns minimum value.
     *
     * @param numOfThreads number of concurrent threads.
     * @param values       values to get minimum of.
     * @param comparator   value comparator.
     * @param <T>          value type.
     * @return minimum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if no values are given.
     */
    @Override
    public <T> T minimum(final int numOfThreads,
                         final List<? extends T> values,
                         final Comparator<? super T> comparator) throws InterruptedException {
        return maximum(numOfThreads, values, comparator.reversed());
    }

    /**
     * Returns whether all values satisfy predicate.
     *
     * @param numOfThreads number of concurrent threads.
     * @param values       values to test.
     * @param predicate    test predicate.
     * @param <T>          value type.
     * @return whether all values satisfy predicate or {@code true}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(final int numOfThreads,
                           final List<? extends T> values,
                           final Predicate<? super T> predicate) throws InterruptedException {
        return !any(numOfThreads, values, predicate.negate());
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param numOfThreads number of concurrent threads.
     * @param values       values to test.
     * @param predicate    test predicate.
     * @param <T>          value type.
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(final int numOfThreads,
                           final List<? extends T> values,
                           final Predicate<? super T> predicate) throws InterruptedException {
        return request(numOfThreads, values,
                stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(Boolean::booleanValue));
    }

    /**
     * Returns number of values satisfying predicate.
     *
     * @param numOfThreads number of concurrent threads.
     * @param values       values to test.
     * @param predicate    test predicate.
     * @param <T>          value type.
     * @return number of values satisfying predicate.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> int count(final int numOfThreads,
                         final List<? extends T> values,
                         final Predicate<? super T> predicate) throws InterruptedException {
        return request(numOfThreads, values,
                stream -> (int) stream.filter(predicate).count(),
                stream -> stream.mapToInt(Integer::intValue).sum());
    }


    /**
     * Generate reduce function for stream by {@code monoid}
     *
     * @param function stream map function
     * @param monoid   given monoid
     * @param <T>      Stream type parameter
     * @param <R>      Monoid type parameter
     * @return reduce function for stream by {@code Monoid}
     */
    private <T, R> Function<Stream<T>, R> genReduceFunc(final Function<T, R> function, final Monoid<R> monoid) {
        return stream -> stream.map(function).reduce(monoid.getOperator()).orElse(monoid.getIdentity());
    }

    /**
     * Reduces values using monoid.
     *
     * @param numOfThreads number of concurrent threads.
     * @param values       values to reduce.
     * @param monoid       monoid to use.
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity}, if no values specified.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> T reduce(final int numOfThreads,
                        final List<T> values,
                        final Monoid<T> monoid) throws InterruptedException {
        final Function<Stream<T>, T> reduceFunc = genReduceFunc(Function.identity(), monoid);
        return request(numOfThreads, values, reduceFunc, reduceFunc);
    }

    /**
     * Maps and reduces values using monoid.
     *
     * @param numOfThreads number of concurrent threads.
     * @param values       values to reduce.
     * @param lift         mapping function.
     * @param monoid       monoid to use.
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if no values specified.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, R> R mapReduce(final int numOfThreads,
                              final List<T> values,
                              final Function<T, R> lift,
                              final Monoid<R> monoid) throws InterruptedException {
        return request(numOfThreads, values, genReduceFunc(lift, monoid), genReduceFunc(Function.identity(), monoid));
    }
}
