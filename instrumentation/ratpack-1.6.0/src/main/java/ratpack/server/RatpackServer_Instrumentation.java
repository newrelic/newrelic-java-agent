package ratpack.server;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import ratpack.util.RatpackVersion;

@Weave(originalName = "ratpack.server.RatpackServer", type = MatchType.Interface)
public abstract class RatpackServer_Instrumentation {

    public void start() throws Exception {
        Weaver.callOriginal();
        NewRelic.setServerInfo("Ratpack", RatpackVersion.getVersion());
        NewRelic.setAppServerPort(getBindPort());
    }

    public abstract int getBindPort();
}
