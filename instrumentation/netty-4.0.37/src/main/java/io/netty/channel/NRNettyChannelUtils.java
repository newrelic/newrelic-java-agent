package io.netty.channel;

import com.agent.instrumentation.netty40.ResponseWrapper;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Token;
import io.netty.handler.codec.http.HttpResponse;

import java.util.logging.Level;

public class NRNettyChannelUtils {

    public static void setTokenToNull(ChannelPipeline pipeline) {
        if(pipeline instanceof DefaultChannelPipeline_Instrumentation) {
            ((DefaultChannelPipeline_Instrumentation) pipeline).nettyToken = null;
        }
    }

    public static com.newrelic.api.agent.Token getToken(ChannelPipeline pipeline) {
        if(pipeline instanceof DefaultChannelPipeline_Instrumentation) {
            return ((DefaultChannelPipeline_Instrumentation) pipeline).nettyToken;
        }
        return null;
    }

    public static boolean processResponse(Object msg, ChannelPipeline pipeline) {
        if (pipeline instanceof DefaultChannelPipeline_Instrumentation) {
            DefaultChannelPipeline_Instrumentation defaultPipeline = (DefaultChannelPipeline_Instrumentation) pipeline;
            if(pipeline != null && defaultPipeline.nettyToken != null) {
                if(defaultPipeline.nettyToken instanceof Token) {
                    return processResponse(msg,(Token)defaultPipeline.nettyToken);
                }
            }
        }
        return false;
    }

    public static boolean processResponse(Object msg, Token token) {
        if (token != null) {
            if (msg instanceof HttpResponse) {
                com.newrelic.api.agent.Transaction tx = token.getTransaction();
                if (tx != null) {
                    try {
                        tx.setWebResponse(new ResponseWrapper((HttpResponse) msg));
                        tx.addOutboundResponseHeaders();
                        tx.markResponseSent();
                    } catch (Exception e) {
                        AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to set web request on transaction: {0}", tx);
                    }
                }
                token.expire();
                return true;
            }
        }
        return false;
    }

}
