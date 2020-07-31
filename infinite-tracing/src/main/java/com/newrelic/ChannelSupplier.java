package com.newrelic;

import com.newrelic.agent.interfaces.backport.Supplier;
import com.newrelic.api.agent.Logger;
import io.grpc.ManagedChannel;

import java.util.logging.Level;

/**
 * Provides a {@link ManagedChannel}, recreating it only if necessary.
 *
 * Not thread-safe.
 */
public class ChannelSupplier implements Supplier<ManagedChannel> {
    private final ConnectionStatus connectionStatus;
    private final Logger logger;
    private final ChannelFactory channelFactory;
    private volatile ManagedChannel channel;

    public ChannelSupplier(ChannelFactory channelFactory, ConnectionStatus connectionStatus, Logger logger) {
        this.connectionStatus = connectionStatus;
        this.logger = logger;
        this.channelFactory = channelFactory;
        channel = null;
    }

    @Override
    public ManagedChannel get() {
        ConnectionStatus.BlockResult blockResult = getBlockResult();

        if (blockResult == ConnectionStatus.BlockResult.GO_AWAY_FOREVER) {
            throw new RuntimeException("No longer attempting to connect.");
        }

        if (blockResult == ConnectionStatus.BlockResult.MUST_ATTEMPT_CONNECTION || channel == null) {
            logger.log(Level.FINE, "Attempting to connect to the Trace Observer.");

            if (channel != null) {
                ManagedChannel oldChannel = channel;
                channel = null;
                oldChannel.shutdown();
            }
            channel = channelFactory.createChannel();
            connectionStatus.didConnect();
        }

        return channel;
    }

    public ConnectionStatus.BlockResult getBlockResult() {
        ConnectionStatus.BlockResult blockResult;
        try {
            blockResult = connectionStatus.blockOnConnection();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while attempting to connect.");
        }
        return blockResult;
    }

}
