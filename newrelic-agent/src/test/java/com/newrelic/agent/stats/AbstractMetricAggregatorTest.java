/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.stats;

import com.newrelic.api.agent.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class AbstractMetricAggregatorTest {

    /**
     * BadAggregator throwns an exception for everything it implements.
     */
    private static class BadAggregator extends AbstractMetricAggregator {

        public BadAggregator(Logger logger) {
            super(logger);
        }

        @Override
        protected void doRecordResponseTimeMetric(String name, long totalTime, long exclusiveTime, TimeUnit timeUnit) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void doRecordMetric(String name, float value) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void doIncrementCounter(String name, int count) {
            throw new UnsupportedOperationException();
        }

    }

    private Logger logger;
    private BadAggregator aggregator;

    @Before
    public void before() {
        this.aggregator = new BadAggregator(this.logger = Mockito.mock(Logger.class));
        Mockito.when(logger.isLoggable(Level.FINER)).thenReturn(true);
    }

    // verify that the exceptions thrown out of our implementation are caught

    @Test
    public void incrementCounter() {
        aggregator.incrementCounter("test");
    }

    @Test
    public void incrementCounter2() {
        Mockito.when(logger.isLoggable(Level.FINEST)).thenReturn(true);
        aggregator.incrementCounter("test", 5);
        Mockito.verify(logger, Mockito.atLeastOnce()).log(Mockito.eq(Level.FINEST),
                Mockito.any(UnsupportedOperationException.class),
                Mockito.eq("Exception incrementing counter \"{0}\",{1} : {2}"), Mockito.eq("test"), Mockito.eq(5),
                Mockito.any(UnsupportedOperationException.class));
    }

    @Test
    public void recordMetric() {
        aggregator.recordMetric("test", 0.6f);
    }

    @Test
    public void recordResponseTimeMetric() {
        aggregator.recordResponseTimeMetric("test", 4);
    }

    @Test
    public void recordResponseTimeMetric2() {
        aggregator.recordResponseTimeMetric("test", 5, 6, TimeUnit.MILLISECONDS);
    }

    // verify that null strings don't throw exceptions

    @Test
    public void incrementCounterNull() {
        aggregator.incrementCounter(null);
        Mockito.verify(logger, Mockito.atLeastOnce()).log(Level.FINER,
                "incrementCounter was invoked with a null metric name");
    }

    @Test
    public void incrementCounter2Null() {

        aggregator.incrementCounter(null, 5);
        Mockito.verify(logger, Mockito.atLeastOnce()).log(Level.FINER,
                "incrementCounter was invoked with a null metric name");
    }

    @Test
    public void recordMetricNull() {
        aggregator.recordMetric(null, 0.6f);
        Mockito.verify(logger, Mockito.atLeastOnce()).log(Level.FINER,
                "recordMetric was invoked with a null or empty name");
    }

    @Test
    public void recordResponseTimeMetricNull() {
        aggregator.recordResponseTimeMetric(null, 4);
        Mockito.verify(logger, Mockito.atLeastOnce()).log(Level.FINER,
                "recordResponseTimeMetric was invoked with a null or empty name");
    }

    @Test
    public void recordResponseTimeMetric2Null() {
        aggregator.recordResponseTimeMetric(null, 5, 6, TimeUnit.MILLISECONDS);
        Mockito.verify(logger, Mockito.atLeastOnce()).log(Level.FINER,
                "recordResponseTimeMetric was invoked with a null or empty name");
    }

    // verify empty strings are logged

    @Test
    public void incrementCounterEmptyString() {
        aggregator.incrementCounter("");
        Mockito.verify(logger, Mockito.atLeastOnce()).log(Level.FINER,
                "incrementCounter was invoked with a null metric name");
    }

    @Test
    public void incrementCounter2EmptyString() {

        aggregator.incrementCounter("", 5);
        Mockito.verify(logger, Mockito.atLeastOnce()).log(Level.FINER,
                "incrementCounter was invoked with a null metric name");
    }

    @Test
    public void recordMetricEmptyString() {
        aggregator.recordMetric("", 0.6f);
        Mockito.verify(logger, Mockito.atLeastOnce()).log(Level.FINER,
                "recordMetric was invoked with a null or empty name");
    }

    @Test
    public void recordResponseTimeMetricEmptyString() {
        aggregator.recordResponseTimeMetric("", 4);
        Mockito.verify(logger, Mockito.atLeastOnce()).log(Level.FINER,
                "recordResponseTimeMetric was invoked with a null or empty name");
    }

    @Test
    public void recordResponseTimeMetric2EmptyString() {
        aggregator.recordResponseTimeMetric("", 5, 6, TimeUnit.MILLISECONDS);
        Mockito.verify(logger, Mockito.atLeastOnce()).log(Level.FINER,
                "recordResponseTimeMetric was invoked with a null or empty name");
    }
}
