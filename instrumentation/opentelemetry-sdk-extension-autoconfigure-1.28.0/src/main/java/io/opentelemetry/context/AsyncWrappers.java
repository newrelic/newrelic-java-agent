package io.opentelemetry.context;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class AsyncWrappers {

    public static final String LAMBDA_IDENTIFIER = "$$Lambda$";

    static Runnable wrap(Runnable runnable, Runnable wrappedRunnable) {
        final Token token = getToken();
        return new Runnable() {
            @Override
            @Trace(async = true)
            public void run() {
                NewRelic.getAgent().getTracedMethod().setMetricName(getMetricName(wrappedRunnable.getClass(), "run"));
                token.linkAndExpire();
                runnable.run();
            }
        };
    }

    static <T> Callable<T> wrap(Callable<T> callable, Callable<T> wrappedCallable) {
        final Token token = getToken();
        return new Callable<T>() {
            @Override
            @Trace(async = true)
            public T call() throws Exception {
                NewRelic.getAgent().getTracedMethod().setMetricName(getMetricName(wrappedCallable.getClass(), "call"));
                token.linkAndExpire();
                return callable.call();
            }
        };
    }

    static <T, U> Function<T, U> wrapFunction(Function<T, U> function, Function<T, U> wrappedFunction) {
        final Token token = getToken();
        return new Function<T, U>() {
            @Override
            @Trace(async = true)
            public U apply(T t) {
                NewRelic.getAgent().getTracedMethod().setMetricName(getMetricName(wrappedFunction.getClass(), "apply"));
                token.linkAndExpire();
                return function.apply(t);
            }
        };
    }

    static <T, U, V> BiFunction<T, U, V> wrapFunction(BiFunction<T, U, V> function, BiFunction<T, U, V> wrappedFunction) {
        final Token token = getToken();
        return new BiFunction<T, U, V>() {
            @Override
            @Trace(async = true)
            public V apply(T t, U u) {
                NewRelic.getAgent().getTracedMethod().setMetricName(getMetricName(wrappedFunction.getClass(), "apply"));
                token.linkAndExpire();
                return function.apply(t, u);
            }
        };
    }

    static <T> Consumer<T> wrapConsumer(Consumer<T> consumer, Consumer<T> wrappedConsumer) {
        final Token token = getToken();
        return new Consumer<T>() {
            @Override
            @Trace(async = true)
            public void accept(T t) {
                NewRelic.getAgent().getTracedMethod().setMetricName(getMetricName(wrappedConsumer.getClass(), "accept"));
                token.linkAndExpire();
                consumer.accept(t);
            }
        };
    }

    static <T, U> BiConsumer<T, U> wrapConsumer(BiConsumer<T, U> consumer, BiConsumer<T, U> wrappedConsumer) {
        final Token token = getToken();
        return new BiConsumer<T, U>() {
            @Override
            @Trace(async = true)
            public void accept(T t, U u) {
                NewRelic.getAgent().getTracedMethod().setMetricName(getMetricName(wrappedConsumer.getClass(), "accept"));
                token.linkAndExpire();
                consumer.accept(t, u);
            }
        };
    }

    static <T> Supplier<T> wrapSupplier(Supplier<T> supplier, Supplier<T> wrappedSupplier) {
        final Token token = getToken();
        return new Supplier<T>() {
            @Override
            @Trace(async = true)
            public T get() {
                NewRelic.getAgent().getTracedMethod().setMetricName(getMetricName(wrappedSupplier.getClass(), "get"));
                token.linkAndExpire();
                return supplier.get();
            }
        };
    }

    static Token getToken() {
        return NewRelic.getAgent().getTransaction().getToken();
    }

    static String getMetricName(Class<?> clazz, String methodName) {
        final String className = trimClassName(clazz.getName());
        return getMetricName(trimClassName(className), methodName);
    }

    static String trimClassName(String className) {
        final int lambdaIndex = className.indexOf(LAMBDA_IDENTIFIER);
        if (lambdaIndex > 0) {
            return className.substring(0, lambdaIndex) + LAMBDA_IDENTIFIER;
        }
        return className;
    }

    static String getMetricName(String className, String methodName) {
        return "Java/" + className + '.' + methodName;
    }
}
