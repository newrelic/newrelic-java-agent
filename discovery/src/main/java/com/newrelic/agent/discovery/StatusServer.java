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
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ServerSocket server;
    private final Thread thread;
    private final AttachOutput attachOutput;

    private StatusServer(AttachOutput output, ServerSocket serverSocket) {
        this.attachOutput = output;
        this.server = serverSocket;
        this.thread = new Thread(new Runnable() {
            @Override
            public void run() {
                StatusServer.this.start();
            }
        });
    }

    public int getPort() {
        return server.getLocalPort();
    }

    public void start() {
        running.set(true);
        while (running.get()) {
            try (Socket socket = server.accept()) {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                final long uid = in.readLong();
                if (StatusMessage.serialVersionUID == uid) {
                    StatusMessage message = StatusMessage.readExternal(in);
                    attachOutput.write(message);
                }
            } catch (IOException e) {
                if (running.get()) {
                    attachOutput.error(e);
                }
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
        try {
            final StatusServer server = new StatusServer(output, new ServerSocket(0));
            server.startAsync();
            return server;
        } catch (IOException e) {
            output.error(e);
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
