package com.amazonaws;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

@Weave(originalName = "com.amazonaws.AmazonWebServiceRequest_Instrumentation")
public abstract class AmazonWebServiceRequest_Instrumentation extends AmazonWebServiceRequest {
    @NewField
    public Token token;
}
