package com.agent.instrumentation.awsjavasdk12.services.kinesis;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.CloudAccountInfo;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class StreamRawData {
    private final String streamName;
    private final String accountId;
    private final String region;

    public StreamRawData(String streamName, Object client, String region) {
        this.streamName = streamName;
        this.accountId = AgentBridge.cloud.getAccountInfo(client, CloudAccountInfo.AWS_ACCOUNT_ID);
        this.region = region;
    }

    public String getStreamName() {
        return streamName;
    }
    public String getAccountId() {
        return accountId;
    }

    public String getRegion() {
        return region;
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
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(region, that.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(streamName, accountId, region);
    }
}