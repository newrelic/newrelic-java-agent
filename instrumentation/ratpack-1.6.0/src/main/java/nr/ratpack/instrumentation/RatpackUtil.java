package nr.ratpack.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weaver;
import ratpack.exec.ExecResult;
import ratpack.exec.Promise;
import ratpack.exec.internal.Continuation;
import ratpack.exec.internal.DefaultPromise_Instrumentation;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.func.BiFunction;
import ratpack.func.Function;
import ratpack.handling.RequestOutcome;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class RatpackUtil {
    private static final Map<Object, Token> tokenMap = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();

    public static void storeTokenForContinuation(Action<? super Continuation> segment) {
        tokenMap.put(segment, NewRelic.getAgent().getTransaction().getToken());
    }

    public static Token getTokenForContinuation(Action<? super Continuation> initial) {
        return tokenMap.remove(initial);
    }

    public static Action<? super RequestOutcome> expireTokenAction(final Object context) {
        return new Action<RequestOutcome>() {
            @Override
            public void execute(RequestOutcome requestOutcome) throws Exception {
                try {
                    removeTokenForContext(context).expire();
                } catch (Throwable t) {
                    AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
                }
            }
        };
    }

    public static <T> BiAction<? super Integer, ? super T> biActionWrapper(Token token,
            BiAction<? super Integer, ? super T> original) {
        return new BiAction<Integer, T>() {
            @Trace(async = true)
            public void execute(Integer integer, T value) throws Exception {
                try {
                    token.link();
                } catch (Throwable t) {
                    AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
                }
                original.execute(integer, value);
            }
        };
    }

    public static void storeTokenForContext(Object context, Token token) {
        tokenMap.put(context, token);
    }

    public static Token getTokenForContext(Object context) {
        return tokenMap.get(context);
    }

    private static Token removeTokenForContext(Object context) {
        return tokenMap.remove(context);
    }

    public static Action<? super Optional<? extends Throwable>> expireToken(Token token) {
        return (Action<Optional<? extends Throwable>>) throwable -> token.expire();
    }

    public static void expireTokenForPromise(Object promise) {
        if (promise != null) {
            final Token token = tokenMap.remove(promise);
            if (token != null) {
                token.linkAndExpire();
            }
        }
    }

    public static <T> BiFunction<Integer, ExecResult<T>, Boolean> wrapOnError(Iterator<? extends Promise<T>> promises,
            BiFunction<Integer, ExecResult<T>, Boolean> original) {
        return new BiFunction<Integer, ExecResult<T>, Boolean>() {

            @Override
            public Boolean apply(Integer integer, ExecResult<T> execResult) throws Exception {
                promises.forEachRemaining(p -> {
                    expireTokenForPromise(p);
                });
                return original.apply(integer, execResult);
            }

            @Override
            public <V> BiFunction<Integer, ExecResult<T>, V> andThen(Function<? super Boolean, ? extends V> transform) {
                return original.andThen(transform);
            }
        };
    }

    public static <T> void storeTokenForPromise(Object promise, Token token) {
        tokenMap.put(promise, token);
    }

    public static <T> Token getAndRemoveTokenForPromise(Object promise) {
        return tokenMap.remove(promise);
    }
}

