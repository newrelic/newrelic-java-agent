package com.newrelic.instrumentation.kotlin.suspends;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.kotlincoroutines.KotlinCoroutinesService;
import com.newrelic.agent.kotlincoroutines.SuspendsConfigListener;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.*;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.AbstractCoroutine;

import java.util.Arrays;
import java.util.HashSet;

public class SuspendsUtils implements SuspendsConfigListener {

    private static final HashSet<String> ignoredSuspends = new HashSet<>();
    public static final String CREATE_METHOD1 = "Continuation at kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$IntrinsicsKt__IntrinsicsJvmKt$4";
    public static final String CREATE_METHOD2 = "Continuation at kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$IntrinsicsKt__IntrinsicsJvmKt$3";
    private static final String CONT_LOC = "Continuation at";
    public static String sub = "createCoroutineFromSuspendFunction";
    public static final String KOTLIN_PACKAGE = "kotlin";

    static {
        KotlinCoroutinesService service = ServiceFactory.getKotlinCoroutinesService();
        service.addSuspendsConfigListener(new SuspendsUtils());
    }

    private SuspendsUtils() {}

    public static void addIgnoredSuspend(String suspendName) {
        ignoredSuspends.add(suspendName);
    }

    public static ExitTracer getSuspendTracer(Continuation<?> continuation) {
        Class<?> clazz = continuation.getClass();
        String className = clazz.getName();
        // don't track suspend functions in internal Coroutines classes
        if(className.startsWith(KOTLIN_PACKAGE)) { return null; }
        String continuationString = getContinuationString(continuation);
        if (continuationString == null || continuationString.isEmpty() || ignoredSuspends.contains(continuationString)) {
            return null;
        }
        ClassMethodSignature signature = new ClassMethodSignature(clazz.getName(), "invokeSuspend", "(Ljava.lang.Object;)Ljava.lang.Object;");
        int index = ClassMethodSignatures.get().getIndex(signature);
        if(index == -1) {
            index = ClassMethodSignatures.get().add(signature);
        }

        if(index >= 0) {
            String metricName = "Custom/Kotlin/Coroutines/SuspendFunction/" + continuationString;
            return AgentBridge.instrumentation.createTracer(continuation, index, metricName, DefaultTracer.DEFAULT_TRACER_FLAGS);
        }
        return null;
    }

    public static <T> String getContinuationString(Continuation<T> continuation) {
        String contString = continuation.toString();

        if(contString.equals(CREATE_METHOD1) || contString.equals(CREATE_METHOD2)) {
            return sub;
        }

        if(contString.startsWith(CONT_LOC)) {
            return contString;
        }

        if(continuation instanceof AbstractCoroutine) {
            return ((AbstractCoroutine<?>)continuation).nameString$kotlinx_coroutines_core();
        }

        int index = contString.indexOf('@');
        if(index > -1) {
            return contString.substring(0, index);
        }

        return null;
    }

    @Override
    public void configureSuspendsIgnores(String[] ignores) {
        if(ignores != null) {
            ignoredSuspends.addAll(Arrays.asList(ignores));
        }
    }
}
