package com.newrelic.instrumentation.kotlin.suspends;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.kotlincoroutines.KotlinCoroutinesService;
import com.newrelic.agent.kotlincoroutines.SuspendsConfigListener;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.*;
import com.newrelic.api.agent.NewRelic;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.AbstractCoroutine;

import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;

public class SuspendsUtils implements SuspendsConfigListener {

    private static final HashSet<String> ignoredSuspends = new HashSet<>();
    public static final String CREATE_METHOD1 = "Continuation at kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$IntrinsicsKt__IntrinsicsJvmKt$4";
    public static final String CREATE_METHOD2 = "Continuation at kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$IntrinsicsKt__IntrinsicsJvmKt$3";
    private static final String CONT_LOC = "Continuation at";
    public static String sub = "createCoroutineFromSuspendFunction";
    public static final String KOTLIN_PACKAGE = "kotlin";
    private static final String SUSPEND_FUNCTION_METRIC_NAME_PREFIX = "Custom/Kotlin/Coroutines/SuspendFunction/";

    static {
        KotlinCoroutinesService service = ServiceFactory.getKotlinCoroutinesService();
        service.addSuspendsConfigListener(new SuspendsUtils());
    }

    private SuspendsUtils() {}

    public static ExitTracer getSuspendTracer(Continuation<?> continuation) {
        Class<?> clazz = continuation.getClass();
        String className = clazz.getName();
        // don't track suspend functions in internal Coroutines classes (i.e. starts with kotlin or kotlinx)
        if(className.startsWith(KOTLIN_PACKAGE)) { return null; }
        String continuationString = getContinuationString(continuation);
        // ignore if can't determine continuation string
        if(continuationString == null || continuationString.isEmpty()) { return null; }

        for(String ignoredSuspend : ignoredSuspends) {
            if(continuationString.matches(ignoredSuspend) || className.matches(ignoredSuspend)) { return null; }
        }
        if (ignoredSuspends.contains(continuationString) || ignoredSuspends.contains(className)) {
            return null;
        }
        ClassMethodSignature signature = new ClassMethodSignature(clazz.getName(), "invokeSuspend", "(Ljava.lang.Object;)Ljava.lang.Object;");
        int index = ClassMethodSignatures.get().getIndex(signature);
        if(index == -1) {
            index = ClassMethodSignatures.get().add(signature);
        }

        if(index >= 0) {
            String metricName = SUSPEND_FUNCTION_METRIC_NAME_PREFIX + continuationString;
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
        NewRelic.getAgent().getLogger().log(Level.FINE,"Will ignore Suspend Functions matching {0}", Arrays.toString(ignores));
        ignoredSuspends.clear();
        if(ignores != null) {
            ignoredSuspends.addAll(Arrays.asList(ignores));
        }
    }
}
