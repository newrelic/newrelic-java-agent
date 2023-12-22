package java.lang;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "java.lang.Process")
public class Process_Instrumentation {
    @Trace
    public int waitFor() {
        //System.out.println("java.lang.Process is da bomb");
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null) {
            transaction.getTracedMethod().setMetricName("Java", "Process", "waitFor");
        }
        return Weaver.callOriginal();
    }
}
