package org.eclipse.jetty.server;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jetty12.JettySampler;
import org.eclipse.jetty.util.thread.ThreadPool;

import java.util.concurrent.TimeUnit;

@Weave(type = MatchType.ExactClass, originalName = "org.eclipse.jetty.server.Server")
public abstract class Server_Instrumentation {

    protected void doStart() {
        AgentBridge.privateApi.addSampler(new JettySampler(this), 1, TimeUnit.MINUTES);
        Weaver.callOriginal();
    }

    public static String getVersion() {
        return Weaver.callOriginal();
    }


    public Connector[] getConnectors() {
        return Weaver.callOriginal();
    }

    public ThreadPool getThreadPool() {
        return Weaver.callOriginal();
    }
}
