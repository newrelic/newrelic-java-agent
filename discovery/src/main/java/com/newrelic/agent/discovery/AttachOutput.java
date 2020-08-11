package com.newrelic.agent.discovery;

interface AttachOutput extends StatusMessageWriter {
    void attachStarted(String pid, String command, String agentArgs);
    void attachFinished();
    void finished();
    @Override
    void write(StatusMessage message);
    void error(Exception e);
    void warn(String message);
    
}
