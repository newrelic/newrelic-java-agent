package com.agent.instrumentation.awsjavasdk2.services.kinesis;

import com.amazonaws.AmazonWebServiceRequest;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.CloudParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.TracedMethod;

import java.util.Map;

public class KinesisUtil {

    public static final String PLATFORM = "aws_kinesis_data_streams";
    public static final String TRACE_CATEGORY = "Kinesis";

    public static final Map<AmazonWebServiceRequest, Token> requestTokenMap = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
    private KinesisUtil() {}

    public static void setTokenForRequest(AmazonWebServiceRequest request) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            if (request != null) {
                requestTokenMap.put(request, NewRelic.getAgent().getTransaction().getToken());
            }
        }
    }

    public static void linkAndExpireToken(AmazonWebServiceRequest request) {
        if (request != null) {
            Token token = requestTokenMap.get(request);
            if (token != null) {
                token.linkAndExpire();
            }
            requestTokenMap.remove(request);
        }
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
