package com.newrelic.agent.kotlincoroutines;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.KotlinCoroutinesConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

import java.util.LinkedHashSet;
import java.util.Set;

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
        for (CoroutineConfigListener listener : listeners) {
            listener.configureDelay(coroutinesConfig.isDelayedEnabled());
            listener.configureScopeIgnores(coroutinesConfig.getIgnoredScopes(),coroutinesConfig.getIgnoredRegexScopes());
            listener.configureDispatchedTasksIgnores(coroutinesConfig.getIgnoredDispatched(),coroutinesConfig.getIgnoredRegexDispatched());
            listener.configureContinuationIgnores(coroutinesConfig.getIgnoredContinuations(),coroutinesConfig.getIgnoredRegExContinuations());
        }
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
        for (CoroutineConfigListener listener : listeners) {
            listener.configureDelay(coroutinesConfig.isDelayedEnabled());
            listener.configureScopeIgnores(coroutinesConfig.getIgnoredScopes(),coroutinesConfig.getIgnoredRegexScopes());
            listener.configureDispatchedTasksIgnores(coroutinesConfig.getIgnoredDispatched(),coroutinesConfig.getIgnoredRegexDispatched());
            listener.configureContinuationIgnores(coroutinesConfig.getIgnoredContinuations(),coroutinesConfig.getIgnoredRegExContinuations());
        }
        for(SuspendsConfigListener listener : suspendListeners) {
            listener.configureSuspendsIgnores(coroutinesConfig.getIgnoredSuspends(),coroutinesConfig.getIgnoredRegexSuspends());
        }

    }
}