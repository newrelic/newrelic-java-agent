package kotlinx.coroutines.channels;

import java.util.logging.Level;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import kotlin.coroutines.Continuation;
import kotlinx.coroutines.selects.SelectClause1;

@Weave(type=MatchType.Interface)
public abstract class ReceiveChannel<E> {

    @Trace
    public Object receive(Continuation<? super E> c) {
        Exception exp = new Exception("receiving");
        NewRelic.getAgent().getLogger().log(Level.FINE, exp, "Call to {0}.resceive({1})",getClass().getName(),c);
        return Weaver.callOriginal();
    }

    @Trace
    public abstract Object receiveOrNull(Continuation<? super E> c);
    
    @Trace
    public abstract SelectClause1<E> getOnReceiveOrNull();
    
    @Trace
    public abstract E poll();
    
    @Trace
    public abstract boolean cancel(Throwable t);
    
    @Trace
    public abstract void cancel();

}
