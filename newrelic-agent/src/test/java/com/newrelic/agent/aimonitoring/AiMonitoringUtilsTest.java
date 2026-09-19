package com.newrelic.agent.aimonitoring;

import com.newrelic.agent.Agent;
import com.newrelic.agent.AgentImpl;
import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.aimonitoring.AiMonitoringUtils;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.MetricAggregator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static com.newrelic.agent.bridge.aimonitoring.AiMonitoringUtils.COLLECT_AI;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AiMonitoringUtilsTest {

    private static com.newrelic.agent.bridge.Agent savedAgent;

    @BeforeClass
    public static void setup() {
        savedAgent = AgentBridge.getAgent();
    }

    @AfterClass
    public static void teardown() {
        AgentBridge.agent = savedAgent;
    }

    private AgentConfig buildConfig(Map<String, Object> localSettings, Map<String, Object> serverData) {
        return AgentConfigFactory.createAgentConfig(localSettings, serverData, null);
    }

    private Map<String, Object> baseLocalSettings() {
        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(AgentConfigImpl.APP_NAME, "Unit Testing");
        localSettings.put(AgentConfigImpl.HOST, "nope.example.invalid");
        return localSettings;
    }

    private void installConfig(AgentConfig agentConfig) {
        ServiceFactory.setServiceManager(new MockServiceManager(new MockConfigService(agentConfig)));
        AgentBridge.agent = new AgentImpl(Agent.LOG);
    }

    private Map<String, Object> withAgentConfig(Object aiMonitoringEnabledValue) {
        Map<String, Object> serverData = new HashMap<>();
        Map<String, Object> agentConfig = new HashMap<>();
        agentConfig.put("ai_monitoring.enabled", aiMonitoringEnabledValue);
        serverData.put(AgentConfigFactory.AGENT_CONFIG, agentConfig);
        return serverData;
    }

    @Test
    public void isAiMonitoringEnabled_serverSideTrue_collectAiFalse_resolveFalse() {
        Map<String, Object> serverData = withAgentConfig(true);
        serverData.put(COLLECT_AI, false);

        AgentConfig agentConfig = buildConfig(baseLocalSettings(), serverData);
        installConfig(agentConfig);

        assertFalse(AiMonitoringUtils.isAiMonitoringEnabled());
    }

    @Test
    public void isAiMonitoringEnabled_serverSideFalse_collectAiTrue_resolvesFalse() {
        Map<String, Object> serverData = withAgentConfig(false);
        serverData.put(COLLECT_AI, true);

        AgentConfig agentConfig = buildConfig(baseLocalSettings(), serverData);
        installConfig(agentConfig);

        assertFalse(AiMonitoringUtils.isAiMonitoringEnabled());
    }

    @Test
    public void isAiMonitoringEnabled_noServerSideOverride_collectAiTrue_resolvesTrue() {
        Map<String, Object> serverData = withAgentConfig(null);
        Map<String, Object> localSettings = baseLocalSettings();
        serverData.put(COLLECT_AI, true);
        localSettings.put("ai_monitoring.enabled", true);

        AgentConfig agentConfig = buildConfig(localSettings, serverData);
        installConfig(agentConfig);

        assertTrue(AiMonitoringUtils.isAiMonitoringEnabled());
    }

    @Test
    public void isAiMonitoringEnabled_noServerSideOverride_collectAiFalse_resolvesFalse() {
        Map<String, Object> serverData = withAgentConfig(null);
        Map<String, Object> localSettings = baseLocalSettings();
        serverData.put(COLLECT_AI, false);
        localSettings.put("ai_monitoring.enabled", true);

        AgentConfig agentConfig = buildConfig(localSettings, serverData);
        installConfig(agentConfig);

        assertFalse(AiMonitoringUtils.isAiMonitoringEnabled());
    }

    @Test
    public void isAiMonitoringEnabled_serverSideTrue_noCollectAi_resolvesTrue() {
        Map<String, Object> serverData = withAgentConfig(true);
        AgentConfig agentConfig = buildConfig(baseLocalSettings(), serverData);
        installConfig(agentConfig);

        assertTrue(AiMonitoringUtils.isAiMonitoringEnabled());
    }

    @Test
    public void isAiMonitoringEnabled_noServerSideOverride_noCollectAi_localSettingTrue_resolvesTrue() {
        Map<String, Object> serverData = withAgentConfig(null);
        Map<String, Object> localSettings = baseLocalSettings();
        localSettings.put("ai_monitoring.enabled", true);

        AgentConfig agentConfig = buildConfig(localSettings, serverData);
        installConfig(agentConfig);

        assertTrue(AiMonitoringUtils.isAiMonitoringEnabled());
    }

    @Test
    public void isAiMonitoringEnabled_noServerSideOverride_noCollectAi_localSettingFalse_resolvesFalse() {
        Map<String, Object> serverData = withAgentConfig(null);
        Map<String, Object> localSettings = baseLocalSettings();
        localSettings.put("ai_monitoring.enabled", false);

        AgentConfig agentConfig = buildConfig(localSettings, serverData);
        installConfig(agentConfig);

        assertFalse(AiMonitoringUtils.isAiMonitoringEnabled());
    }

    @Test
    public void isAiMonitoringEnabled_hsmEnabled_serverSideTrue_resolvesFalse() {
        Map<String, Object> serverData = withAgentConfig(true);
        Map<String, Object> localSettings = baseLocalSettings();
        localSettings.put(AgentConfigImpl.HIGH_SECURITY, true);

        AgentConfig agentConfig = buildConfig(localSettings, serverData);
        installConfig(agentConfig);

        assertFalse(AiMonitoringUtils.isAiMonitoringEnabled());
    }

    @Test
    public void isAiMonitoringEnabled_hsmEnabled_collectAiTrue_localSettingTrue_resolvesFalse() {
        Map<String, Object> serverData = withAgentConfig(null);
        Map<String, Object> localSettings = baseLocalSettings();
        serverData.put(COLLECT_AI, true);
        localSettings.put(AgentConfigImpl.HIGH_SECURITY, true);
        localSettings.put("ai_monitoring.enabled", true);

        AgentConfig agentConfig = buildConfig(localSettings, serverData);
        installConfig(agentConfig);

        assertFalse(AiMonitoringUtils.isAiMonitoringEnabled());
    }

    @Test
    public void isAiMonitoringEnabled_whenEnabled_recordsEnabledSupportabilityMetric() {
        Map<String, Object> serverData = withAgentConfig(null);
        Map<String, Object> localSettings = baseLocalSettings();
        localSettings.put("ai_monitoring.enabled", true);

        AgentConfig agentConfig = buildConfig(localSettings, serverData);
        installConfig(agentConfig);

        assertTrue(AiMonitoringUtils.isAiMonitoringEnabled());

        MetricAggregator metricAggregator = ((MockServiceManager) ServiceFactory.getServiceManager()).getStatsService().getMetricAggregator();
        Mockito.verify(metricAggregator).incrementCounter("Supportability/Java/ML/Enabled");
    }

    @Test
    public void isAiMonitoringEnabled_whenDisabled_recordsDisabledSupportabilityMetric() {
        Map<String, Object> serverData = withAgentConfig(null);
        Map<String, Object> localSettings = baseLocalSettings();
        localSettings.put("ai_monitoring.enabled", false);

        AgentConfig agentConfig = buildConfig(localSettings, serverData);
        installConfig(agentConfig);

        assertFalse(AiMonitoringUtils.isAiMonitoringEnabled());

        MetricAggregator metricAggregator = ((MockServiceManager) ServiceFactory.getServiceManager()).getStatsService().getMetricAggregator();
        Mockito.verify(metricAggregator).incrementCounter("Supportability/Java/ML/Disabled");
    }

}