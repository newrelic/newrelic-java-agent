package com.agent.instrumentation.awsjavasdk2.services.lambda;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceRequest_Instrumentation;
import com.amazonaws.handlers.AsyncHandler_Instrumentation;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.CloudParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TracedMethod;

public class KinesisUtil {

    public static final String PLATFORM = "aws_kinesis_data_streams";
    public static final String TRACE_CATEGORY = "Kinesis";
    private KinesisUtil() {}

    public static void setupToken(AsyncHandler_Instrumentation asyncHandler, AmazonWebServiceRequest amazonWebServiceRequest) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            if (asyncHandler != null) {
                asyncHandler.token = NewRelic.getAgent().getTransaction().getToken();
            }
            if (amazonWebServiceRequest != null && amazonWebServiceRequest instanceof AmazonWebServiceRequest_Instrumentation) {
                AmazonWebServiceRequest_Instrumentation request = (AmazonWebServiceRequest_Instrumentation)amazonWebServiceRequest;
                request.token = NewRelic.getAgent().getTransaction().getToken();
            }
        }
    }

    public static void linkAndExpireToken(AmazonWebServiceRequest amazonWebServiceRequest) {
        if (amazonWebServiceRequest != null && amazonWebServiceRequest instanceof AmazonWebServiceRequest_Instrumentation) {
            AmazonWebServiceRequest_Instrumentation request = (AmazonWebServiceRequest_Instrumentation)amazonWebServiceRequest;
            if (request.token != null) {
                request.token.linkAndExpire();
                request.token = null;
            }
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
