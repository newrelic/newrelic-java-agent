package com.newrelic.agent.profile.v2;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.profile.ProfilerParameters;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.Test;

public class ProfileSessionTest {

    ProfileSession target;

    private static long MAX_DURATION = 1000;

    @Test
    public void test_singleSample() {
        setup(50L, 50L);
        target.start();
        waitForDoneOrTimeout();
        target.stop(true);

        Assert.assertTrue(target.isDone());
        Assert.assertNotNull(target.getProfile());
        Assert.assertEquals(new Long(0), target.getProfileId());
        Assert.assertEquals(1, target.getProfile().getSampleCount());
    }

    @Test
    public void test_multiSample() {
        setup(50L, 500L);
        target.start();
        waitForDoneOrTimeout();
        target.stop(true);

        Assert.assertTrue(target.isDone());
        Assert.assertNotNull(target.getProfile());
        Assert.assertEquals(new Long(0), target.getProfileId());
        // we can't guarantee exactly 10 samples, but it should be close
        Assert.assertTrue(target.getProfile().getSampleCount() >= 9);
    }

    @Test
    public void test_multiStopBeforeDone() {
        setup(50L, 500L);
        target.start();
        target.stop(true);
    }
    
    private void waitForDoneOrTimeout() {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis();
        while (!target.isDone() && ((end-start) < MAX_DURATION)) {
            try {
                Thread.sleep(1);
            } catch (Exception e) {}
        }
    }

    private void setup(long samplePeriodMillis, long durationMillis) {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ProfilerService profilerService = new ProfilerService();
        ProfilerParameters parameters = new ProfilerParameters(0L, samplePeriodMillis,
                durationMillis, false, false, Agent.isDebugEnabled(),
                null, null);

        target = new ProfileSession(profilerService, parameters);
    }
}
