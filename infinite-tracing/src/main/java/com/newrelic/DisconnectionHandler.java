package com.newrelic;

import com.newrelic.api.agent.Logger;
import io.grpc.Status;

import java.util.logging.Level;

public class DisconnectionHandler {
    private final ConnectionStatus connectionStatus;
    private final BackoffPolicy backoffPolicy;
    private final Logger logger;

    public DisconnectionHandler(ConnectionStatus connectionStatus, BackoffPolicy backoffPolicy, Logger logger) {
        this.connectionStatus = connectionStatus;
        this.backoffPolicy = backoffPolicy;
        this.logger = logger;
    }

    public void terminate() {
        connectionStatus.shutDownForever();
    }

    public void handle(Status responseStatus) {
        if (!connectionStatus.shouldReconnect()) {
            return;
        }

        if (!backoffPolicy.shouldReconnect(responseStatus)) {
            if (responseStatus != null) {
                logger.log(Level.WARNING, "Got gRPC status " + responseStatus.getCode().toString() + ", no longer permitting connections.");
            }
            terminate();
        }

        logger.log(Level.FINE, "Backing off due to gRPC errors.");
        backoffPolicy.backoff();
        logger.log(Level.FINE, "Backoff complete, attempting connection.");
        connectionStatus.reattemptConnection();
    }
}
