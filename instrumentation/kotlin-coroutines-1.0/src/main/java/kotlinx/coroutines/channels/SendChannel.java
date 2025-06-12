package kotlinx.coroutines.channels;

import java.util.logging.Level;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Weave(type=MatchType.Interface)
public abstract class SendChannel<E> {

    @Trace
    public Object send(E e, Continuation<? super Unit> c) {
        Exception exp = new Exception("sending");
        NewRelic.getAgent().getLogger().log(Level.FINE, exp, "Call to {0}.send({1},{2})",getClass().getName(),e,c);
        return Weaver.callOriginal();
    }
    
}
