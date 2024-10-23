package com.agent.instrumentation.awsjavasdk2.services.kinesis;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.CloudParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.TracedMethod;

import java.util.function.Function;

public class KinesisUtil {

    public static final String PLATFORM = "aws_kinesis_data_streams";
    public static final String TRACE_CATEGORY = "Kinesis";

    private static final Function<StreamRawData, StreamProcessedData> CACHE =
            AgentBridge.collectionFactory.createAccessTimeBasedCache(3600, 8, KinesisUtil::processStreamData);
    private KinesisUtil() {}

    public static Segment beginSegment(String kinesisOperation, StreamRawData streamRawData) {
        String traceName = createTraceName(kinesisOperation, streamRawData);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(TRACE_CATEGORY, traceName);
        segment.reportAsExternal(createCloudParams(streamRawData));
        return segment;
    }

    public static void setTraceDetails(String kinesisOperation, StreamRawData streamRawData) {
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        String traceName = createTraceName(kinesisOperation, streamRawData);
        tracedMethod.setMetricName(TRACE_CATEGORY, traceName);
        tracedMethod.reportAsExternal(createCloudParams(streamRawData));
    }

    public static String createTraceName(String kinesisOperation, StreamRawData streamRawData) {
        String streamName = CACHE.apply(streamRawData).getStreamName();
        if (streamName != null && !streamName.isEmpty()) {
            return kinesisOperation + "/" + streamName;
        }
        return kinesisOperation;
    }
    public static CloudParameters createCloudParams(StreamRawData streamRawData) {
        return CloudParameters.provider(PLATFORM)
                .resourceId(CACHE.apply(streamRawData).getCloudResourceId())
                .build();
    }

    public static StreamProcessedData processStreamData(StreamRawData streamRawData) {
        String cloudResourceId = createCloudResourceId(streamRawData);
        String streamName = processStreamName(streamRawData, cloudResourceId);
        return new StreamProcessedData(streamName, cloudResourceId);
    }

    public static String processStreamName(StreamRawData streamRawData, String cloudResourceId) {
        if (streamRawData.getStreamName() != null && !streamRawData.getStreamName().isEmpty()) {
            return streamRawData.getStreamName();
        } else if (cloudResourceId != null) {
            // Stream names can be extracted from ARNs.
            int streamPrefixIdx = cloudResourceId.lastIndexOf("stream/");
            int streamNameIdx = streamPrefixIdx + 7;

            int consumerPrefixIdx = cloudResourceId.lastIndexOf("/consumer/");
            int endIdx = consumerPrefixIdx > streamNameIdx ? consumerPrefixIdx : cloudResourceId.length();

            if (0 <= streamPrefixIdx && streamPrefixIdx < (cloudResourceId.length() - 1)) {
                return cloudResourceId.substring(streamNameIdx, endIdx);
            }
        }
        return "";
    }

    public static String createCloudResourceId(StreamRawData streamRawData) {
        if (streamRawData.getProvidedArn() != null && !streamRawData.getProvidedArn().isEmpty()) {
            String cloudResourceId = streamRawData.getProvidedArn();
            // Check if the arn is a consumer ARN and extract the stream ARN from it.
            int consumerPrefixIdx = cloudResourceId.lastIndexOf("/consumer/");
            if (-1 < consumerPrefixIdx && consumerPrefixIdx < cloudResourceId.length()) {
                cloudResourceId = cloudResourceId.substring(0, consumerPrefixIdx);
            }
            // check if a partial arn without region
            if (cloudResourceId.startsWith("arn:aws:kinesis::")) {

                String region = streamRawData.getRegion();
                if (region == null || region.isEmpty()) {
                    return null;
                }

                cloudResourceId = "arn:aws:kinesis:" + region + cloudResourceId.substring(16);
            }
            return cloudResourceId;
        }

        String accountId = streamRawData.getAccountId();
        if (accountId == null || accountId.isEmpty()) {
            return null;
        }

        String streamName = streamRawData.getStreamName();
        if (streamName == null || streamName.isEmpty()) {
            return null;
        }
        String region = streamRawData.getRegion();
        if (region == null || region.isEmpty()) {
            return null;
        }

        return "arn:aws:kinesis:" + region + ':' + accountId + ":stream/" + streamRawData.getStreamName();
    }

}
