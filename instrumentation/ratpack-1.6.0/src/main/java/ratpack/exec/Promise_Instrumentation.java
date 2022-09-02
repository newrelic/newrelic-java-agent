package ratpack.exec;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import nr.ratpack.instrumentation.RatpackUtil;

@Weave(originalName = "ratpack.exec.Promise", type = MatchType.Interface)
public class Promise_Instrumentation<T> {
    public static <T> Promise_Instrumentation<T> value(T t) {
        final Promise_Instrumentation o = Weaver.callOriginal();
        RatpackUtil.expireTokenForPromise(o);
        return o;
    }
}
