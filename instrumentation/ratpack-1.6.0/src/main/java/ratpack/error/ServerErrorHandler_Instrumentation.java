package ratpack.error;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import nr.ratpack.instrumentation.RatpackUtil;
import ratpack.handling.Context;

@Weave(type = MatchType.Interface, originalName = "ratpack.error.ServerErrorHandler")
public class ServerErrorHandler_Instrumentation {

    @Trace(async = true)
    public void error(Context context, Throwable throwable) throws Exception {
        Token token = RatpackUtil.getTokenForContext(context);
        if (token != null) {
            token.link();
        }
        NewRelic.noticeError(throwable);
        Weaver.callOriginal();
    }
}