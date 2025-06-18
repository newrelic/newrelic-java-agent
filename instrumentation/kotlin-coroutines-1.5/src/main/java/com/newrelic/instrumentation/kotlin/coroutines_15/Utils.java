package com.newrelic.instrumentation.kotlin.coroutines_15;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.jvm.internal.BaseContinuationImpl;
import kotlinx.coroutines.AbstractCoroutine_Instrumentation;
import kotlinx.coroutines.CoroutineName;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.DispatchedTask;

public class Utils implements AgentConfigListener {

    private static final List<String> ignoredContinuations = new ArrayList<String>();
    private static final List<String> ignoredScopes = new ArrayList<>();
    private static final String CONT_IGNORE_CONFIG = "Coroutines.ignores.continuations";
    private static final String SCOPES_IGNORE_CONFIG = "Coroutines.ignores.scopes";
    private static final String DISPATCHED_IGNORE_CONFIG = "Coroutines.ignores.dispatched";
    private static final String DELAYED_ENABLED_CONFIG = "Coroutines.delayed.enabled";
    
    public static final String CREATEMETHOD1 = "Continuation at kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$IntrinsicsKt__IntrinsicsJvmKt$4";
    public static final String CREATEMETHOD2 = "Continuation at kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$IntrinsicsKt__IntrinsicsJvmKt$3";
    private static final Utils INSTANCE = new Utils();
    private static final String CONT_LOC = "Continuation at";
    public static boolean DELAYED_ENABLED = true;

    static {
        ServiceFactory.getConfigService().addIAgentConfigListener(INSTANCE);
        Config config = NewRelic.getAgent().getConfig();
        loadConfig(config);
        ignoredContinuations.add(CREATEMETHOD1);
        ignoredContinuations.add(CREATEMETHOD2);
        Object value = config.getValue(DELAYED_ENABLED_CONFIG);
        if(value != null) {
            if(value instanceof Boolean) {
                DELAYED_ENABLED = (Boolean)value;
            } else {
                DELAYED_ENABLED = Boolean.valueOf(value.toString());
            }
        }
        
    }
    
    public static NRRunnable getRunnableWrapper(Runnable r) {
        if(r instanceof NRRunnable) {
            return null;
        }
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
    
    private static void loadConfig(Config config) {
        String ignores = config.getValue(CONT_IGNORE_CONFIG);
        NewRelic.getAgent().getLogger().log(Level.FINE, "Value of {0}: {1}", CONT_IGNORE_CONFIG, ignores);
        if (ignores != null && !ignores.isEmpty()) {
            ignoredContinuations.clear();
            String[] ignoresList = ignores.split(",");
            
            for(String ignore : ignoresList) {
                if (!ignoredContinuations.contains(ignore)) {
                    ignoredContinuations.add(ignore);
                    NewRelic.getAgent().getLogger().log(Level.FINE, "Will ignore Continuations named {0}", ignore);
                }
            }
        } else if(!ignoredContinuations.isEmpty()) {
            ignoredContinuations.clear();
        }
        ignores = config.getValue(DISPATCHED_IGNORE_CONFIG);
        NewRelic.getAgent().getLogger().log(Level.FINE, "Value of {0}: {1}", DISPATCHED_IGNORE_CONFIG, ignores);
        DispatchedTaskIgnores.reset();
        if (ignores != null && !ignores.isEmpty()) {
            DispatchedTaskIgnores.configure(ignores);
        }
        ignores = config.getValue(SCOPES_IGNORE_CONFIG);
        if (ignores != null && !ignores.isEmpty()) {
            ignoredScopes.clear();
            String[] ignoresList = ignores.split(",");
            
            for(String ignore : ignoresList) {
                if (!ignoredScopes.contains(ignore)) {
                    ignoredScopes.add(ignore);
                    NewRelic.getAgent().getLogger().log(Level.FINE, "Will ignore CoroutineScopes named {0}", ignore);
                }
            }
        } else if(!ignoredScopes.isEmpty()) {
            ignoredScopes.clear();
        }
        
    }
    
    public static boolean ignoreScope(CoroutineScope scope) {
        CoroutineContext ctx = scope.getCoroutineContext();
        String name = getCoroutineName(ctx);
        String className = scope.getClass().getName();
        return ignoreScope(className) || ignoreScope(name);
    }
    
    public static boolean ignoreScope(String coroutineScope) {
        return ignoredScopes.contains(coroutineScope);
    }
    
    public static boolean ignoreContinuation(String cont_string) {
        return ignoredContinuations.contains(cont_string);
    }
    
    public static String sub = "createCoroutineFromSuspendFunction";

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
            }
        }
        return null;
    }

    public static Token getToken(CoroutineContext context) {
        NRCoroutineToken coroutineToken = context.get(NRCoroutineToken.key);
        Token token = null;
        if(coroutineToken != null) {
            token = coroutineToken.getToken();
        }

        return token;
    }

    public static void expireToken(CoroutineContext context) {
        NRCoroutineToken coroutineToken = context.get(NRCoroutineToken.key);
        if(coroutineToken != null) {
            Token token = coroutineToken.getToken();
            token.expire();
            context.minusKey(NRCoroutineToken.key);
        }
    }
    
    @SuppressWarnings("unchecked")
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

    @Override
    public void configChanged(String appName, AgentConfig agentConfig) {
        loadConfig(agentConfig);
        Object value = agentConfig.getValue(DELAYED_ENABLED_CONFIG);
        if(value != null) {
            if(value instanceof Boolean) {
                DELAYED_ENABLED = (Boolean)value;
            } else {
                DELAYED_ENABLED = Boolean.parseBoolean(value.toString());
            }
        }
    }
    
    public static <T> String getContinuationString(Continuation<T> continuation) {
        String contString = continuation.toString();
        
        if(contString.equals(CREATEMETHOD1) || contString.equals(CREATEMETHOD2)) {
            return sub;
        }
        
        if(contString.startsWith(CONT_LOC)) {
            return contString;
        }
        
        if(continuation instanceof AbstractCoroutine_Instrumentation) {
            return ((AbstractCoroutine_Instrumentation<?>)continuation).nameString$kotlinx_coroutines_core();
        }
        
        int index = contString.indexOf('@');
        if(index > -1) {
            return contString.substring(0, index);
        }
        
        return null;
    }
}