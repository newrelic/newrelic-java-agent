package kotlinx.coroutines.intrinsics;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines_19.NRContinuationWrapper;
import com.newrelic.instrumentation.kotlin.coroutines_19.NRFunction1Wrapper;
import com.newrelic.instrumentation.kotlin.coroutines_19.NRFunction2Wrapper;
import com.newrelic.instrumentation.kotlin.coroutines_19.Utils;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.jvm.internal.SuspendFunction;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

@Weave(originalName = "kotlinx.coroutines.intrinsics.CancellableKt")
public abstract class CancellableKt_Instrumentation {

        @Trace
        public static <T> void startCoroutineCancellable(Function1<? super Continuation<? super T>, ? extends java.lang.Object> f, Continuation<? super T> cont) {
                String continuationString = Utils.getContinuationString(cont);
                if(!(cont instanceof SuspendFunction)) {
                        if(!(cont instanceof NRContinuationWrapper) && Utils.continueContinuation(continuationString)) {
                            cont = new NRContinuationWrapper<>(cont, continuationString);
                        }
                }
                if(continuationString != null) {
                        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Continuation", continuationString);
                }
                NewRelic.getAgent().getTracedMethod().addCustomAttribute("Suspend-Type", "Function1");
                if(!(f instanceof NRFunction1Wrapper)) {
                    f = new NRFunction1Wrapper<>(f);
                }
                Weaver.callOriginal();
        }

        @Trace
        public static <R, T> void startCoroutineCancellable(Function2<? super R, ? super Continuation<? super T>, ? extends java.lang.Object> f, R receiver, Continuation<? super T> cont) {
                String continuationString = Utils.getContinuationString(cont);
                if(!(cont instanceof SuspendFunction)) {
                        // create continuation wrapper if needed
                        if(Utils.continueContinuation(continuationString) && !(cont instanceof NRContinuationWrapper)) {
                            cont = new NRContinuationWrapper<>(cont, continuationString);
                        }
                }
                if(continuationString != null) {
                        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Continuation", continuationString);
                }
                NewRelic.getAgent().getTracedMethod().addCustomAttribute("Suspend-Type", "Function2");
                NewRelic.getAgent().getTracedMethod().addCustomAttribute("Receiver", receiver.getClass().getName());

                if(!(f instanceof NRFunction2Wrapper)) {
                    f = new NRFunction2Wrapper<>(f);
                }
                Weaver.callOriginal();
        }

        @Trace
        public static void startCoroutineCancellable(Continuation<? super kotlin.Unit> completion, Continuation<?> cont) {
                String completionString = Utils.getContinuationString(completion);
                if(!(completion instanceof SuspendFunction)) {
                        // create continuation wrapper if needed
                        if(Utils.continueContinuation(completionString) && !(completion instanceof NRContinuationWrapper)) {
                            completion = new NRContinuationWrapper<>(completion, completionString);
                        }
                }
                String continuationString = Utils.getContinuationString(cont);
                if(!(cont instanceof SuspendFunction)) {
                        // create continuation wrapper if needed
                        if(Utils.continueContinuation(continuationString) && !(cont instanceof NRContinuationWrapper)) {
                            cont = new NRContinuationWrapper<>(cont, continuationString);
                        }
                }
                TracedMethod traced = NewRelic.getAgent().getTracedMethod();
                if(completionString != null) {
                        traced.addCustomAttribute("Completion", completionString);
                }
                if(continuationString != null) {
                        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Continuation", continuationString);
                }
                traced.addCustomAttribute("Suspend-Type", "None");
                Weaver.callOriginal();
        }

}
