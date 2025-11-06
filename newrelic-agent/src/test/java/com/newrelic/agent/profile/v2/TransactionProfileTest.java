package com.newrelic.agent.profile.v2;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.profile.ProfilerParameters;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.threads.BasicThreadInfo;
import com.newrelic.agent.threads.ThreadNameNormalizer;
import com.newrelic.agent.threads.ThreadNames;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.test.marker.RequiresFork;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;

@Category(RequiresFork.class)
public class TransactionProfileTest {

    TransactionProfile target;
    StatsService statsService;

    @Test
    public void test_transactionFinished() throws IOException {
        setupServices();

        StackTraceElement ste = new StackTraceElement("class", "method", "file", 1);
        ClassMethodSignature cms = new ClassMethodSignature("class", "method", "()V");
        Tracer tracer = Mockito.mock(Tracer.class);
        Mockito.when(tracer.getAgentAttribute(Mockito.any())).thenReturn(Arrays.asList(ste));
        Mockito.when(tracer.getClassMethodSignature()).thenReturn(cms);
        TransactionActivity ta = Mockito.mock(TransactionActivity.class);
        Mockito.when(ta.getTracers()).thenReturn(Arrays.asList(tracer));
        Mockito.when(ta.getTotalCpuTime()).thenReturn(123L);
        Mockito.when(ta.getThreadId()).thenReturn(1L);
        Mockito.when(ta.getRootTracer()).thenReturn(tracer);
        TransactionData td = Mockito.mock(TransactionData.class);
        Mockito.when(td.getTransactionActivities()).thenReturn(new HashSet<>(Arrays.asList(ta)));

        target.transactionFinished(td);

        Mockito.verify(statsService, Mockito.times(1)).doStatsWork(Mockito.any(), Mockito.any());

        StackTraceElement ste2 = new StackTraceElement("class2", "method2", "file2", 2);
        target.addStackTrace(Arrays.asList(ste2));

        StringWriter writer = new StringWriter();
        target.writeJSONString(writer);
        Assert.assertTrue(writer.toString().contains("Test worker"));
        Assert.assertTrue(writer.toString().contains("\"2\""));
    }

    private void setupServices() {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        statsService = Mockito.mock(StatsService.class);
        serviceManager.setStatsService(statsService);

        ProfilerParameters profilerParameters = new ProfilerParameters(0L, 50L,
                50L, false, false, Agent.isDebugEnabled(),
                null, null);
        Profile profile = new Profile(profilerParameters, "sessionId", ServiceFactory.getThreadService().getThreadNameNormalizer());
        ThreadNameNormalizer tnn = getThreadNameNormalizer();

        target = new TransactionProfile(profile, tnn);
    }

    private static final ThreadNameNormalizer getThreadNameNormalizer() {
        ThreadNames threadNames = new ThreadNames() {

            @Override
            public String getThreadName(BasicThreadInfo thread) {
                return thread.getName();
            }
        };
        return new ThreadNameNormalizer(threadNames);
    }
}
