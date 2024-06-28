package com.nr.instrumentation.r2dbc.postgresql092;

public class EndpointData {
    private final int port;
    private final String hostName;

    public EndpointData(String hostName, int port) {
        this.port = port;
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public String getHostName() {
        return hostName;
    }
}
