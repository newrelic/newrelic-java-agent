package org.apache.camel;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "org.apache.camel.Consumer", type = MatchType.Interface)
public class Consumer_Instrumentation {

    public Exchange createExchange(boolean autoRelease) {
        Exchange exchange = Weaver.callOriginal();
        if (exchange instanceof Exchange_Instrumentation) {
            ((Exchange_Instrumentation)exchange).fromConsumer = true;
        }
        return exchange;
    }
}
