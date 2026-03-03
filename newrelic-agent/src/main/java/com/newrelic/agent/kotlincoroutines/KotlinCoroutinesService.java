package com.newrelic.agent.kotlincoroutines;

import com.newrelic.agent.config.*;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

import java.util.*;

public class KotlinCoroutinesService extends AbstractService implements AgentConfigListener {

    private final Set<CoroutineConfigListener> listeners = new LinkedHashSet<>();
    private final Set<SuspendsConfigListener> suspendListeners = new LinkedHashSet<>();
    private KotlinCoroutinesConfig coroutinesConfig;

    public KotlinCoroutinesService(KotlinCoroutinesConfig coroutinesConfig) {
        super("KotlinCoroutinesService");
        this.coroutinesConfig = coroutinesConfig;
        ServiceFactory.getConfigService().addIAgentConfigListener(this);
    }

    public void addCoroutineConfigListener(CoroutineConfigListener listener) {
        if(listener != null) {
            listeners.add(listener);
            listener.configureContinuationIgnores(coroutinesConfig.getIgnoredContinuations(),coroutinesConfig.getIgnoredRegExContinuations());
            listener.configureScopeIgnores(coroutinesConfig.getIgnoredScopes(), coroutinesConfig.getIgnoredRegexScopes());
            listener.configureDispatchedTasksIgnores(coroutinesConfig.getIgnoredDispatched(), coroutinesConfig.getIgnoredRegexDispatched());
            listener.configureDelay(coroutinesConfig.isDelayedEnabled());
        }
    }

    public void addSuspendsConfigListener(SuspendsConfigListener listener) {
        if(listener != null) {
            suspendListeners.add(listener);
            listener.configureSuspendsIgnores(coroutinesConfig.getIgnoredSuspends(),coroutinesConfig.getIgnoredRegexSuspends());
        }
    }

    @Override
    protected void doStart() throws Exception {
        reconfigure();
    }

    @Override
    protected void doStop() throws Exception {
        ConfigService configService = ServiceFactory.getConfigService();
        configService.removeIAgentConfigListener(this);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void configChanged(String appName, AgentConfig agentConfig) {
        coroutinesConfig = agentConfig.getKotlinCoroutinesConfig();
        reconfigure();
    }

    /**
     * Used to reconfigure the ignores after some have been added.  The adding should only happen once
     * rather than every time something is added.   User should add all the necessary and then reconfigure
     */
    public void reconfigure() {
        Set<String> ignoredContinuations = KotlinIgnoresCache.getIgnoredContinuations();
        Set<String> ignoredRegexContinuations = KotlinIgnoresCache.getIgnoredRegexContinuations();
        Set<String> ignoredScopes = KotlinIgnoresCache.getIgnoredScopes();
        Set<String> ignoredRegexScopes = KotlinIgnoresCache.getIgnoredRegexScopes();
        Set<String> ignoredDispatched = KotlinIgnoresCache.getIgnoredDispatched();
        Set<String> ignoredRegexDispatched = KotlinIgnoresCache.getIgnoredRegexDispatched();
        Set<String> ignoredSuspends = KotlinIgnoresCache.getIgnoredSuspends();
        Set<String> ignoredRegexSuspends = KotlinIgnoresCache.getIgnoredRegexSuspends();

        String[] allIgnoredContinuations = merge(coroutinesConfig.getIgnoredContinuations(), ignoredContinuations);
        String[] allIgnoredScopes = merge(coroutinesConfig.getIgnoredScopes(), ignoredScopes);
        String[] allIgnoredDispatched = merge(coroutinesConfig.getIgnoredDispatched(), ignoredDispatched);
        String[] allIgnoredSuspends = merge(coroutinesConfig.getIgnoredSuspends(), ignoredSuspends);

        String[] allIgnoredRegexContinuations = merge(coroutinesConfig.getIgnoredRegExContinuations(), ignoredRegexContinuations);
        String[] allIgnoredRegexScopes = merge(coroutinesConfig.getIgnoredRegexScopes(), ignoredRegexScopes);
        String[] allIgnoredRegexDispatched = merge(coroutinesConfig.getIgnoredRegexDispatched(), ignoredRegexDispatched);
        String[] allIgnoredRegexSuspends = merge(coroutinesConfig.getIgnoredRegexSuspends(), ignoredRegexSuspends);

        for (CoroutineConfigListener listener : listeners) {
            listener.configureDelay(coroutinesConfig.isDelayedEnabled());
            listener.configureScopeIgnores(allIgnoredScopes,allIgnoredRegexScopes);
            listener.configureDispatchedTasksIgnores(allIgnoredDispatched,allIgnoredRegexDispatched);
            listener.configureContinuationIgnores(allIgnoredContinuations,allIgnoredRegexContinuations);
        }
        for(SuspendsConfigListener listener : suspendListeners) {
            listener.configureSuspendsIgnores(allIgnoredSuspends,allIgnoredRegexSuspends);
        }
    }

    private static String[] merge(String[] array, Collection<String> collection) {
        Set<String> set = new HashSet<>(Arrays.asList(array));
        set.addAll(collection);
        String[] result = new String[set.size()];
        return set.toArray(result);
    }

    /*
     * The following set of methods allow an instrumentation developer to add components
     * to ignore automatically rather than relying on the user to include them in newrelic.yml
     */
    public void addIgnoredContinuation(String continuation) {
        KotlinIgnoresCache.addIgnoredContinuation(continuation);
    }

    public void addIgnoredRegExContinuation(String continuationRegEx) {
        KotlinIgnoresCache.addIgnoredRegexContinuation(continuationRegEx);
    }

    public void addIgnoredScope(String scope) {
        KotlinIgnoresCache.addIgnoredScope(scope);
    }

    public void addIgnoredRegexScope(String scopeRegEx) {
        KotlinIgnoresCache.addIgnoredRegexScope(scopeRegEx);
    }

    public void addIgnoredDispatched(String dispatched) {
        KotlinIgnoresCache.addIgnoredDispatched(dispatched);
    }

    public void addIgnoredRegexDispatched(String dispatchedRegEx) {
        KotlinIgnoresCache.addIgnoredRegexDispatched(dispatchedRegEx);
    }

    public void addIgnoredSuspends(String suspend) {
        KotlinIgnoresCache.addIgnoredSuspend(suspend);
    }

    public void addIgnoredRegexSuspends(String suspendRegEx) {
        KotlinIgnoresCache.addIgnoredRegexSuspend(suspendRegEx);
    }

}
