package com.newrelic.agent.tracing;

import com.newrelic.agent.trace.TransactionGuidFactory;
import com.newrelic.agent.tracing.samplers.TraceIdRatioBasedSampler;
import org.junit.Test;

import static org.junit.Assert.*;

public class TraceRatioSamplerTest {

    @Test
    public void testSampleTraceIdFromRatio() {
        float traceRatio = 0.467f;
        int sampleSize = 10000;
        float maxExpectedErrorRate = 0.01f;
        //create 1000 random trace ids
        //then verify that the number that gets sampled converges to traceRatio
        TraceIdRatioBasedSampler sampler = new TraceIdRatioBasedSampler(traceRatio);
        int totalTracesSampled = 0;
        for (int i = 0; i < sampleSize; i++) {
            String traceId = TransactionGuidFactory.generate16CharGuid() +  TransactionGuidFactory.generate16CharGuid();
            totalTracesSampled += (sampler.shouldSample(traceId) ? 1 : 0);
        }
        int expectedSampled = (int)(sampleSize * traceRatio);
        int errorMargin = (int)(sampleSize * maxExpectedErrorRate);
        float actualErrorRate = (expectedSampled - totalTracesSampled)/(float) sampleSize;
        System.out.println("Total sampled: " + totalTracesSampled + ", expected sampled: " + expectedSampled + ", actual error: " + actualErrorRate);
        assertTrue(expectedSampled + errorMargin >= totalTracesSampled);
        assertTrue(expectedSampled - errorMargin <= totalTracesSampled);
    }

}