package com.newrelic.instrumentation.kotlin.coroutines_17;

import com.newrelic.agent.kotlincoroutines.CoroutineConfigListener;
import com.newrelic.agent.kotlincoroutines.KotlinCoroutinesService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.jvm.internal.BaseContinuationImpl;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.DispatchedTask;
import kotlinx.coroutines.AbstractCoroutine_Instrumentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class Utils implements CoroutineConfigListener {

	private static final List<String> ignoredContinuations = new ArrayList<>();
	private static final List<Pattern> ignoredContinuationPatterns = new ArrayList<>();
	private static final List<String> ignoredScopes = new ArrayList<>();
	private static final List<Pattern> ignoredScopePatterns = new ArrayList<>();

	public static final String CREATE_METHOD_1 = "Continuation at kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$IntrinsicsKt__IntrinsicsJvmKt$4";
	public static final String CREATE_METHOD_2 = "Continuation at kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$IntrinsicsKt__IntrinsicsJvmKt$3";
	private static final String CONT_LOC = "Continuation at";
	public static boolean DELAYED_ENABLED = true;

	static {
		/*
		 * Register this class with the KotlinCoroutinesService to initialize and update
		 * the ignored items
		 */
		KotlinCoroutinesService service = ServiceFactory.getKotlinCoroutinesService();
		service.addCoroutineConfigListener(new Utils());
		ignoredContinuations.add(CREATE_METHOD_1);
		ignoredContinuations.add(CREATE_METHOD_2);

	}

	/*
	 * Returns a Runnable wrapper to track Runnable tasks that
	 * are being passed to another thread
	 */
	public static NRRunnable getRunnableWrapper(Runnable r) {
		if(r instanceof NRRunnable) {
			return null;
		}
		if(r instanceof DispatchedTask) {
			DispatchedTask<?> task = (DispatchedTask<?>)r;
			Continuation<?> cont = task.getDelegate$kotlinx_coroutines_core();
			String cont_string = getContinuationString(cont);
			if(cont_string == null || DispatchedTaskIgnores.ignoreDispatchedTask(cont_string)) {
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
	 * Allows certain Coroutine scopes to be ignored
	 * coroutineScope can be a Coroutine name or CoroutineScope class name
	 */
	public static boolean continueWithScope(CoroutineScope scope) {
		CoroutineContext ctx = scope.getCoroutineContext();
		String name = getCoroutineName(ctx);
		String className = scope.getClass().getName();
		return continueWithScope(className) && continueWithScope(name);
	}

	/*
	 * Allows certain Coroutine scopes to be ignored
	 * coroutineScope can be a Coroutine name or CoroutineScope class name
	 */
	public static boolean continueWithScope(String coroutineScope) {
		for(Pattern ignoredScope : ignoredScopePatterns) {
			if(ignoredScope.matcher(coroutineScope).matches()) {
				return false;
			}
		}
		return !ignoredScopes.contains(coroutineScope);
	}

	public static boolean continueWithContinuation(Continuation<?> continuation) {
		/*
		 *	Don't trace internal Coroutines Continuations
		 */
		String className = continuation.getClass().getName();
		if(className.startsWith("kotlin")) return false;

		/*
		 * Get the continuation string and check if it should be ignored
		 */
		String cont_string = getContinuationString(continuation);
		if(cont_string == null) { return false; }

		if(ignoredContinuations.contains(cont_string)) {
			NewRelic.getAgent().getLogger().log(Level.FINE, "Returning false for continuation {0}", cont_string);
			return false;
		}

		for(Pattern pattern : ignoredContinuationPatterns) {
			if(pattern.matcher(cont_string).matches()) {
				return false;
			}
		}

		NewRelic.getAgent().getLogger().log(Level.FINE, "Returning true for continuation {0}", cont_string);
		return true;
	}

	public static String sub = "createCoroutineFromSuspendFunction";

	/*
	 * Set the async token in the CoroutineContext
	 * Used to track the transaction across multiple threads
	 */
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

	/*
	 * Gets the async token in the CoroutineContext if it is set
	 */
	public static Token getToken(CoroutineContext context) {
		TokenContext tokenContext = NRTokenContextKt.getTokenContextOrNull(context);
		if(tokenContext != null) {
			return tokenContext.getToken();
		}
		return null;
	}

	/*
	 * Expires the async token in the CoroutineContext if it is set and
	 * removes the tokencontext from the CoroutineContext
	 */
	public static void expireToken(CoroutineContext context) {
		TokenContext tokenContext = NRTokenContextKt.getTokenContextOrNull(context);
		if(tokenContext != null) {
			Token token = tokenContext.getToken();
			token.expire();
			NRTokenContextKt.removeTokenContext(context);
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
	public void configureContinuationIgnores(String[] ignores, String[] ignoresRegExs) {
		ignoredContinuations.clear();
		ignoredContinuationPatterns.clear();

		if(ignores != null) {
			ignoredContinuations.addAll(Arrays.asList(ignores));
			for(String ignore : ignoredContinuations) {
				NewRelic.getAgent().getLogger().log(Level.FINER,"Will ignore these continuation: {0}", ignore);
			}
		}
		if(ignoresRegExs != null) {
			for(String ignore : ignoresRegExs) {
				ignoredContinuationPatterns.add(Pattern.compile(ignore));
				NewRelic.getAgent().getLogger().log(Level.FINER,"Will ignore these continuations matching regex: {0}", ignore);
			}
		}
	}

	@Override
	public void configureScopeIgnores(String[] ignores, String[] ignoresRegExs) {
		ignoredScopes.clear();
		ignoredScopePatterns.clear();

		if (ignores != null) {
			ignoredScopes.addAll(Arrays.asList(ignores));
			for(String ignore : ignoredScopes) {
				NewRelic.getAgent().getLogger().log(Level.FINER,"Will ignore these Scope: {0}", ignore);
			}
		}

		if(ignoresRegExs != null) {
			for(String ignore : ignoresRegExs) {
				ignoredScopePatterns.add(Pattern.compile(ignore));
				NewRelic.getAgent().getLogger().log(Level.FINER,"Will ignore these scope matching regex: {0}", ignore);
			}
		}
	}

	@Override
	public void configureDispatchedTasksIgnores(String[] ignores, String[] ignoresRegExs) {
		DispatchedTaskIgnores.reset();

		if(ignores != null) {
			DispatchedTaskIgnores.addIgnoredTasks(Arrays.asList(ignores), Arrays.asList(ignoresRegExs));
		}
	}

	@Override
	public void configureDelay(boolean enabled) {
		DELAYED_ENABLED = enabled;
	}
}
