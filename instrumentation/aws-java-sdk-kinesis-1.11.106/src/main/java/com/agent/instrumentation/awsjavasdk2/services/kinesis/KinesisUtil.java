package com.agent.instrumentation.awsjavasdk2.services.kinesis;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.handlers.AsyncHandler_Instrumentation;
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
                Token token = NewRelic.getAgent().getTransaction().getToken();
                requestTokenMap.put(request, token);
            }
        }
    }

    public static void setTraceInformation(String kinesisOperation, AmazonWebServiceRequest request) {
        Token token = KinesisUtil.getToken(request);
        if (token != null) {
            token.linkAndExpire();
        }
        KinesisUtil.cleanToken(request);
        TracedMethod tracedMethod = NewRelic.getAgent().getTransaction().getTracedMethod();
        KinesisUtil.setTraceDetails(kinesisOperation, tracedMethod);
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

    public static void setTraceDetails(String kinesisOperation, TracedMethod tracedMethod) {
        tracedMethod.setMetricName(TRACE_CATEGORY, kinesisOperation);
        tracedMethod.reportAsExternal(createCloudParams());
    }

    public static CloudParameters createCloudParams() {
        // Todo: add arn to cloud parameters
        return CloudParameters.provider(PLATFORM).build();
    }

}
