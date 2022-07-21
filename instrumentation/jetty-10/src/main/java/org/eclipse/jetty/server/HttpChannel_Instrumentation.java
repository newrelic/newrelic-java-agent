package org.eclipse.jetty.server;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.util.Callback;

import java.nio.ByteBuffer;

@Weave(type = MatchType.ExactClass, originalName = "org.eclipse.jetty.server.HttpChannel")
public abstract class HttpChannel_Instrumentation {

    protected boolean sendResponse(MetaData.Response response, ByteBuffer content, boolean complete, final Callback callback) {
        NewRelic.getAgent().getTransaction().addOutboundResponseHeaders();
        return Weaver.callOriginal();
    }

}
