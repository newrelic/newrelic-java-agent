package com.newrelic.instrumentation.labs.ktor.client;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ClassMethodSignatures;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.api.agent.HttpParameters;
import io.ktor.client.HttpClient_Instrumentation;
import kotlin.coroutines.Continuation;

import java.util.HashSet;
import java.util.Set;

public class KtorClientUtils {

    private static final Set<String> LEAF_CLIENTS = new HashSet<>();
    private static final String METRIC_PREFIX = "Custom/Ktor/Client/HttpClient";
    private static final String METRIC_PREFIX2 = "Custom/Ktor/Client/HttpStatement/";

    static {
        LEAF_CLIENTS.add("CIOEngine");
        LEAF_CLIENTS.add("JavaHttpEngine");
    }

    public static boolean needsLeaf(String engine) {
        return LEAF_CLIENTS.contains(engine);
    }

    public static <T> NRContinuationWrapper<T> getContinuationWrapper(Continuation<T> continuation, HttpParameters httpParameters) {
        if(continuation instanceof NRContinuationWrapper) {
            return null;
        }
        return new NRContinuationWrapper<>(continuation, httpParameters);
    }

    public static ExitTracer getExitTracer(HttpClient_Instrumentation httpStatement) {
        String methodName = "execute$ktor_client_core";
        String methodDesc = "(Lio.ktor.client.request.HttpRequestBuilder;,Lkotlin.coroutines.Continuation;)Ljava.lang.Object;";
        ClassMethodSignature signature = new ClassMethodSignature("io.ktor.client.HttpClient",methodName,methodDesc);
        int index = ClassMethodSignatures.get().getIndex(signature);
        if(index == -1) {
            index = ClassMethodSignatures.get().add(signature);
        }
        String metricName = METRIC_PREFIX + methodName;
        int flags = DefaultTracer.DEFAULT_TRACER_FLAGS + TracerFlags.LEAF;
        return AgentBridge.instrumentation.createTracer(httpStatement,index,metricName, flags);

    }

}
