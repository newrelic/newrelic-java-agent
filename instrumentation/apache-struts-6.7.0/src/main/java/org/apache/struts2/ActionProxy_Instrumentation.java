package org.apache.struts2;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "org.apache.struts2.ActionProxy")
public abstract class ActionProxy_Instrumentation {
    @Trace
    public String execute() throws Exception {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null) {
            transaction.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "StrutsAction", getActionName());
        }

        return Weaver.callOriginal();
    }

    public abstract String getActionName();
}
