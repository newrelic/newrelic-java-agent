package com.agent.instrumentation.awsjavasdk218.services.kinesis;

public class StreamProcessedData {
    private final String streamName;
    private final String arn;

    public StreamProcessedData(String streamName, String arn) {
        this.streamName = streamName;
        this.arn = arn;
    }

    public String getStreamName() {
        return streamName;
    }

    public String getArn() {
        return arn;
    }
}
