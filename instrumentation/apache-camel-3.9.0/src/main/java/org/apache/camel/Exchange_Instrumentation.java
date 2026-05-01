package org.apache.camel;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

@Weave(type = MatchType.Interface, originalName = "org.apache.camel.Exchange")
public abstract class Exchange_Instrumentation {
    @NewField
    public Token token = null;
    @NewField
    public Segment outboundSegment = null;
    @NewField
    public boolean consumerTxnStarted = false;
}
