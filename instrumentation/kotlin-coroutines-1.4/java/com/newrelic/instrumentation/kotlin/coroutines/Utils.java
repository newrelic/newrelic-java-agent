package com.newrelic.instrumentation.kotlin.coroutines;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;

import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.CoroutineName;

public class Utils implements AgentConfigListener {

	private static final List<String> ignoredSuspends = new ArrayList<String>();
	private static final List<String> ignoredContinuations = new ArrayList<String>();
	private static final List<String> ignoredDispatchs = new ArrayList<String>();
	private static final String SUSPENDSIGNORECONFIG = "Coroutines.ignores.suspends";
	private static final String CONTIGNORECONFIG = "Coroutines.ignores.continuations";
	private static final String DISPATCHEDIGNORECONFIG = "Coroutines.ignores.dispatched";

	private static final Utils INSTANCE = new Utils();

	static {
		ServiceFactory.getConfigService().addIAgentConfigListener(INSTANCE);
		Config config = NewRelic.getAgent().getConfig();
		String ignores = config.getValue(SUSPENDSIGNORECONFIG);
		if (ignores != null && !ignores.isEmpty()) {
			StringTokenizer st = new StringTokenizer(ignores, ",");
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token != null && !token.isEmpty()) {
					NewRelic.getAgent().getLogger().log(Level.INFO, "will ignore suspend: {0}", token);
					ignoredSuspends.add(token);
				}
			} 
		}
		ignores = config.getValue(CONTIGNORECONFIG);
		if (ignores != null && !ignores.isEmpty()) {
			StringTokenizer st = new StringTokenizer(ignores, ",");
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token != null && !token.isEmpty()) {
					NewRelic.getAgent().getLogger().log(Level.INFO, "will ignore continuation: {0}", token);
					ignoredContinuations.add(token);
				}
			} 
		}
		ignores = config.getValue(DISPATCHEDIGNORECONFIG);
		if (ignores != null && !ignores.isEmpty()) {
			StringTokenizer st = new StringTokenizer(ignores, ",");
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token != null && !token.isEmpty()) {
					NewRelic.getAgent().getLogger().log(Level.INFO, "will ignore dispatched continuation: {0}", token);
					ignoredDispatchs.add(token);
				}
			} 
		}
		
	}
	
	public static boolean ignoreContinuation(String name) {
		return ignoredContinuations.contains(name);
	}
	
	public static boolean ignoreContinuation(Class<?> continuation, CoroutineContext context) {
		
		String classname = continuation.getName();
		if(ignoredContinuations.contains(classname)) return true;
		
		if(context == null) return false;
		
		String name = getCoroutineName(context);
		
		if(ignoredContinuations.contains(name)) return true;
		
		return false;
	}
	
	public static boolean ignoreDispatched(Class<?> dispatched, CoroutineContext context) {
		String classname = dispatched.getName();
		if(ignoredDispatchs.contains(classname)) return true;
		
		if(context == null) return false;
		
		String name = getCoroutineName(context);
		
		if(ignoredDispatchs.contains(name)) return true;
		
		return false;
	}

	public static boolean ignoreSuspend(Class<?> suspend, CoroutineContext context) {

		String classname = suspend.getName();

		if(ignoredSuspends.contains(classname)) return true;

		if(context == null) return false;

		String name = getCoroutineName(context);

		if(ignoredSuspends.contains(name)) {
			return true;
		}

		return false;
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

	public static String getCoroutineName(CoroutineContext context, Class<?> clazz) {
		String name = getCoroutineName(context);
		if(name != null) {
			return name;
		}
		return clazz.getSimpleName();
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
		// TODO Auto-generated method stub

	}
}
