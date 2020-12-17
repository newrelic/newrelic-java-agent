package com.newrelic;

import com.newrelic.api.agent.Logger;
import io.grpc.Status;

import java.util.logging.Level;

public class DisconnectionHandler {
    private final ConnectionStatus connectionStatus;
    private final BackoffPolicy defaultBackoffPolicy;
    private final ConnectBackoffPolicy connectBackoffPolicy;
    private final Logger logger;

    public DisconnectionHandler(ConnectionStatus connectionStatus, BackoffPolicy defaultBackoffPolicy,
                                ConnectBackoffPolicy connectBackoffPolicy, Logger logger) {
        this.connectionStatus = connectionStatus;
        this.defaultBackoffPolicy = defaultBackoffPolicy;
        this.connectBackoffPolicy = connectBackoffPolicy;
        this.logger = logger;
    }

    public void terminate() {
        connectionStatus.shutDownForever();
    }

    public void handle(Status responseStatus) {
        if (!defaultBackoffPolicy.shouldReconnect(responseStatus)) {
            if (responseStatus != null) {
                logger.log(Level.WARNING, "Got gRPC status {0}, no longer permitting connections.", responseStatus.getCode().toString());
            }
            terminate();
        }

        if (isFailedPrecondition(responseStatus)) {
            backingOff(connectBackoffPolicy);
            connectionStatus.reattemptConnection();
        }

        if (!connectionStatus.shouldReconnect()) {
            return;
        }

        backingOff(defaultBackoffPolicy);
        connectionStatus.reattemptConnection();
    }

    void backingOff(BackoffPolicy backoffPolicy) {
        logger.log(Level.FINE, "Backing off with {0} for {1} seconds", backoffPolicy.getClass().getSimpleName(), backoffPolicy.duration());
        backoffPolicy.backoff();
        logger.log(Level.FINE, "Backoff complete, attempting connection.");
    }

    boolean isFailedPrecondition(Status responseStatus) {
        if(responseStatus != null) {
            return responseStatus.getCode() == Status.Code.FAILED_PRECONDITION;
        }
        return false;
    }

    public void resetConnectBackoffPolicy() {
        if(connectBackoffPolicy.duration() > 0) {
            connectBackoffPolicy.reset();
        }
    }
}