package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.tracing.DistributedTraceUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The adaptive sampler is non-deterministic in its behavior, ie, we can almost never guarantee
 * the exact number of things that will be sampled. Most of the tests
 * have a margin of error (the errorDelta) around the expected value TARGET for this reason.
 * If these tests start flaking, and there is no clear cause, consider adjusting the error margin to
 * account for the non-deterministic behavior.
 *
 * Over time, the adaptive sampler should average to TARGET sampled=true decisions per period.
 * In a given period, there could be considerable deviation from TARGET, but results should always
 * be between 0 and TARGET*2. Because TARGET is the expected value in the long run, these tests should
 * become more reliable the greater NUM_PERIODS we use.
 *
 * Also, the adaptive sampler runs on an internal timer. Most of the tests have a period
 * that runs at a more rapid pace than we'd expect in the wild, because running multiple
 * iterations of a 60sec period isn't practical. Even so, expect these tests to run for awhile.
 */
public class AdaptiveSamplerTest {

    static int DEFAULT_REQUESTS_PER_SEC = 20;
    static float DEFAULT_ERROR_MARGIN = 0.15f;
    static String errorMessage = "Expected %s sampled but actual sampled was %s";

    private MockServiceManager serviceManager;

    @Before
    public void setup(){
        serviceManager = new MockServiceManager();
    }

    @Test
    public void testCalculatePriorityDefaultVals() throws InterruptedException{
        int DEFAULT_TARGET = 120;
        int DEFAULT_REPORT_PERIOD = 60;
        int numPeriods = 5;
        long testLengthMillis = DEFAULT_REPORT_PERIOD * numPeriods * 1000;

        AdaptiveSampler defaultSampler = AdaptiveSampler.getSharedInstance();
        int totalSampled = runSamplerAndGetSampled(defaultSampler, testLengthMillis, DEFAULT_REQUESTS_PER_SEC);

        int expectedSampled = DEFAULT_TARGET * numPeriods;
        int errorDelta = (int)(expectedSampled * DEFAULT_ERROR_MARGIN);
        Assert.assertTrue(String.format(errorMessage, expectedSampled, totalSampled),
                Math.abs(expectedSampled - totalSampled) < errorDelta);
    }

    @Test
    public void testTargetIsZeroShouldSampleNothing() throws InterruptedException{
        int target = 0;
        int reportPeriod = 5;
        int numPeriods = 12;
        long testLengthMillis = reportPeriod * numPeriods * 1000;

        AdaptiveSampler sampler = new AdaptiveSampler(target, reportPeriod);
        int totalSampled = runSamplerAndGetSampled(sampler, testLengthMillis, DEFAULT_REQUESTS_PER_SEC);

        int expectedSampled = 0;
        Assert.assertEquals(String.format(errorMessage, expectedSampled, totalSampled), expectedSampled, totalSampled);
    }

    @Test
    public void testFirstPeriodShouldSampleTargetExactly() throws InterruptedException {
        int target = 15;
        int reportPeriod = 10;
        AdaptiveSampler sampler = new AdaptiveSampler(target, reportPeriod);
        long testLengthMillis = 12000L; //run test for 12 seconds, just over 1 period

        runSamplerAndGetSampledRandomLoad(sampler, testLengthMillis);

        int sampledFirstPeriod = sampler.getSampledCountLastPeriod();
        int expectedSampled = 15;
        Assert.assertEquals(String.format(errorMessage, expectedSampled, sampledFirstPeriod), expectedSampled, sampledFirstPeriod);
    }

    @Test
    public void testIntermittentLoad() throws InterruptedException {
        int target = 50;
        int reportPeriod = 5;
        int numPeriods = 6;
        long testLengthMillis = numPeriods * reportPeriod * 1000L; //this test runs through this twice

        AdaptiveSampler sampler = new AdaptiveSampler(target, reportPeriod);
        int totalSampledFirstLoad = runSamplerAndGetSampled(sampler, testLengthMillis, DEFAULT_REQUESTS_PER_SEC);
        Thread.sleep(13000); //sleep to stop traffic to the sampler
        int totalSampledSecondLoad = runSamplerAndGetSampled(sampler, testLengthMillis, DEFAULT_REQUESTS_PER_SEC);
        int totalSampled = totalSampledFirstLoad + totalSampledSecondLoad;

        int expectedSampled = target * numPeriods * 2;
        int errorDelta = (int)(expectedSampled * DEFAULT_ERROR_MARGIN);
        System.out.println("Expected: " + expectedSampled + " Actual: " + totalSampled);
        Assert.assertTrue(String.format(errorMessage, expectedSampled, totalSampled), Math.abs(totalSampled - expectedSampled) <= errorDelta);
    }

