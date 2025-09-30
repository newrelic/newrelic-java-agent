package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.tracing.DistributedTraceUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The adaptive sampler is non-deterministic in its behavior. Most of the tests
 * have a margin of error (the errorDelta) around the expected value TARGET for this reason.
 *
 * Over time, the adaptive sampler should average to TARGET sampled=true decisions per period.
 * In a given period, there could be considerable deviation from TARGET, but results should always
 * be between 0 and TARGET*2. Because TARGET is the expected value in the long run, these tests should
 * become more reliable the greater NUM_PERIODS we use.
 *
 * Also, the adaptive sampler runs on an internal timer. Most of the tests have a period
 * that runs at a more rapid pace than we'd expect in the wild, because running multiple
 * iterations of a 60sec period isn't practical.
 */
public class AdaptiveSamplerTest {


    private MockServiceManager serviceManager;

    @Before
    public void setup(){
        serviceManager = new MockServiceManager();
    }

    @Test
    public void testCalculatePriorityDefaultVals() throws InterruptedException{
        int DEFAULT_TARGET = 120;
        int DEFAULT_REPORT_PERIOD = 60;
        AdaptiveSampler defaultSampler = AdaptiveSampler.getSharedInstance();
        int NUM_PERIODS = 5;
        int expectedSampled = DEFAULT_TARGET * NUM_PERIODS;
        long testLengthMillis = DEFAULT_REPORT_PERIOD * NUM_PERIODS * 1000;
        runSingleThreadedTest(defaultSampler, testLengthMillis, expectedSampled);
    }

    @Test
    public void testCalculatePriorityFastPeriod() throws InterruptedException{
        int TARGET = 10;
        int REPORT_PERIOD = 5;
        AdaptiveSampler sampler = new AdaptiveSampler(TARGET, REPORT_PERIOD);
        int NUM_PERIODS = 30;
        int expectedSampled = TARGET * NUM_PERIODS;
        long testLengthMillis = REPORT_PERIOD * NUM_PERIODS * 1000;
        runSingleThreadedTest(sampler, testLengthMillis, expectedSampled);
    }

    @Test
    public void testTargetIsZeroShouldSampleNothing() throws InterruptedException{
        int TARGET = 0;
        int REPORT_PERIOD = 5;
        AdaptiveSampler sampler = new AdaptiveSampler(TARGET, REPORT_PERIOD);
        int NUM_PERIODS = 12;
        int expectedSampled = 0;
        long testLengthMillis = REPORT_PERIOD * NUM_PERIODS * 1000;
        runSingleThreadedTest(sampler, testLengthMillis, expectedSampled);
    }

    @Test
    public void testFirstPeriodShouldSampleTargetExactly() throws InterruptedException {
        int TARGET = 15;
        int REPORT_PERIOD = 10;
        AdaptiveSampler sampler = new AdaptiveSampler(TARGET, REPORT_PERIOD);

        long testLengthMillis = 12000L; //run test for 12 seconds
        runSamplerAndGetSampledRandomLoad(sampler, testLengthMillis);

        int sampledFirstPeriod = sampler.getSampledCountLastPeriod();
        int expectedSampled = 15;
        Assert.assertEquals(expectedSampled, sampledFirstPeriod);
    }

    @Test
    public void testIntermittentLoad() throws InterruptedException {
        int TARGET = 50;
        int REPORT_PERIOD = 5;
        int numPeriods = 10;

        AdaptiveSampler sampler = new AdaptiveSampler(TARGET, REPORT_PERIOD);

        long testLengthMillis = numPeriods * REPORT_PERIOD * 1000L;
        int totalSampledFirstLoad = runSamplerAndGetSampled(sampler, testLengthMillis);
        int expectedSampledFirstLoad = TARGET * numPeriods;

        Thread.sleep(13000); //sleep to stop traffic to the sampler

        int totalSampledSecondLoad = runSamplerAndGetSampled(sampler, testLengthMillis);
        int expectedSampledSecondLoad = TARGET * numPeriods;

        //results
        int expectedSampled = expectedSampledFirstLoad + expectedSampledSecondLoad;
        int totalSampled = totalSampledFirstLoad + totalSampledSecondLoad;
        float errorMargin = 0.05f;
        int errorDelta = (int)(errorMargin * expectedSampled);
        System.out.println("Expected: " + expectedSampled + " Actual: " + totalSampled);
        Assert.assertTrue(Math.abs(totalSampled - expectedSampled) <= errorDelta);
    }

    @Test
    public void testDelayedLoad() throws InterruptedException {
        int TARGET = 50;
        int REPORT_PERIOD = 5;
        int numPeriods = 10;

        AdaptiveSampler sampler = new AdaptiveSampler(TARGET, REPORT_PERIOD);

        long testLengthMillis = numPeriods * REPORT_PERIOD * 1000L;

        Thread.sleep(12000); //sleep for a few periods initially

        int totalSampled = runSamplerAndGetSampled(sampler, testLengthMillis);
        int expectedSampled = TARGET * numPeriods;

        //evaluate results
        float errorMargin = 0.1f;
        int errorDelta = (int)(errorMargin * expectedSampled);
        System.out.println("Expected: " + expectedSampled + " Actual: " + totalSampled + " Max accepted difference: " + errorDelta);
        Assert.assertTrue(Math.abs(totalSampled - expectedSampled) <= errorDelta);
    }

