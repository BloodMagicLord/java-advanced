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
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return applyFunction(threads, values, x -> x.max(comparator).orElseThrow(NoSuchElementException::new), x -> x.max(comparator).orElseThrow(NoSuchElementException::new));
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return applyFunction(threads, values, x -> x.min(comparator).orElseThrow(NoSuchElementException::new), x -> x.min(comparator).orElseThrow(NoSuchElementException::new));
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return applyFunction(threads, values, x -> x.allMatch(predicate), x -> x.allMatch(y -> y == true));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return applyFunction(threads, values, x -> x.anyMatch(predicate), x -> x.anyMatch(y -> y == true));
    }

    //=========================================================================//

    private  <T, R> R applyFunction(int threads, List<? extends T> values, Function<Stream<? extends T>, R> functionForValues, Function<Stream<? extends R>, R> functionForResult) throws InterruptedException {
        int threadsCol = Math.min(threads, values.size());
        List<Thread> threadList = new ArrayList<>();
        List<R> newValues = new ArrayList<>();
        for (int i = 0; i < threadsCol; i++) {
            newValues.add(null);
        }
        int left, right = 0;
        int elementsPerThread = values.size() / threadsCol;
        int extra = values.size() % threadsCol;
        int[] threadsSizes = new int[threadsCol];
        for (int i = 0; i < threadsCol; i++) {
            threadsSizes[i] = elementsPerThread;
            if (extra > i) {
                threadsSizes[i]++;
            }
        }

        for (int i = 0; i < threadsCol; i++) {
            int finalI = i;
            left = right;
            right = left + threadsSizes[i];
            List<? extends T> subList = values.subList(left, right);

            Thread thread = new Thread(
                    () -> newValues.set(finalI, functionForValues.apply(subList.stream()))
            );
            threadList.add(thread);
            thread.start();
        }

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new InterruptedException("Error: error while working with threads: " + e.getMessage());
            }
        }
        return functionForResult.apply(newValues.stream());
    }
}
