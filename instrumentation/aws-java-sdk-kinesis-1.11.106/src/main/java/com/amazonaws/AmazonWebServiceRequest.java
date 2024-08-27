package com.amazonaws;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

@Weave(originalName = "com.amazonaws.AmazonWebServiceRequest")
public class AmazonWebServiceRequest {
    @NewField
    public Token token;
}
