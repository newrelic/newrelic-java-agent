package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_19.NRTokenContextKt;
import com.newrelic.instrumentation.kotlin.coroutines_19.Utils;
import kotlin.coroutines.CoroutineContext;

@Weave(originalName = "kotlinx.coroutines.CoroutineContextKt")
public class CoroutineContextKt_Instrumentation {

    public static CoroutineContext newCoroutineContext(CoroutineScope scope, CoroutineContext context) {
        CoroutineContext ctx = Weaver.callOriginal();
        Token token = Utils.getToken(ctx);
        if (token != null) {
            token.link();
        } else {
            Token token2 = NewRelic.getAgent().getTransaction().getToken();
            if(token2 != null) {
                if(token2.isActive()) {
                    return NRTokenContextKt.addTokenContext(ctx, token2);
                } else {
                    token2.expire();
                    token2 = null;
                }
            }
        }
        return ctx;
    }

    public static CoroutineContext newCoroutineContext(CoroutineContext ctx1,CoroutineContext ctx2) {
        CoroutineContext ctx = Weaver.callOriginal();
        Token token = Utils.getToken(ctx);
        if (token != null) {
            token.link();
        } else {
            Token token2 = NewRelic.getAgent().getTransaction().getToken();
            if(token2 != null) {
                if(token2.isActive()) {
                    return NRTokenContextKt.addTokenContext(ctx, token2);
                } else {
                    token2.expire();
                    token2 = null;
                }
            }
        }
        return ctx;
    }


}