    @Test
    public void testDelayedLoad() throws InterruptedException {
        int target = 50;
        int reportPeriod = 5;
        int numPeriods = 15;
        long testLengthMillis = numPeriods * reportPeriod * 1000L;

        AdaptiveSampler sampler = new AdaptiveSampler(target, reportPeriod);
        Thread.sleep(12000); //sleep for a few periods initially
        int totalSampled = runSamplerAndGetSampled(sampler, testLengthMillis, DEFAULT_REQUESTS_PER_SEC);

        int expectedSampled = target * numPeriods;
        int errorDelta = (int)(expectedSampled * DEFAULT_ERROR_MARGIN);
        Assert.assertTrue(String.format(errorMessage, expectedSampled, totalSampled), Math.abs(totalSampled - expectedSampled) <= errorDelta);
    }

    @Test
    public void testExponentialBackoff() throws InterruptedException, ExecutionException {
        //here, we overload the sampler aggressively for a few periods and verify that
        //no more than 2x sampled count is sampled each time.
        int target = 10;
        int reportPeriod = 5;
        AdaptiveSampler sampler = new AdaptiveSampler(target, reportPeriod);

        int overloadPhasePeriods = 10;
        long overloadPhaseLength = 50000L; //50 seconds, 10 periods
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> f = executor.submit(() -> {
            try {
                return runSamplerAndGetSampled(sampler, overloadPhaseLength, 1000);
            } catch (InterruptedException e) {
                return null;
            }
        });

        //While the sampler runs in the background, check in each period to see how many things
        //it sampled most recently.
        List<Integer> sampledCountEachPeriod = new ArrayList<>();
        for (int i = 0; i < overloadPhasePeriods; i++) {
            Thread.sleep(reportPeriod * 1000);
            sampledCountEachPeriod.add(sampler.getSampledCountLastPeriod());
        }

        //Block until the thread is done
        Integer actualTotalSampled = f.get();

        //assert the total seems right
        Assert.assertNotNull(actualTotalSampled);
        int expectedTotalSampled = target * overloadPhasePeriods;
        int expectedDelta = (int) (expectedTotalSampled * 0.2f); //this test is a bit flakier, setting higher error margin
        Assert.assertTrue(String.format(errorMessage, expectedTotalSampled, actualTotalSampled),
                Math.abs(expectedTotalSampled - actualTotalSampled) <= expectedDelta );

        //iterate over the results from each period and assert that no individual
        //period had a result exceeding 2*target, AND that the first time through we sampled exactly target.
        Assert.assertEquals(target, (int) sampledCountEachPeriod.get(0));
        for (int sampledCount : sampledCountEachPeriod) {
            Assert.assertTrue(String.format("Expected less than %s sampled for each period, but a period actually sampled %s", 2*target, sampledCount),
                    sampledCount <= 2*target);
        }
    }

    @Test
    public void testCalculatePriorityMultithreaded() throws InterruptedException {
        int target = 10;
        int reportPeriod = 5;
        int totalPeriods = 10;
        long totalTestTimeMillis = reportPeriod * totalPeriods * 1000;

        AdaptiveSampler sampler = new AdaptiveSampler(target, reportPeriod);
        int totalSampled = runSamplerConcurrentAndGetSampled(sampler, totalTestTimeMillis);

        int expectedSampled = target * totalPeriods;
        int errorDelta = (int)(expectedSampled * DEFAULT_ERROR_MARGIN);
        Assert.assertTrue(String.format(errorMessage, expectedSampled, totalSampled), Math.abs(totalSampled - expectedSampled) <= errorDelta);
    }

    @Test
    public void testGetAdaptiveSamplerInstanceFulfillsSingleton() throws InterruptedException, ExecutionException {
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
            Assert.assertEquals("All sampler instances retrieved by .getSharedInstance should be equal, but they were not", baseSampler, f.get());
        }
    }

    private int runSamplerAndGetSampled(AdaptiveSampler sampler, long testLengthMillis, int requestsPerSecond) throws InterruptedException {
        //fastest we're going to go for this test is 1 request per ms.
        requestsPerSecond = Math.max(1000, requestsPerSecond);
        long testStartTime = System.currentTimeMillis();
        int totalSampled = 0;
        int waitBetweenSamples = 1000 / requestsPerSecond;
        while (System.currentTimeMillis() - testStartTime < testLengthMillis) {
            float priority = sampler.calculatePriority();
            boolean sampled = DistributedTraceUtil.isSampledPriority(priority);
            if (sampled) {
                totalSampled++;
            }
            Thread.sleep(waitBetweenSamples);
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