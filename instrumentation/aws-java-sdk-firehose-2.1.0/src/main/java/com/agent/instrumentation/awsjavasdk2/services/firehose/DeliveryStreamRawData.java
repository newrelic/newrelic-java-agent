package com.agent.instrumentation.awsjavasdk2.services.firehose;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.CloudAccountInfo;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.regions.Region;

import java.util.Objects;

public class DeliveryStreamRawData {
    private final String streamName;
    private final String accountId;
    private final String region;

    public DeliveryStreamRawData(String streamName, Object client, SdkClientConfiguration config) {
        this.streamName = streamName;
        this.accountId = AgentBridge.cloud.getAccountInfo(client, CloudAccountInfo.AWS_ACCOUNT_ID);
        this.region = getRegionFromConfig(config);
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
        if (!(o instanceof DeliveryStreamRawData)) {
            return false;
        }
        DeliveryStreamRawData that = (DeliveryStreamRawData) o;
        return Objects.equals(streamName, that.streamName) &&
                Objects.equals(region, that.region) &&
                Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(streamName, region, accountId);
    }

    private static String getRegionFromConfig(SdkClientConfiguration config) {
        if (config == null) {
            return null;
        }
        Region option = config.option(AwsClientOption.AWS_REGION);
        if (option == null) {
            return null;
        }
        return option.toString();
    }
}
