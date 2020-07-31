package com.newrelic;

import com.newrelic.agent.interfaces.backport.Supplier;
import com.newrelic.api.agent.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ConnectionHeaders implements Supplier<Map<String, String>> {
    private final ConnectionStatus connectionStatus;
    private final Logger logger;
    private final String licenseKey;
    private volatile Map<String, String> headers;

    public ConnectionHeaders(ConnectionStatus connectionStatus, Logger logger, String licenseKey) {
        this.connectionStatus = connectionStatus;
        this.logger = logger;
        this.licenseKey = licenseKey;
    }

    public void set(String newRunToken, Map<String, String> headers) {
        Map<String, String> newHeaders = new HashMap<>(headers);
        newHeaders.put("agent_run_token", newRunToken);
        newHeaders.put("license_key", licenseKey);
        this.headers = newHeaders;

        logger.log(Level.INFO, "New Relic connection successful. Attempting connection to the Trace Observer.");
        connectionStatus.reattemptConnection();
    }

    @Override
    public Map<String, String> get() {
        return headers;
    }
}
