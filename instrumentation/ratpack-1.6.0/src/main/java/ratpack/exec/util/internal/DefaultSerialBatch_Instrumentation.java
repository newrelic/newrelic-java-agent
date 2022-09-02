package ratpack.exec.util.internal;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import nr.ratpack.instrumentation.RatpackUtil;
import ratpack.exec.ExecResult;
import ratpack.exec.Promise;
import ratpack.func.BiAction;
import ratpack.func.BiFunction;

import java.util.Iterator;

@Weave(type = MatchType.ExactClass, originalName = "ratpack.exec.util.internal.DefaultSerialBatch")
public class DefaultSerialBatch_Instrumentation {

    private static <T> void yieldPromise(Iterator<? extends Promise<T>> promises, int i,
            BiAction<Integer, ExecResult<T>> withItem, BiFunction<Integer, ExecResult<T>, Boolean> onError,
            Runnable onComplete) {

        final BiFunction<Integer, ExecResult<T>, Boolean> wrapped = RatpackUtil.wrapOnError(promises, onError);
        onError = wrapped;
        Weaver.callOriginal();
    }
}