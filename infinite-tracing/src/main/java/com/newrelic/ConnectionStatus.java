package com.newrelic;

import com.newrelic.api.agent.Logger;
import io.grpc.Status;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class ConnectionStatus {
    public ConnectionStatus(Logger logger) {
        this.logger = logger;
    }

    public enum BlockResult {ALREADY_CONNECTED, MUST_ATTEMPT_CONNECTION, GO_AWAY_FOREVER}

    private enum ConnectionState {CONNECT_NEEDED, CONNECTING, CONNECTED, BACKOFF_PAUSE, STOPPED_FOREVER}

    private final Logger logger;
    private final AtomicReference<ConnectionState> currentState = new AtomicReference<>(ConnectionState.CONNECT_NEEDED);

    /**
     * Blocks until a connection is either made, or this thread is responsible for making the connection.
     *
     * @return {@link BlockResult#MUST_ATTEMPT_CONNECTION} if this thread is responsible for connecting; {@link BlockResult#ALREADY_CONNECTED} if already connected.
     */
    public BlockResult blockOnConnection() throws InterruptedException {
        while (true) {
            ConnectionState current = currentState.get();
            if (current == ConnectionState.CONNECTED) {
                return BlockResult.ALREADY_CONNECTED;
            } else if (current == ConnectionState.STOPPED_FOREVER) {
                logger.log(Level.FINE, "No longer attempting to reconnect to gRPC");
                return BlockResult.GO_AWAY_FOREVER;
            } else if (currentState.compareAndSet(ConnectionState.CONNECT_NEEDED, ConnectionState.CONNECTING)) {
                return BlockResult.MUST_ATTEMPT_CONNECTION;
            }

            Thread.sleep(1000);
        }
    }

    /**
     * Threads that successfully connect should call this when complete. It will release other threads busy-waiting for a connection.
     */
    public void didConnect() {
        currentState.set(ConnectionState.CONNECTED);
    }

    /**
     * Tells all threads that all connections should be stopped and not resumed.
     */
    public void shutDownForever() {
        currentState.set(ConnectionState.STOPPED_FOREVER);
    }

    /**
     * Indicates whether or not this thread should follow the disconnect/backoff routine
     */
    public boolean shouldReconnect() {
        return currentState.compareAndSet(ConnectionState.CONNECTED, ConnectionState.BACKOFF_PAUSE);
    }

    /**
     * Tells all threads that the next thread should attempt to reconnect.
     */
    public void reattemptConnection() {
        currentState.set(ConnectionState.CONNECT_NEEDED);
    }
}
