package ratpack.http.internal;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import nr.ratpack.instrumentation.RatpackResponse;
import ratpack.func.Action;
import ratpack.http.Response;

import java.util.Iterator;
import java.util.List;

@Weave(originalName = "ratpack.http.internal.DefaultResponse")
public abstract class DefaultResponse_Instrumentation implements Response {
    private List<Action<? super Response>> responseFinalizers = Weaver.callOriginal();

    private void finalizeResponse(Runnable then) {
        if (responseFinalizers.isEmpty()) {
            Transaction txn = NewRelic.getAgent().getTransaction();
            txn.setWebResponse(new RatpackResponse(this));
            txn.addOutboundResponseHeaders();
            txn.markResponseSent();
        }
        Weaver.callOriginal();
    }

    private void finalizeResponse(Iterator<Action<? super Response>> finalizers, Runnable then) {
        if (!finalizers.hasNext()) {
            Transaction txn = NewRelic.getAgent().getTransaction();
            txn.setWebResponse(new RatpackResponse(this));
            txn.addOutboundResponseHeaders();
            txn.markResponseSent();
        }

        Weaver.callOriginal();
    }

}
