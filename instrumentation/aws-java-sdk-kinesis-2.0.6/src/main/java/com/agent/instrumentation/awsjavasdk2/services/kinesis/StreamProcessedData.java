package com.agent.instrumentation.awsjavasdk2.services.kinesis;

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

    public String getArn() {
        return cloudResourceId;
    }
}
