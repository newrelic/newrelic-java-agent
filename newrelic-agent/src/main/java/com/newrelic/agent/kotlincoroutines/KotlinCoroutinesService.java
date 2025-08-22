package com.newrelic.agent.kotlincoroutines;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

import java.util.LinkedHashSet;
import java.util.Set;

public class KotlinCoroutinesService extends AbstractService implements AgentConfigListener {

    private static final String COROUTINES_IGNORES_CONTINUATIONS = "Coroutines.ignores.continuations";
    private static final String COROUTINES_IGNORES_SCOPES = "Coroutines.ignores.scopes";
    private static final String COROUTINES_IGNORES_DISPATCHED = "Coroutines.ignores.dispatched";
    private static final String DELAYED_ENABLED_CONFIG = "Coroutines.delayed.enabled";
    private final Set<CoroutineConfigListener> listeners = new LinkedHashSet<>();
    private String[] ignoredContinuations = null;
    private String[] ignoredScopes = null;
    private String[] ignoredDispatched = null;

    public KotlinCoroutinesService() {
        super("KotlinCoroutinesService");
    }

    public void addCoroutineConfigListener(CoroutineConfigListener listener) {
        if(listener != null) {
            listeners.add(listener);
            listener.configureContinuationIgnores(ignoredContinuations);
            listener.configureScopeIgnores(ignoredScopes);
            listener.configureDispatchedTasksIgnores(ignoredDispatched);
        }
    }

    @Override
    protected void doStart() throws Exception {
        ConfigService configService = ServiceFactory.getConfigService();
        configService.addIAgentConfigListener(this);

        AgentConfig localAgentConfig = configService.getLocalAgentConfig();
        loadConfig(localAgentConfig);
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
        loadConfig(agentConfig);
    }

    private void loadConfig(AgentConfig agentConfig) {
        String ignoredContinuationStr = agentConfig.getValue(COROUTINES_IGNORES_CONTINUATIONS);
        ignoredContinuations = ignoredContinuationStr != null ? ignoredContinuationStr.split(",") : null;

        String ignoredScopeStr = agentConfig.getValue(COROUTINES_IGNORES_SCOPES);
        ignoredScopes = ignoredScopeStr != null ? ignoredScopeStr.split(",") : null;

        String ignoredDispatchedStr = agentConfig.getValue(COROUTINES_IGNORES_DISPATCHED);
        ignoredDispatched = ignoredDispatchedStr != null ? ignoredDispatchedStr.split(",") : null;

        boolean delayedEnabled = agentConfig.getValue(DELAYED_ENABLED_CONFIG, Boolean.FALSE);

        for (CoroutineConfigListener listener : listeners) {
            listener.configureContinuationIgnores(ignoredContinuations);
            listener.configureScopeIgnores(ignoredScopes);
            listener.configureDispatchedTasksIgnores(ignoredDispatched);
            listener.configureDelay(delayedEnabled);
        }


    }
}
