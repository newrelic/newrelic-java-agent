package io.opentelemetry.context;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.Weaver;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.ExitTracerSpan;

import java.util.Collections;

class ContextHelper {
    private ContextHelper() {}

    public static Context current(Context context) {
        Span currentSpan = Span.fromContext(context);
        if (currentSpan == Span.getInvalid()) {
            Transaction transaction = AgentBridge.getAgent().getTransaction(false);
            if (transaction != null) {
                TracedMethod tracedMethod = transaction.getTracedMethod();
                if (tracedMethod instanceof ExitTracer) {
                    return context.with(new ExitTracerSpan((ExitTracer) tracedMethod, SpanKind.INTERNAL,
                            Collections.emptyMap()));
                }
            }
        }
        return context;
    }

    public static Scope makeCurrent(Context context, Scope scope) {
        final Transaction currentTransaction = AgentBridge.getAgent().getTransaction(false);
        if (currentTransaction == null) {
            Span currentSpan = Span.fromContext(context);

            if (currentSpan instanceof ExitTracerSpan) {
                return ((ExitTracerSpan)currentSpan).createScope(scope);
            }
        }
        return scope;
    }
}
