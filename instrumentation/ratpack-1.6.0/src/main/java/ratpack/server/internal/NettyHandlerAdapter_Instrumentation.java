package ratpack.server.internal;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

@Weave(originalName = "ratpack.server.internal.NettyHandlerAdapter", type = MatchType.ExactClass)
public class NettyHandlerAdapter_Instrumentation {

    // This is where Ratpack has detected a full HTTP request and starts processing it.
    @Trace(dispatcher = true)
    private void newRequest(final ChannelHandlerContext ctx, final HttpRequest nettyRequest) throws Exception {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "Ratpack",
                "NettyHandlerAdapter.newRequest");
        Weaver.callOriginal();
    }
}
