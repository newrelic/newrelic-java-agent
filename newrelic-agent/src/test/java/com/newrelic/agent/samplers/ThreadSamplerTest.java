package com.newrelic.agent.samplers;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ThreadSamplerTest {
    @Before
    public void setup() {
        MockServiceManager serviceManager = new MockServiceManager();
        serviceManager.setConfigService(new MockConfigService(AgentConfigFactory.createAgentConfig(
                Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(), null)));
        ServiceFactory.setServiceManager(serviceManager);
    }

    @Test
    public void sample_withDeadLockDetectorOn_onlyRecordsThreadCount() {
        StatsEngine mockStatsEngine = mock(StatsEngine.class);
        ThreadSampler threadSampler = new ThreadSampler();
        when(mockStatsEngine.getStats(MetricNames.THREAD_COUNT)).thenReturn(new StatsImpl(0, 0.0f, 0.0f, 0.0f, 0.0));
        when(mockStatsEngine.getStats(MetricNames.THREAD_DEADLOCK_COUNT)).thenReturn(new StatsImpl(0, 0.0f, 0.0f, 0.0f, 0.0));

        threadSampler.sample(mockStatsEngine);
        verify(mockStatsEngine, times(1)).getStats(MetricNames.THREAD_COUNT);
        verify(mockStatsEngine, times(1)).getStats(MetricNames.THREAD_DEADLOCK_COUNT);
    }

}
