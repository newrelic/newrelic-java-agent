package com.newrelic;

import com.newrelic.api.agent.Logger;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ConnectionStatusTest {
    @Test
    public void onlyOneThreadStartsConnecting() throws InterruptedException {
        final ConnectionStatus status = new ConnectionStatus(mock(Logger.class));
        final AtomicInteger numConnectingThreads = new AtomicInteger(0);
        final AtomicInteger numFailingThreads = new AtomicInteger(0);

        int numThreads = 10;
        List<Thread> threads = new ArrayList<>();
        final CyclicBarrier barrier = new CyclicBarrier(numThreads);

        while (threads.size() < numThreads) {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        barrier.await();
                        if (status.blockOnConnection() == ConnectionStatus.BlockResult.MUST_ATTEMPT_CONNECTION) {
                            numConnectingThreads.incrementAndGet();
                            Thread.sleep(100); // emulate time it takes to establish connection
                            status.didConnect();
                        }
                    } catch (Throwable t) {
                        numFailingThreads.incrementAndGet();
                    }
                }
            });
            thread.setName("Thread " + threads.size());
            thread.start();
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.join(2000);
            if (thread.isAlive()) {
                fail(thread.getName() + " is still alive");
            }
        }

        assertEquals(0, numFailingThreads.get());
        assertEquals(1, numConnectingThreads.get());
    }

    @Test
    public void onlyOneThreadBacksOff() throws InterruptedException {
        final ConnectionStatus status = new ConnectionStatus(mock(Logger.class));
        status.didConnect();

        final AtomicInteger numBackingOffThreads = new AtomicInteger(0);
        final AtomicInteger numFailingThreads = new AtomicInteger(0);

        int numThreads = 10;
        List<Thread> threads = new ArrayList<>();
        final CyclicBarrier barrier = new CyclicBarrier(numThreads);

        while (threads.size() < numThreads) {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        barrier.await();
                        if (status.shouldReconnect()) {
                            numBackingOffThreads.incrementAndGet();
                            Thread.sleep(100); // emulate backoff time
                            status.reattemptConnection();
                        }
                    } catch (Throwable t) {
                        numFailingThreads.incrementAndGet();
                    }
                }
            });
            thread.setName("Thread " + threads.size());
            thread.start();
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.join(2000);
            if (thread.isAlive()) {
                fail(thread.getName() + " is still alive");
            }
        }

        assertEquals(0, numFailingThreads.get());
        assertEquals(1, numBackingOffThreads.get());
    }

    @Test
    @Timeout(2)
    public void shouldReconnectIfDisconnected() throws InterruptedException {
        ConnectionStatus target = new ConnectionStatus(mock(Logger.class));
        assertEquals(ConnectionStatus.BlockResult.MUST_ATTEMPT_CONNECTION, target.blockOnConnection());
        target.didConnect();
        assertEquals(ConnectionStatus.BlockResult.ALREADY_CONNECTED, target.blockOnConnection());
        target.reattemptConnection();
        assertEquals(ConnectionStatus.BlockResult.MUST_ATTEMPT_CONNECTION, target.blockOnConnection());
    }

    @Test
    public void shouldGoAwayIfToldTo() throws InterruptedException {
        ConnectionStatus target = new ConnectionStatus(mock(Logger.class));
        assertEquals(ConnectionStatus.BlockResult.MUST_ATTEMPT_CONNECTION, target.blockOnConnection());
        target.didConnect();
        assertEquals(ConnectionStatus.BlockResult.ALREADY_CONNECTED, target.blockOnConnection());
        target.shutDownForever();
        assertEquals(ConnectionStatus.BlockResult.GO_AWAY_FOREVER, target.blockOnConnection());
    }

}