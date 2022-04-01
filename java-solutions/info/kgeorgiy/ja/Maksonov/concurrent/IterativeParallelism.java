package info.kgeorgiy.ja.Maksonov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;


import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;
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
        return applyFunction(threads, values, x -> x.max(comparator).orElseThrow(NoSuchElementException::new), x -> x.max(comparator).orElseThrow(NoSuchElementException::new));
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
        return applyFunction(threads, values, x -> x.allMatch(predicate), x -> x.allMatch(y -> y == true));
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
        return applyFunction(threads, values, x -> x.anyMatch(predicate), x -> x.anyMatch(y -> y == true));
    }

    //=========================================================================//

    /**
     * Applying {@code functionForValues} on {@code values} to get list of {@code <R>} values
     * and then applying {@code functionForResult} to get single value as answer.
     *
     * @param threads col of threads that can be used.
     * @param values list of values that should be analyzed.
     * @param functionForValues function for {@code values}.
     * @param functionForResult function to get answer after applying {@code functionForValues}.
     * @param <T> type of {@code values}.
     * @param <R> return type.
     * @return value of type <R> after applying {@code functionForValues} and {@code functionForResult}.
     * @throws InterruptedException when something gone wrong while working with threads.
     */
    private  <T, R> R applyFunction(final int threads, final List<? extends T> values, final Function<Stream<? extends T>, R> functionForValues, final Function<Stream<? extends R>, R> functionForResult) throws InterruptedException {
        int threadsCol = Math.min(threads, values.size());
        threadsCol = Math.max(1, threadsCol);
        final List<Thread> threadList = new ArrayList<>();
        final List<R> newValues = new ArrayList<>();
        for (int i = 0; i < threadsCol; i++) {
            newValues.add(null);
        }
        int left, right = 0;
        final int elementsPerThread = values.size() / threadsCol;
        final int extra = values.size() % threadsCol;
        final int[] threadsSizes = new int[threadsCol];
        for (int i = 0; i < threadsCol; i++) {
            threadsSizes[i] = elementsPerThread;
            if (extra > i) {
                threadsSizes[i]++;
            }
        }

        for (int i = 0; i < threadsCol; i++) {
            final int finalI = i;
            left = right;
            right = left + threadsSizes[i];
            final List<? extends T> subList = values.subList(left, right);

            final Thread thread = new Thread(
                    () -> newValues.set(finalI, functionForValues.apply(subList.stream()))
            );
            threadList.add(thread);
            thread.start();
        }

        for (final Thread thread : threadList) {
            try {
                thread.join();
            } catch (final InterruptedException e) {
                throw new InterruptedException("Error: error while working with threads: " + e.getMessage());
            }
        }
        return functionForResult.apply(newValues.stream());
    }
}

