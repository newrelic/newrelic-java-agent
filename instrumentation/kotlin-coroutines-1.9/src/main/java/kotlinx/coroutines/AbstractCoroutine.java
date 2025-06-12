package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_19.NRFunction2Wrapper;
import com.newrelic.instrumentation.kotlin.coroutines_19.Utils;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.jvm.functions.Function2;

@Weave(type=MatchType.BaseClass)
public abstract class AbstractCoroutine<T> {

        public abstract CoroutineContext getContext();
        public abstract String nameString$kotlinx_coroutines_core();

        protected void onCompleted(T value) {
                Utils.expireToken(getContext());
                Weaver.callOriginal();
        }

        protected void onCancelled(Throwable t, boolean b) {
                Utils.expireToken(getContext());
                Weaver.callOriginal();
        }

        public void handleOnCompletionException$kotlinx_coroutines_core(Throwable t) {
                Weaver.callOriginal();
        }

        @Trace
        public <R> void start(CoroutineStart start, R receiver, Function2<? super R, ? super Continuation<? super T>, ? extends Object> block) {
                if(!(block instanceof NRFunction2Wrapper)) {
                        NRFunction2Wrapper<? super R, ? super Continuation<? super T>, ? extends Object> wrapper = new NRFunction2Wrapper<>(block);
                        block = wrapper;
                }
                String ctxName = Utils.getCoroutineName(getContext());
                String name = ctxName != null ? ctxName : nameString$kotlinx_coroutines_core();
                TracedMethod traced = NewRelic.getAgent().getTracedMethod();
                traced.addCustomAttribute("Coroutine-Name", name);
                traced.addCustomAttribute("Block", block.toString());
                
                if(name != null && !name.isEmpty()) {
                        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "Coroutine", "Kotlin","Coroutine",name);
                        traced.setMetricName("Custom","Kotlin","Coroutines",getClass().getSimpleName(),"start",name);
                } else {
                        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "Coroutine", "Kotlin","Coroutine");
                }

                Weaver.callOriginal();
        }

}
