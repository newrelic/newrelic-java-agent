package org.apache.camel;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.apache.camel.NrCamelTracingService;

import java.util.logging.Level;

@Weave(originalName = "org.apache.camel.CamelContextLifecycle", type = MatchType.Interface)
public abstract class CamelContextLifecycle_Instrumentation {

    public void start() {
        try {
            if (this instanceof CamelContext) {
                CamelContext context = (CamelContext) this;
                if (context.hasService(NrCamelTracingService.class) == null) {
                    context.addService(new NrCamelTracingService((CamelContext) this), true, true);
                }
            }
        } catch (Exception e) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "Failed to add NrCamelTracingService to CamelContext", e);
        }

        Weaver.callOriginal();
    }
}
