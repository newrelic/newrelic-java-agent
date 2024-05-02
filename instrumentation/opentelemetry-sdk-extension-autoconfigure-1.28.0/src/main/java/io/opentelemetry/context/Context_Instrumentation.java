package io.opentelemetry.context;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Weave(type = MatchType.Interface, originalName = "io.opentelemetry.context.Context")
public abstract class Context_Instrumentation {

    public Runnable wrap(Runnable runnable) {
        return NewRelic.getAgent().getTransaction().getToken().wrap(Weaver.<Runnable>callOriginal(),
                MetricNames.getMetricName(runnable.getClass(), "run"));
    }

    public <T> Callable<T> wrap(Callable<T> callable) {
        return NewRelic.getAgent().getTransaction().getToken().wrap(Weaver.<Callable>callOriginal(),
                MetricNames.getMetricName(callable.getClass(), "call"));
    }

    /**
     * Skip instrumenting the Executor wrappers.
    */

    public <T, U> Function<T, U> wrapFunction(Function<T, U> function) {
        return NewRelic.getAgent().getTransaction().getToken().wrapFunction(Weaver.<Function>callOriginal(),
                MetricNames.getMetricName(function.getClass(), "apply"));
    }

    public <T, U, V> BiFunction<T, U, V> wrapFunction(BiFunction<T, U, V> function) {
        return NewRelic.getAgent().getTransaction().getToken().wrapFunction(Weaver.<BiFunction>callOriginal(),
                MetricNames.getMetricName(function.getClass(), "apply"));
    }

    public <T> Consumer<T> wrapConsumer(Consumer<T> consumer) {
        return NewRelic.getAgent().getTransaction().getToken().wrapConsumer(Weaver.<Consumer>callOriginal(),
                MetricNames.getMetricName(consumer.getClass(), "accept"));
    }

    public <T, U> BiConsumer<T, U> wrapConsumer(BiConsumer<T, U> consumer) {
        return NewRelic.getAgent().getTransaction().getToken().wrapConsumer(Weaver.<BiConsumer>callOriginal(),
                MetricNames.getMetricName(consumer.getClass(), "accept"));
    }

    public <T> Supplier<T> wrapSupplier(Supplier<T> supplier) {
        return NewRelic.getAgent().getTransaction().getToken().wrapSupplier(Weaver.<Supplier>callOriginal(),
                MetricNames.getMetricName(supplier.getClass(), "get"));
    }
}
