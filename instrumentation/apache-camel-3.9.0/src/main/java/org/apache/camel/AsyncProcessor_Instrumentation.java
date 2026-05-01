package org.apache.camel;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.logging.Level;

@Weave(originalName = "org.apache.camel.AsyncProcessor", type = MatchType.Interface)
public class AsyncProcessor_Instrumentation {

    @Trace(async = true, excludeFromTransactionTrace = true)
    public boolean process(Exchange exchange, AsyncCallback callback) {
        if (exchange instanceof Exchange_Instrumentation) {
            if (((Exchange_Instrumentation) exchange).token != null) {
                ((Exchange_Instrumentation) exchange).token.link();
            }
        }

        return Weaver.callOriginal();
    }
}
