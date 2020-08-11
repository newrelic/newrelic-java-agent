package com.newrelic.agent.discovery;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Allows the attach process to receive feedback from target process agents.
 */
public class StatusServer implements Closeable {
    private static final int INITIAL_PORT = 8509;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ServerSocket server;
    private final int port;
    private final Thread thread;
    private final AttachOutput attachOutput;

    private StatusServer(AttachOutput output, int port, ServerSocket serverSocket) {
        this.attachOutput = output;
        this.server = serverSocket;
        this.port = port;
        this.thread = new Thread(new Runnable() {
            @Override
            public void run() {
                StatusServer.this.start();
            }
        });
    }

    public int getPort() {
        return port;
    }

    public void start() {
        running.set(true);
        while (running.get()) {
            try (Socket socket = server.accept()) {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                StatusMessage message = new StatusMessage();
                message.readExternal(in);
                attachOutput.write(message);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void startAsync() {
        thread.start();
    }

    /**
     * Returns a StatusServer or null if none could be created.
     */
    public static StatusServer createAndStart(AttachOutput output) {
        for (int i = 0; i < 500; i++) {
            final int port = INITIAL_PORT + i;
            try {
                final StatusServer server = new StatusServer(output, port, new ServerSocket(port));
                server.startAsync();
                return server;
            } catch (IOException e) {
                output.warn("Skipping port " + port);
            }
        }
        return null;
    }

    @Override
    public void close() {
        try {
            thread.join(TimeUnit.SECONDS.toMillis(1), 0);
        } catch (InterruptedException e) {
            // ignore
        }
        running.set(false);
        try {
            server.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public void flush() {
        try {
            thread.join(100, 0);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}
