package com.newrelic.agent.superagent;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.SuperAgentIntegrationConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.jfr.ThreadNameNormalizer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.newrelic.agent.config.AgentConfigImpl.DEFAULT_EVENT_INGEST_URI;
import static com.newrelic.agent.config.AgentConfigImpl.DEFAULT_METRIC_INGEST_URI;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class SuperAgentIntegrationServiceTest {
    @Mock
    AgentConfig agentConfig;

    @Mock
    SuperAgentIntegrationConfig superAgentIntegrationConfig;

    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);

        MockServiceManager manager = new MockServiceManager();
        ServiceFactory.setServiceManager(manager);

        when(agentConfig.getApplicationName()).thenReturn("test_app_name");
        when(agentConfig.getMetricIngestUri()).thenReturn(DEFAULT_METRIC_INGEST_URI);
        when(agentConfig.getEventIngestUri()).thenReturn(DEFAULT_EVENT_INGEST_URI);
        when(agentConfig.getLicenseKey()).thenReturn("test_1234_license_key");
        when(agentConfig.getValue(eq(ThreadService.NAME_PATTERN_CFG_KEY), any(String.class)))
                .thenReturn(ThreadNameNormalizer.DEFAULT_PATTERN);
    }

    @Test
    public void foo() throws Exception {
        //SuperAgentIntegrationService s = new SuperAgentIntegrationService(agentConfig);
        //s.start();
    }
}
