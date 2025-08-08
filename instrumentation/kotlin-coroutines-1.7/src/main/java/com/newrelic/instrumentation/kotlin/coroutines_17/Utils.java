package com.newrelic.instrumentation.kotlin.coroutines_17;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.newrelic.agent.kotlincoroutines.CoroutineConfigListener;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.jvm.internal.BaseContinuationImpl;
import kotlinx.coroutines.AbstractCoroutine_Instrumentation;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.DispatchedTask;

public class Utils implements CoroutineConfigListener {

	private static final List<String> ignoredContinuations = new ArrayList<>();
	private static final List<String> ignoredScopes = new ArrayList<>();

	public static final String CREATE_METHOD_1 = "Continuation at kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$IntrinsicsKt__IntrinsicsJvmKt$4";
	public static final String CREATE_METHOD_2 = "Continuation at kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$IntrinsicsKt__IntrinsicsJvmKt$3";
	private static final Utils INSTANCE = new Utils();
	private static final String CONT_LOC = "Continuation at";
	public static boolean DELAYED_ENABLED = true;

	static {
		ServiceFactory.getKotlinCoroutinesService().addCoroutineConfigListener(INSTANCE);
		ignoredContinuations.add(CREATE_METHOD_1);
		ignoredContinuations.add(CREATE_METHOD_2);
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
	
	public static boolean continueWithScope(CoroutineScope scope) {
		CoroutineContext ctx = scope.getCoroutineContext();
		String name = getCoroutineName(ctx);
		String className = scope.getClass().getName();
		return continueWithScope(className) && continueWithScope(name);
	}
	
	public static boolean continueWithScope(String coroutineScope) {
		return !ignoredScopes.contains(coroutineScope);
	}
	
	public static boolean continueWithContinuation(String cont_string) {
		return !ignoredContinuations.contains(cont_string);
	}
	
	public static String sub = "createCoroutineFromSuspendFunction";

	public static void setToken(CoroutineContext context) {
		TokenContext tokenContext = NRTokenContextKt.getTokenContextOrNull(context);
		if (tokenContext == null) {
			Token t = NewRelic.getAgent().getTransaction().getToken();
			if(t != null && t.isActive()) {
				NRTokenContextKt.addTokenContext(context, t);
			} else if(t != null) {
				t.expire();
				t = null;
			}
		}
	}

	public static Token getToken(CoroutineContext context) {
		TokenContext tokenContext = NRTokenContextKt.getTokenContextOrNull(context);
		if(tokenContext != null) {
			return tokenContext.getToken();
		}
		return null;
	}

	public static void expireToken(CoroutineContext context) {
		TokenContext tokenContext = NRTokenContextKt.getTokenContextOrNull(context);
		if(tokenContext != null) {
			Token token = tokenContext.getToken();
			token.expire();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> String getCoroutineName(CoroutineContext context, Continuation<T> continuation) {
		if(continuation instanceof AbstractCoroutine_Instrumentation) {
			return ((AbstractCoroutine_Instrumentation<T>)continuation).nameString$kotlinx_coroutines_core();
		}
		String name = getCoroutineName(context);
		if(name != null) {
			return name;
		}
		if(continuation instanceof BaseContinuationImpl) {
			return ((BaseContinuationImpl)continuation).toString();
		}
		return null;
	}

	public static String getCoroutineName(CoroutineContext context) {
		return CoroutineNameUtilsKt.getCoroutineName(context);
	}

	public static <T> String getContinuationString(Continuation<T> continuation) {
		String contString = continuation.toString();
		
		if(contString.equals(CREATE_METHOD_1) || contString.equals(CREATE_METHOD_2)) {
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

	@Override
	public void configureContinuationIgnores(String[] ignores) {
		ignoredContinuations.clear();
		ignoredContinuations.addAll(Arrays.asList(ignores));
	}

	@Override
	public void configureScopeIgnores(String[] ignores) {
		ignoredScopes.clear();
		ignoredScopes.addAll(Arrays.asList(ignores));
	}

	@Override
	public void configureDispatchedTasksIgnores(String[] ignores) {
		DispatchedTaskIgnores.reset();
		DispatchedTaskIgnores.addIgnoredTasks(Arrays.asList(ignores));
	}

	@Override
	public void configureDelay(boolean enabled) {
		DELAYED_ENABLED = enabled;
	}
}
