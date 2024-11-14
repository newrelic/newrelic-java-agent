package com.agent.instrumentation.awsjavasdk218.services.kinesis;

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
                .resourceId(CACHE.apply(streamRawData).getArn())
                .build();
    }

    public static StreamProcessedData processStreamData(StreamRawData streamRawData) {
        String arn = createArn(streamRawData);
        String streamName = processStreamName(streamRawData, arn);
        return new StreamProcessedData(streamName, arn);
    }

    public static String processStreamName(StreamRawData streamRawData, String arn) {
        if (streamRawData.getStreamName() != null && !streamRawData.getStreamName().isEmpty()) {
            return streamRawData.getStreamName();
        } else if (arn != null) {
            // Stream names can be extracted from ARNs.
            int streamPrefixIdx = arn.lastIndexOf("stream/");
            int streamNameIdx = streamPrefixIdx + 7;

            int consumerPrefixIdx = arn.lastIndexOf("/consumer/");
            int endIdx = consumerPrefixIdx > streamNameIdx ? consumerPrefixIdx : arn.length();

            if (0 <= streamPrefixIdx && streamPrefixIdx < (arn.length() - 1)) {
                return arn.substring(streamNameIdx, endIdx);
            }
        }
        return "";
    }

    public static String createArn(StreamRawData streamRawData) {
        if (streamRawData.getProvidedArn() != null && !streamRawData.getProvidedArn().isEmpty()) {
            String arn = streamRawData.getProvidedArn();
            // Check if the arn is a consumer ARN and extract the stream ARN from it.
            int consumerPrefixIdx = arn.lastIndexOf("/consumer/");
            if (-1 < consumerPrefixIdx && consumerPrefixIdx < arn.length()) {
                arn = arn.substring(0, consumerPrefixIdx);
            }
            // check if a partial arn without region
            if (arn.startsWith("arn:aws:kinesis::")) {

                String region = streamRawData.getRegion();
                if (region == null || region.isEmpty()) {
                    return null;
                }

                arn = "arn:aws:kinesis:" + region + arn.substring(16);
            }
            return arn;
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
