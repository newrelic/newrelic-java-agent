package io.ktor.server.netty.cio;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.ktor.server.netty.NettyApplicationCall_Instrumentation;

@Weave(originalName = "io.ktor.server.netty.cio.NettyHttpResponsePipeline")
public abstract class NettyHttpResponsePipeline_Instrumentation {

    @Trace(async = true)
    private void handleRequestMessage(NettyApplicationCall_Instrumentation call) {
        if(call.token != null) {
            call.token.linkAndExpire();
            call.token = null;
        }
        Weaver.callOriginal();
    }

    @Trace
    public void processResponse$ktor_server_netty(NettyApplicationCall_Instrumentation call) {
        if(call.token == null) {
            Token t = NewRelic.getAgent().getTransaction().getToken();
            if(t != null && t.isActive()) {
                call.token = t;
            } else if(t != null) {
                t.expire();
                t = null;
            }
        }
        Weaver.callOriginal();
    }

}