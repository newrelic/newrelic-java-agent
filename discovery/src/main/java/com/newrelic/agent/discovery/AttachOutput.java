package com.newrelic.agent.discovery;

/**
 * We support plain text and json output.
 */
interface AttachOutput extends StatusMessageWriter {
    void attachStarted(String pid, String command, String agentArgs);
    void attachFinished();
    /**
     * Called to finalize the output when the attach operation is finished.
     */
    void finished();
    @Override
    void write(StatusMessage message);
    void error(Exception e);
    void warn(String message);
    void listHeader();
    void list(String id, String displayName, String vmVersion, boolean isAttachable);
}
