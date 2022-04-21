package info.kgeorgiy.ja.Maksonov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ParallelMapperImpl implements ParallelMapper {

    private final List<Thread> threads;
    private final Queue<Runnable> problems;

    /**
     * Creates ParallelMapper with {@code threads} threads.
     *
     * @see java.lang.Thread
     */
    public ParallelMapperImpl(int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Error: number of threads must be 1 or greater.");
        }
        // :NOTE: лишний this.
        this.threads = new ArrayList<>();
        problems = new ArrayDeque<>();
        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(
                    () -> {
                        while (!Thread.currentThread().isInterrupted()) {
                            Runnable problem;
                            synchronized (problems) {
                                while (problems.isEmpty()) {
                                    try {
                                        problems.wait();
                                    } catch (InterruptedException e) {
                                        return;
                                    }
                                }
                                problem = problems.poll();
                            }
                            problem.run();
                        }
                    }
            );
            this.threads.add(thread);
            thread.start();
        }
    }

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        final SmartCounter counter = new SmartCounter();
        final List<R> results = new ArrayList<>();
        // :NOTE: nCopy
        for (int i = 0; i < args.size(); i++) {
            results.add(null);
        }
        IntStream.range(0, args.size())
                .forEach(i -> addProblem(
                        () -> {
                            results.set(i, f.apply(args.get(i)));
                            // :NOTE: возможно было бы лучше сделать synchronized
                            synchronized (counter) {
                                counter.add();
                                if (counter.get() == args.size()) {
                                    counter.notify();
                                }
                            }
                        }
                ));
        synchronized (counter) {
            while (counter.get() < args.size()) {
                counter.wait();
            }
        }
        return results;
    }

    /**
     * Stops all threads. All unfinished mappings leave in undefined state.
     */
    @Override
    public void close() {
        for (Thread thread : threads) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException ignored) {
                // nothing to do
            }
        }
    }

    //===========================================================================//

    /**
     * Add {@code problem} which should be solved to {@link ParallelMapperImpl#problems}
     * @param problem task to solve
     */
    private void addProblem(Runnable problem) {
        synchronized (problems) {
            problems.add(problem);
            // :NOTE: notifyAll
            problems.notify();
        }
    }

    /**
     * Class to control threads.
     */
    private static class SmartCounter {
        private int col;

        SmartCounter() {
            col = 0;
        }

        public int get() {
            return col;
        }

        public void add() {
            col++;
        }
    }

}
