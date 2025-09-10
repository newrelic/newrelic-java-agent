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
    private KotlinCoroutinesConfig coroutinesConfig;

    public KotlinCoroutinesService(KotlinCoroutinesConfig coroutinesConfig) {
        super("KotlinCoroutinesService");
        this.coroutinesConfig = coroutinesConfig;
    }

    public void addCoroutineConfigListener(CoroutineConfigListener listener) {
        if(listener != null) {
            listeners.add(listener);
            listener.configureContinuationIgnores(coroutinesConfig.getIgnoredContinuations());
            listener.configureScopeIgnores(coroutinesConfig.getIgnoredScopes());
            listener.configureDispatchedTasksIgnores(coroutinesConfig.getIgnoredDispatched());
            listener.configureDelay(coroutinesConfig.isDelayedEnabled());
        }
    }

    @Override
    protected void doStart() throws Exception {
        for (CoroutineConfigListener listener : listeners) {
            listener.configureDelay(coroutinesConfig.isDelayedEnabled());
            listener.configureScopeIgnores(coroutinesConfig.getIgnoredScopes());
            listener.configureDispatchedTasksIgnores(coroutinesConfig.getIgnoredDispatched());
            listener.configureContinuationIgnores(coroutinesConfig.getIgnoredContinuations());
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
            listener.configureScopeIgnores(coroutinesConfig.getIgnoredScopes());
            listener.configureDispatchedTasksIgnores(coroutinesConfig.getIgnoredDispatched());
            listener.configureContinuationIgnores(coroutinesConfig.getIgnoredContinuations());
        }

    }
}
