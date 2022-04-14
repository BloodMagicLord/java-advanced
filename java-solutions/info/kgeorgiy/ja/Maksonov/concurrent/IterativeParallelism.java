package info.kgeorgiy.ja.Maksonov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;


import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IterativeParallelism implements ScalarIP {

    /**
     * Returns maximum value.
     *
     * @param threads number or concurrent threads.
     * @param values values to get maximum of.
     * @param comparator value comparator.
     * @param <T> value type.
     *
     * @return maximum of given values
     *
     * @throws InterruptedException if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if no values are given.
     */
    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        // :NOTE: Похоже на minimum
        // :FIXED:
        return minimum(threads, values, Collections.reverseOrder(comparator));
    }

    /**
     * Returns minimum value.
     *
     * @param threads number or concurrent threads.
     * @param values values to get minimum of.
     * @param comparator value comparator.
     * @param <T> value type.
     *
     * @return minimum of given values
     *
     * @throws InterruptedException if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if no values are given.
     */
    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return applyFunction(threads, values, x -> x.min(comparator).orElseThrow(NoSuchElementException::new), x -> x.min(comparator).orElseThrow(NoSuchElementException::new));
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threads number or concurrent threads.
     * @param values values to test.
     * @param predicate test predicate.
     * @param <T> value type.
     *
     * @return whether all values satisfies predicate or {@code true}, if no values are given
     *
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        // :NOTE: Похоже на any
        // :FIXED:
        return !any(threads, values, predicate.negate());
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads number or concurrent threads.
     * @param values values to test.
     * @param predicate test predicate.
     * @param <T> value type.
     *
     * @return whether any value satisfies predicate or {@code false}, if no values are given
     *
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return Boolean.TRUE.equals(applyFunction(threads, values, x -> x.anyMatch(predicate), x -> x.anyMatch(y -> y == true)));
    }

    //=========================================================================//

    /**
     * Applying {@code functionForValues} on {@code values} to get list of {@code <R>} values
     * and then applying {@code functionForResult} to get single value as answer.
     * <p>
     * Applying {@code functionForValues} on {@code values} to get list of {@code <R>} values
     * and then applying {@code functionForResult} to get single value as answer.
     *
     * If {@code value} is empty, returns null.
     *
     * @param threads col of threads that can be used, must be 0 or greater
     * @param values list of values that should be analyzed.
     * @param functionForValues function for {@code values}.
     * @param functionForResult function to get answer after applying {@code functionForValues}.
     * @param <T> type of {@code values}.
     * @param <R> return type.
     * @return value of type <R> after applying {@code functionForValues} and {@code functionForResult}.
     * @throws IllegalArgumentException when {@code threads} is less then 0.
     * @throws InterruptedException when something gone wrong while working with threads.
     */
    private  <T, R> R applyFunction(final int threads, final List<? extends T> values, final Function<Stream<? extends T>, R> functionForValues, final Function<Stream<? extends R>, R> functionForResult) throws InterruptedException {

        // :NOTE: Лишний поток для пустого списка
        // :FIXED:
        if (threads < 0) {
            throw new IllegalArgumentException("Error: number of threads must be 0 or greater.");
        }
        if (values.size() == 0) {
            return null;
        }
        int threadsCol = Math.min(threads, values.size());
        if (threadsCol > 0) {
            final List<Thread> threadList = new ArrayList<>();
            final List<R> newValues = new ArrayList<>();
            for (int i = 0; i < threadsCol; i++) {
                newValues.add(null);
            }
            final int elementsPerThread = values.size() / threadsCol;
            final int extra = values.size() % threadsCol;

            // :NOTE: Упростить
            // :FIXED:
            //:NOTE: IntStream
            // :FIXED:
            IntStream
                    .range(0, threadsCol)
                    .forEach(i -> {
                        final List<? extends T> subList = values.subList(
                                i * elementsPerThread + Math.min(i, extra)
                                , (i + 1) * elementsPerThread + Math.min(i + 1, extra)
                        );
                        final Thread thread = new Thread(
                                () -> newValues.set(i, functionForValues.apply(subList.stream()))
                        );
                        threadList.add(thread);
                        thread.start();
                    });

            Exception exception = null;
            for (final Thread thread : threadList) {
                try {
                    thread.join();
                } catch (final InterruptedException e) {
                    // :NOTE: Бесполезно
                    // :FIXED:
                    if (exception == null) {
                        exception = e;
                    } else {
                        exception.addSuppressed(e);
                    }
                }
            }

            if (exception != null) {
                throw new InterruptedException("Error: error while working with threads. " + exception);
            }
            return functionForResult.apply(newValues.stream());
        }
        final List<R> newValues = new ArrayList<>();
        newValues.add(functionForValues.apply(values.stream()));
        return functionForResult.apply(newValues.stream());
    }
}

