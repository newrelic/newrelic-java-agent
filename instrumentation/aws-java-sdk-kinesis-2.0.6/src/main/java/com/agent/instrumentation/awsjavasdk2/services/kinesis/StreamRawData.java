package com.agent.instrumentation.awsjavasdk2.services.kinesis;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.CloudAccountInfo;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.regions.Region;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class StreamRawData {
    private final String streamName;
    private final String providedArn;
    private final WeakReference<Object> clientRef;
    private final WeakReference<SdkClientConfiguration> configRef;

    public StreamRawData(String streamName, String providedArn, Object client, SdkClientConfiguration config) {
        this.streamName = streamName;
        this.providedArn = providedArn;
        this.clientRef = new WeakReference<>(client);
        this.configRef = new WeakReference<>(config);
    }

    public String getStreamName() {
        return streamName;
    }

    public String getProvidedArn() {
        return providedArn;
    }

    public String getAccountId() {
        return AgentBridge.cloud.getAccountInfo(clientRef.get(), CloudAccountInfo.AWS_ACCOUNT_ID);
    }

    public String getRegion() {
        SdkClientConfiguration config = configRef.get();
        if (config == null) {
            return null;
        }
        Region option = config.option(AwsClientOption.AWS_REGION);
        if (option == null) {
            return null;
        }
        return option.toString();
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
                Objects.equals(clientRef, that.clientRef) &&
                // config uses Object.equals, so should be fast
                Objects.equals(configRef, that.configRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(streamName, providedArn, clientRef, configRef);
    }
}
