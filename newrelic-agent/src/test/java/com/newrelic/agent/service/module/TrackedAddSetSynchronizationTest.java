package com.newrelic.agent.service.module;

import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class TrackedAddSetSynchronizationTest {
    ExecutorService executorService = Executors.newFixedThreadPool(10, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    });

    @After
    public void after() {
        executorService.shutdownNow();
    }

    /*
     * If this test ever fails, there is a design/runtime flaw in the TrackedAddSet.
     * Do not pass this off as a test flicker.
     */
    @Test
    public void verifySynchronization() throws InterruptedException {
        Random r = new Random();
        CountDownLatch latch = new CountDownLatch(1);
        TrackedAddSet<String> target = new TrackedAddSet<>(1_000_000);
        List<String> expectedValues = Collections.synchronizedList(new LinkedList<String>());
        List<Set<String>> deltas = Collections.synchronizedList(new LinkedList<Set<String>>());
        AtomicInteger value = new AtomicInteger(1234);
        int resetSubmitCount = 0;
        int acceptSubmitCount = 0;

        // Stuff the executor queue. Note that the tasks will block on the latch.
        // The latch is in place to ensure that a single thread can't complete its work
        // before the next item is queued.
        for (int i = 0; i < 500_000; i++) {
            if (r.nextFloat() < 0.05) {
                // capture the items added since the last reset.
                resetSubmitCount += 1;
                executorService.submit(new ResettingRunnable(latch, deltas, target));
            } else {
                // add a new item to the tracked set.
                acceptSubmitCount += 1;
                executorService.submit(new AddingRunnable(latch, value, expectedValues, target));
            }
        }

        // release the initial tasks
        latch.countDown();
        // we're not queueing anything else, and we want to make sure everything is complete.
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // ensure a reset is the last thing we do.
        resetSubmitCount += 1;
        deltas.add(target.resetReturningAdded());

        // conditions we test:
        //  - the number of accept and reset operations matches what we expected
        //  - the individual deltas are disjoint from each other
        //  - the full set is a union of the individual deltas.

        Set<String> fullSet = target.resetReturningAll();

        assertEquals("test has an issue: not all runnables completed", resetSubmitCount, deltas.size());
        assertEquals("test has an issue: not all runnables completed", acceptSubmitCount, expectedValues.size());
        assertEquals("full set lost values somewhere", acceptSubmitCount, fullSet.size());

        int sumOfDeltaSizes = 0;
        Set<String> allDeltas = new HashSet<>();
        for (Set<String> delta : deltas) {
            sumOfDeltaSizes += delta.size();
            allDeltas.addAll(delta);
        }

        // since `allDeltas` is a Set, any values in more than one delta will result in a smaller
        // `allDeltas.size()` than `sumOfDeltaSizes`.
        assertEquals("the same value was in different sets", allDeltas.size(), sumOfDeltaSizes);

        StringBuilder builder = new StringBuilder("Missing expected values:");

        for (String expected : expectedValues) {
            if (!allDeltas.contains(expected)) {
                builder.append(" ").append(expected);
            }
        }

        assertEquals(builder.toString(), allDeltas, fullSet);
    }

    private static class ResettingRunnable implements Runnable {
        private final CountDownLatch latch;
        private final List<Set<String>> deltas;
        private final TrackedAddSet<String> target;

        public ResettingRunnable(CountDownLatch latch, List<Set<String>> deltas, TrackedAddSet<String> target) {
            this.latch = latch;
            this.deltas = deltas;
            this.target = target;
        }

        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }

            deltas.add(target.resetReturningAdded());
        }
    }

    private static class AddingRunnable implements Runnable {
        private final CountDownLatch latch;
        private final AtomicInteger value;
        private final List<String> expectedValues;
        private final TrackedAddSet<String> target;

        public AddingRunnable(CountDownLatch latch, AtomicInteger value, List<String> expectedValues, TrackedAddSet<String> target) {
            this.latch = latch;
            this.value = value;
            this.expectedValues = expectedValues;
            this.target = target;
        }

        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }

            String toAdd = String.valueOf(value.incrementAndGet());
            expectedValues.add(toAdd);
            target.accept(toAdd);
        }
    }
}
