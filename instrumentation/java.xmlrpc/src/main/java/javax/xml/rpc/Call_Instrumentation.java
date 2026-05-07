package javax.xml.rpc;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import javax.xml.namespace.QName;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.logging.Level;

@Weave(originalName = "javax.xml.rpc.Call", type = MatchType.Interface)
public abstract class Call_Instrumentation {
    @Trace
    public Object invoke(Object[] inputParams) throws RemoteException {
        Object response = Weaver.callOriginal();
        reportExternal();

        return response;
    }

    @Trace
    public Object invoke(QName operationName, Object[] inputParams) throws RemoteException {
        Object response = Weaver.callOriginal();
        reportExternal();

        return response;
    }

    public abstract String getTargetEndpointAddress();

    private void reportExternal() {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);

        if (transaction == null) {
            return;
        }

        transaction.getTracedMethod().setMetricName("Java", "Call", "invoke");
        String endpointAddress = getTargetEndpointAddress();
        URI uri;

        try {
            uri = URI.create(endpointAddress);
        } catch (Exception e) {
            uri = URI.create("http://UnknownHost/");
        }

        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Reporting XML RPC external to: {0}", uri);

        NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                .library("XmlRpc")
                .uri(uri)
                .procedure("invoke")
                .noInboundHeaders()
                .build());

    }
}
