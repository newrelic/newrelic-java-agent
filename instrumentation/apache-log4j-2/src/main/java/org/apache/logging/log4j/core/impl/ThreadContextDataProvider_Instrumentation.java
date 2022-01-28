package org.apache.logging.log4j.core.impl;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.log4j2.AgentUtil;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.util.ContextDataProvider;

import java.util.Map;

@Weave(originalName = "org.apache.logging.log4j.core.impl.ThreadContextDataProvider", type = MatchType.ExactClass)
public class ThreadContextDataProvider_Instrumentation implements ContextDataProvider {

    @Override
    public Map<String, String> supplyContextData() {
        // TODO check config to see if logs should be decorated or not
//        if (shouldDecorateLogs) {
//            ThreadContext.putAll(AgentUtil.getNewRelicLinkingMetadata());
//        }
        // Inject the agent linking metadata into the Log4j2 ThreadContext
        ThreadContext.putAll(AgentUtil.getNewRelicLinkingMetadata());
        return Weaver.callOriginal();
    }
}
