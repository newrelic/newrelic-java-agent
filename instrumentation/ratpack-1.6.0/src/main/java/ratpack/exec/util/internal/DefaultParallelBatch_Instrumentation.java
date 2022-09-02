package ratpack.exec.util.internal;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import nr.ratpack.instrumentation.RatpackUtil;
import ratpack.exec.Operation;
import ratpack.func.BiAction;

@Weave(type = MatchType.ExactClass, originalName = "ratpack.exec.util.internal.DefaultParallelBatch")
public class DefaultParallelBatch_Instrumentation<T> {

    public Operation forEach(BiAction<? super Integer, ? super T> consumer) {
        final Token token = NewRelic.getAgent().getTransaction().getToken();

        // Wrap the consumer so we can get the transaction into the execute() method
        final BiAction<? super Integer, ? super T> originalConsumer = consumer;
        consumer = RatpackUtil.biActionWrapper(token, originalConsumer);
        Operation operation = Weaver.callOriginal();
        return operation.wiretap(RatpackUtil.expireToken(token));
    }

}
