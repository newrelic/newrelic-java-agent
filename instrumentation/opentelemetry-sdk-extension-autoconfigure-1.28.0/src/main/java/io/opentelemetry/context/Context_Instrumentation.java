package io.opentelemetry.context;

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
        return AsyncWrappers.wrap(Weaver.<Runnable>callOriginal(), runnable);
    }

    public <T> Callable<T> wrap(Callable<T> callable) {
        return AsyncWrappers.wrap(Weaver.<Callable>callOriginal(), callable);
    }

    /**
     * Skip instrumenting the Executor wrappers.
    */

    public <T, U> Function<T, U> wrapFunction(Function<T, U> function) {
        return AsyncWrappers.wrapFunction(Weaver.<Function>callOriginal(), function);
    }

    public <T, U, V> BiFunction<T, U, V> wrapFunction(BiFunction<T, U, V> function) {
        return AsyncWrappers.wrapFunction(Weaver.<BiFunction>callOriginal(), function);
    }

    public <T> Consumer<T> wrapConsumer(Consumer<T> consumer) {
        return AsyncWrappers.wrapConsumer(Weaver.<Consumer>callOriginal(), consumer);
    }

    public <T, U> BiConsumer<T, U> wrapConsumer(BiConsumer<T, U> consumer) {
        return AsyncWrappers.wrapConsumer(Weaver.<BiConsumer>callOriginal(), consumer);
    }

    public <T> Supplier<T> wrapSupplier(Supplier<T> supplier) {
        return AsyncWrappers.wrapSupplier(Weaver.<Supplier>callOriginal(), supplier);
    }
}
