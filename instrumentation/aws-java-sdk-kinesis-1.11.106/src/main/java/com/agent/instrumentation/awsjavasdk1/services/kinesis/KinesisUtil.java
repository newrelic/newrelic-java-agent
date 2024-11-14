package com.agent.instrumentation.awsjavasdk1.services.kinesis;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.util.AwsHostNameUtils;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.CloudParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.TracedMethod;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;

public class KinesisUtil {

    public static final String PLATFORM = "aws_kinesis_data_streams";
    public static final String TRACE_CATEGORY = "Kinesis";

    public static final Map<AmazonWebServiceRequest, Token> requestTokenMap = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();

    private static final Function<StreamRawData, String> ARN_CACHE =
            AgentBridge.collectionFactory.createAccessTimeBasedCache(3600, 8, KinesisUtil::createArn);
    private KinesisUtil() {}

    public static String getRegion(String serviceName, URI endpoint, String overrideRegion) {
        String parsedRegion = AwsHostNameUtils.parseRegion(endpoint.getHost(), serviceName);
        if (parsedRegion != null && !parsedRegion.isEmpty()) {
            return parsedRegion;
        }
        return overrideRegion;
    }

    public static void setTokenForRequest(AmazonWebServiceRequest request) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            if (request != null) {
                Token token = NewRelic.getAgent().getTransaction().getToken();
                requestTokenMap.put(request, token);
            }
        }
    }

    public static void setTraceInformation(String kinesisOperation, AmazonWebServiceRequest request, StreamRawData streamRawData) {
        Token token = KinesisUtil.getToken(request);
        if (token != null) {
            token.linkAndExpire();
            KinesisUtil.cleanToken(request);
        }
        TracedMethod tracedMethod = NewRelic.getAgent().getTransaction().getTracedMethod();
        KinesisUtil.setTraceDetails(kinesisOperation, tracedMethod, streamRawData);
    }

    public static Token getToken(AmazonWebServiceRequest request) {
        if (request != null) {
            return requestTokenMap.get(request);
        }
        return null;
    }

    public static void cleanToken(AmazonWebServiceRequest request) {
        if (request != null) {
            requestTokenMap.remove(request);
        }
    }

    public static void setTraceDetails(String kinesisOperation, TracedMethod tracedMethod, StreamRawData streamRawData) {
        String traceName = createTraceName(kinesisOperation, streamRawData);
        tracedMethod.setMetricName(TRACE_CATEGORY, traceName);
        tracedMethod.reportAsExternal(createCloudParams(streamRawData));
    }

    public static String createTraceName(String kinesisOperation, StreamRawData streamRawData) {
        String streamName = streamRawData.getStreamName();
        if (streamName != null && !streamName.isEmpty()) {
            return kinesisOperation + "/" + streamName;
        }
        return kinesisOperation;
    }

    public static CloudParameters createCloudParams(StreamRawData streamRawData) {
        return CloudParameters.provider(PLATFORM)
                .resourceId(ARN_CACHE.apply(streamRawData))
                .build();
    }

    public static String createArn(StreamRawData streamRawData) {
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

        return "arn:aws:kinesis:" + region + ':' + accountId + ":stream/" + streamName;
    }

}