    @Test
    public void testCalculatePriorityMultithreaded() throws Exception{
        //setup sampler
        int TARGET = 10;
        int REPORT_PERIOD = 5;
        AdaptiveSampler sampler = new AdaptiveSampler(TARGET, REPORT_PERIOD);
        int TOTAL_PERIODS = 30;
        long totalTestTimeMillis = REPORT_PERIOD * TOTAL_PERIODS * 1000;
        int totalSampled = runSamplerConcurrentAndGetSampled(sampler, totalTestTimeMillis);
        int expectedSampled = TARGET * TOTAL_PERIODS;
        float errorMargin = 0.15f;
        int errorDelta = (int)(errorMargin * expectedSampled);
        System.out.println("Expected: " + expectedSampled + " Actual: " + totalSampled);
        Assert.assertTrue(Math.abs(totalSampled - expectedSampled) <= errorDelta);
    }

    @Test
    public void testGetAdaptiveSamplerInstance() throws InterruptedException, ExecutionException {
        //The sampler is REQUIRED to be a singleton instance.
        //This test verifies that access to the sampler always returns the same instance.
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<?>> samplerArray = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            Future<?> fut = executor.submit(AdaptiveSampler::getSharedInstance);
            samplerArray.add(fut);
        }
        AdaptiveSampler baseSampler = AdaptiveSampler.getSharedInstance();
        Assert.assertNotNull(baseSampler);
        for (Future<?> f : samplerArray) {
            Assert.assertEquals(baseSampler, f.get());
        }
    }


    private void setupDefaultConfig() {
        Map<String, Object> settings = new HashMap<>();
        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(settings),
                Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
    }

    private void runSingleThreadedTest(AdaptiveSampler sampler, long testLengthMillis, int expectedSampled) throws InterruptedException{
        int totalSampled = runSamplerAndGetSampledRandomLoad(sampler, testLengthMillis);
        //evaluate results
        float errorMargin = 0.15f;
        int errorDelta = (int)(errorMargin * expectedSampled);
        System.out.println("Expected: " + expectedSampled + " Actual: " + totalSampled);
        Assert.assertTrue(Math.abs(totalSampled - expectedSampled) <= errorDelta);
    }

    private int runSamplerAndGetSampled(AdaptiveSampler sampler, long testLengthMillis) throws InterruptedException {
        long testStartTime = System.currentTimeMillis();
        int totalSampled = 0;
        int WAIT_BETWEEN_SAMPLES = 50;
        while (System.currentTimeMillis() - testStartTime < testLengthMillis) {
            float priority = sampler.calculatePriority();
            boolean sampled = DistributedTraceUtil.isSampledPriority(priority);
            if (sampled) {
                totalSampled++;
            }
            Thread.sleep(WAIT_BETWEEN_SAMPLES);
        }
        return totalSampled;
    }

    private int runSamplerAndGetSampledRandomLoad(AdaptiveSampler sampler, long testLengthMillis) throws InterruptedException {
        //in this iteration, we randomize the load on the sampler a bit.
        //We'll hit the sampler randomly, with no more than 200 millis between accesses.
        Random random = new Random();
        int MAX_WAIT_BETWEEN_SAMPLES = 200;

        long testStartTime = System.currentTimeMillis();
        int totalSampled = 0;
        while (System.currentTimeMillis() - testStartTime < testLengthMillis) {
            float priority = sampler.calculatePriority();
            boolean sampled = DistributedTraceUtil.isSampledPriority(priority);
            if (sampled) {
                totalSampled++;
            }
            Thread.sleep(random.nextInt(MAX_WAIT_BETWEEN_SAMPLES));
        }
        return totalSampled;
    }

    public int runSamplerConcurrentAndGetSampled(AdaptiveSampler sampler, long totalTestTimeMillis) throws InterruptedException {
        int nThreads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        AtomicInteger totalSampled = new AtomicInteger(0);
        //in this example, the wait is random, and the load is distributed across 4 threads.
        Random random = new Random();
        int MAX_WAIT_BETWEEN_SAMPLES = 500;
        //Start it up
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < nThreads ; i++) {
            executor.submit(() -> {
                while(System.currentTimeMillis() - startTime < totalTestTimeMillis) {
                    float priority = sampler.calculatePriority();
                    boolean sampled = DistributedTraceUtil.isSampledPriority(priority);
                    if (sampled) {
                        totalSampled.incrementAndGet();
                    }
                    try {
                        Thread.sleep(random.nextInt(MAX_WAIT_BETWEEN_SAMPLES));
                    } catch(InterruptedException e){
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
        Thread.sleep(totalTestTimeMillis + 1000);
        return totalSampled.get();
    }

}