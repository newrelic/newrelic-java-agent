package com.newrelic.instrumentation.kotlin.coroutines_14;

import java.util.ArrayList;
import java.util.List;
import com.newrelic.agent.kotlincoroutines.CoroutineConfigListener;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.jvm.internal.BaseContinuationImpl;
import kotlinx.coroutines.AbstractCoroutine_Instrumentation;
import kotlinx.coroutines.CoroutineDispatcher;
import kotlinx.coroutines.CoroutineName;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.DispatchedTask;
import org.jetbrains.annotations.Nullable;

public class Utils implements CoroutineConfigListener {

    private static final List<String> ignoredContinuations = new ArrayList<String>();
    private static final List<String> ignoredScopes = new ArrayList<>();

    public static final String CREATE_METHOD_1 = "Continuation at kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$IntrinsicsKt__IntrinsicsJvmKt$4";
    public static final String CREATE_METHOD_2 = "Continuation at kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$IntrinsicsKt__IntrinsicsJvmKt$3";
    private static final Utils INSTANCE = new Utils();
    private static final String CONT_LOC = "Continuation at";

    public static boolean DELAYED_ENABLED = true;

    static {
        ignoredContinuations.add(CREATE_METHOD_1);
        ignoredContinuations.add(CREATE_METHOD_2);
        ServiceFactory.getKotlinCoroutinesService().addCoroutineConfigListener(INSTANCE);
    }

    /*
    * Wrap runnable objects with NR wrapper runnable that includes an async token
    * */
    public static NRRunnable getRunnableWrapper(Runnable r) {
        // Don't wrap if it already is an NRRunnable
        if(r instanceof NRRunnable) {
            return null;
        }
        // if DispatchedTask then check if we ignore
        if(r instanceof DispatchedTask) {
            DispatchedTask<?> task = (DispatchedTask<?>)r;
            Continuation<?> cont = task.getDelegate$kotlinx_coroutines_core();
            String cont_string = getContinuationString(cont);
            if(cont_string != null && DispatchedTaskIgnores.ignoreDispatchedTask(cont_string)) {
                return null;
            }
        }
        
        Token t = NewRelic.getAgent().getTransaction().getToken();
        if(t != null && t.isActive()) {
            return new NRRunnable(r, t);
        } else if(t != null) {
            t.expire();
            t = null;
        }
        return null;
    }

    /*
    * see whether to track on the CoroutineScope
     */
    public static boolean continueWithScope(CoroutineScope scope) {
        CoroutineContext ctx = scope.getCoroutineContext();
        String name = getCoroutineName(ctx);
        String className = scope.getClass().getName();
        return continueWithScope(className) || continueWithScope(name);
    }
    
    public static boolean continueWithScope(String coroutineScope) {
        return !ignoredScopes.contains(coroutineScope);
    }
    
    public static boolean continueWithContinuation(String cont_string) {
        return !ignoredContinuations.contains(cont_string);
    }
    
    public static String sub = "createCoroutineFromSuspendFunction";

    /*
    * Sets an aysnc token in the context
    * */
    public static NRCoroutineToken setToken(CoroutineContext context) {
        NRCoroutineToken coroutineToken = context.get(NRCoroutineToken.key);
        if(coroutineToken == null) {
            Token t = NewRelic.getAgent().getTransaction().getToken();
            if(t != null && t.isActive()) {
                coroutineToken = new NRCoroutineToken(t);
                return coroutineToken;
            } else if(t != null) {
                t.expire();
                t = null;
                return null;
            }
        }
        return null;
    }

    /*
    * Get the async token from the context if it exists
    * */
    public static Token getToken(CoroutineContext context) {
        @Nullable
        NRCoroutineToken coroutineToken = context.get(NRCoroutineToken.key);
        Token token = null;

        if (coroutineToken != null) {
            token = coroutineToken.getToken();
        }
        return token;
    }

    /*
    * Expire the async token if present in the context.
    * Also removes it from the context
    * */
    public static void expireToken(CoroutineContext context) {
        NRCoroutineToken coroutineToken = context.get(NRCoroutineToken.key);
        if(coroutineToken != null) {
            Token token = coroutineToken.getToken();
            token.expire();
            context.minusKey(NRCoroutineToken.key);
        }
    }

    public static <T> String getCoroutineName(CoroutineContext context, Continuation<T> continuation) {
        if(continuation instanceof AbstractCoroutine_Instrumentation) {
            return ((AbstractCoroutine_Instrumentation<T>)continuation).nameString$kotlinx_coroutines_core();
        }
        if(continuation instanceof BaseContinuationImpl) {
            return ((BaseContinuationImpl)continuation).toString();
        }
        return null;
    }

    public static String getCoroutineName(CoroutineContext context) {
        CoroutineName cName = context.get(CoroutineName.Key);
        if(cName != null) {
            String name = cName.getName();
            if(name != null && !name.isEmpty()) return name;
        }

        return null;
    }

    @Nullable
    public static <T> String getContinuationString(Continuation<T> continuation) {
        String contString = continuation.toString();

        /*
        * Replace instances with a more readable name
        * */
        if(contString.equals(CREATE_METHOD_1) || contString.equals(CREATE_METHOD_2)) {
            return sub;
        }

        /*
        * if starts with Continuation at use the string
        * */
        if(contString.startsWith(CONT_LOC)) {
            return contString;
        }

        /*
        * If it is a coroutine use its name if available
        * */
        if(continuation instanceof AbstractCoroutine_Instrumentation) {
            return ((AbstractCoroutine_Instrumentation<?>)continuation).nameString$kotlinx_coroutines_core();
        }
        
        int index = contString.indexOf('@');
        if(index > -1) {
            return contString.substring(0, index);
        }
        
        return null;
    }

    @Override
    public void configureContinuationIgnores(String[] ignores) {
        ignoredContinuations.clear();
        if (ignores != null) {
            for (String ignore : ignores) {
                if(!ignoredContinuations.contains(ignore)) {
                    ignoredContinuations.add(ignore);
                }
            }
        }
    }

    @Override
    public void configureScopeIgnores(String[] ignores) {
        ignoredScopes.clear();
        if (ignores != null) {
            for (String ignore : ignores) {
                if(!ignoredScopes.contains(ignore)) {
                    ignoredScopes.add(ignore);
                }
            }
        }
    }

    @Override
    public void configureDispatchedTasksIgnores(String[] ignores) {
        DispatchedTaskIgnores.reset();
        if (ignores != null) {
            for (String ignore : ignores) {
                DispatchedTaskIgnores.addIgnore(ignore);
            }
        }
    }

    @Override
    public void configureDelay(boolean enabled) {
        DELAYED_ENABLED = enabled;
    }
}