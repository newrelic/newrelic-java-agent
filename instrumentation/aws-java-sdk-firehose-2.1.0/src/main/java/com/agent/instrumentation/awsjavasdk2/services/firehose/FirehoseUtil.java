package com.agent.instrumentation.awsjavasdk2.services.firehose;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.CloudParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.TracedMethod;

import java.util.function.Function;

public class FirehoseUtil {

    public static final String PLATFORM = "aws_kinesis_delivery_streams";
    public static final String TRACE_CATEGORY = "Firehose";

    private static final Function<DeliveryStreamRawData, String> ARN_CACHE =
            AgentBridge.collectionFactory.createAccessTimeBasedCache(3600, 8, FirehoseUtil::createArn);
    private FirehoseUtil() {}

    public static Segment beginSegment(String firehoseOperation, DeliveryStreamRawData streamRawData) {
        String traceName = createTraceName(firehoseOperation, streamRawData);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(TRACE_CATEGORY, traceName);
        segment.reportAsExternal(createCloudParams(streamRawData));
        return segment;
    }

    public static void setTraceDetails(String firehoseOperation, DeliveryStreamRawData streamRawData) {
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        String traceName = createTraceName(firehoseOperation, streamRawData);
        tracedMethod.setMetricName(TRACE_CATEGORY, traceName);
        tracedMethod.reportAsExternal(createCloudParams(streamRawData));
    }

    public static String createTraceName(String firehoseOperation, DeliveryStreamRawData streamRawData) {
        String streamName = streamRawData.getStreamName();
        if (streamName != null && !streamName.isEmpty()) {
            return firehoseOperation + "/" + streamName;
        }
        return firehoseOperation;
    }
    public static CloudParameters createCloudParams(DeliveryStreamRawData streamRawData) {
        return CloudParameters.provider(PLATFORM)
                .resourceId(ARN_CACHE.apply(streamRawData))
                .build();
    }

    public static String createArn(DeliveryStreamRawData streamRawData) {
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

        return "arn:aws:firehose:" + region + ':' + accountId + ":deliverystream/" + streamRawData.getStreamName();
    }

}
