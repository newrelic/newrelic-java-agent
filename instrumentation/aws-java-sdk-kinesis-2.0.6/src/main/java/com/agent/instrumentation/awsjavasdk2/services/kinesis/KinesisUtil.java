package com.agent.instrumentation.awsjavasdk2.services.kinesis;

import com.newrelic.api.agent.CloudParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.TracedMethod;

public class KinesisUtil {

    public static final String PLATFORM = "aws_kinesis_data_streams";
    public static final String TRACE_CATEGORY = "Kinesis";
    private KinesisUtil() {}

    public static Segment beginSegment(String kinesisOperation) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(TRACE_CATEGORY, kinesisOperation);
        segment.reportAsExternal(createCloudParams());
        return segment;
    }

    public static void setTraceDetails(String kinesisOperation) {
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        tracedMethod.setMetricName(TRACE_CATEGORY, kinesisOperation);
        tracedMethod.reportAsExternal(createCloudParams());
    }
    public static CloudParameters createCloudParams() {
        // Todo: add arn to cloud parameters
        return CloudParameters.provider(PLATFORM).build();
    }

}
