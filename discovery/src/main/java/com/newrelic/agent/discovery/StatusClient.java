package com.newrelic.agent.discovery;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Allows the agent in the target process to communicate status to the attach process.
 */
public class StatusClient implements StatusMessageWriter {

    private final int port;
    private final InetAddress address;

    private StatusClient(int port) throws UnknownHostException {
        this.port = port;
        this.address = InetAddress.getLocalHost();
    }

    public static StatusClient create(int port) throws UnknownHostException {
        return new StatusClient(port);
    }

    @Override
    public void write(StatusMessage message) throws IOException {
        write(StatusMessage.serialVersionUID, message);
    }

    private void write(long uid, Externalizable externalizable) throws IOException {
        try (Socket socket = new Socket(address, port)) {
            socket.setSoLinger(true, 1);
            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                out.writeLong(uid);
                externalizable.writeExternal(out);
            }
        }
    }
}
