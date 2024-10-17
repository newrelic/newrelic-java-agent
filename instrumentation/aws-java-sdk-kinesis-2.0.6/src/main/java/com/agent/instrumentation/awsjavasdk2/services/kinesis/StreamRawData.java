package com.agent.instrumentation.awsjavasdk2.services.kinesis;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.CloudAccountInfo;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;

import java.util.Objects;

public class StreamRawData {
    private final String streamName;
    private final String providedArn;
    private final Object client;
    private final SdkClientConfiguration config;

    public StreamRawData(String streamName, String providedArn, Object client, SdkClientConfiguration config) {
        this.streamName = streamName;
        this.providedArn = providedArn;
        this.client = client;
        this.config = config;
    }

    public String getStreamName() {
        return streamName;
    }

    public String getProvidedArn() {
        return providedArn;
    }

    public String getAccountId() {
        return AgentBridge.cloud.getAccountInfo(client, CloudAccountInfo.AWS_ACCOUNT_ID);
    }

    public String getRegion() {
        return config.option(AwsClientOption.AWS_REGION).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StreamRawData)) {
            return false;
        }
        StreamRawData that = (StreamRawData) o;
        return Objects.equals(streamName, that.streamName) &&
                Objects.equals(providedArn, that.providedArn) &&
                Objects.equals(client, that.client) &&
                // config uses Object.equals, so should be fast
                Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(streamName, providedArn, client, config);
    }
}
