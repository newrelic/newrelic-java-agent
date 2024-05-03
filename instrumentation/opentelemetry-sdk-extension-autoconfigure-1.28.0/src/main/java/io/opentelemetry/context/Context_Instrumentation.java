package io.opentelemetry.context;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.ExitTracerSpan;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Weave(type = MatchType.Interface, originalName = "io.opentelemetry.context.Context")
public abstract class Context_Instrumentation {
    public static Context current() {
        return ContextHelper.current(Weaver.callOriginal());
    }

    public Scope makeCurrent() {
        return ContextHelper.makeCurrent((Context)this, Weaver.callOriginal());
    }
}
