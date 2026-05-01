package org.apache.camel;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.logging.Level;

@Weave(type = MatchType.Interface, originalName = "org.apache.camel.Processor")
public class Processor_Instrumentation {

    @Trace(async = true, excludeFromTransactionTrace = true)
    public void process(Exchange exchange) {
        if (exchange instanceof Exchange_Instrumentation) {
            if (((Exchange_Instrumentation) exchange).token != null) {
                ((Exchange_Instrumentation) exchange).token.link();
            }
            AgentBridge.getAgent().getLogger().log(Level.INFO, "Processor_Instrumentation Exchange token value is {}", ((Exchange_Instrumentation) exchange).token);
        }


        Weaver.callOriginal();
    }
}
