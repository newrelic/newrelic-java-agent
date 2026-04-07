package org.apache.struts2.result;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.struts2.ActionInvocation;

@Weave(type = MatchType.Interface, originalName = "org.apache.struts2.result.Result")
public class Result_Instrumentation {
    @Trace
    public void execute(ActionInvocation invocation) throws Exception {
        TracedMethod tracedMethod =  NewRelic.getAgent().getTracedMethod();
        if (tracedMethod != null && invocation != null) {
            tracedMethod.setMetricName("StrutsResult", invocation.getAction().getClass().getName());
        }

        Weaver.callOriginal();
    }
}
