package com.agent.instrumentation.awsjavasdk12.services.kinesis;

public class StreamProcessedData {
    private final String streamName;
    private final String cloudResourceId;

    public StreamProcessedData(String streamName, String cloudResourceId) {
        this.streamName = streamName;
        this.cloudResourceId = cloudResourceId;
    }

    public String getStreamName() {
        return streamName;
    }

    public String getCloudResourceId() {
        return cloudResourceId;
    }
}
