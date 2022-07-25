package com.newrelic.agent.discovery;

import java.io.IOException;

public interface StatusMessageWriter {
    void write(StatusMessage message) throws IOException;
}
