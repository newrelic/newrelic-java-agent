/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.lettuce43.instrumentation;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.introspec.DatastoreHelper;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.nr.lettuce43.instrumentation.helper.Data;
import com.nr.lettuce43.instrumentation.helper.RedisDataService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"com.lambdaworks.redis"})
public class Lettuce43InstrumentationTest {

    @Rule
    public GenericContainer redis = new GenericContainer(DockerImageName.parse("redis:5.0.3-alpine"))
            .withExposedPorts(6379);
    private RedisDataService redisDataService;

    @Before
    public void before() {
        redisDataService = new RedisDataService(redis);
        redisDataService.init();
    }

    @Test
    public void testSync() {
        // given some data
        String key = "syncKey";
        String value = "syncValue";

        // when sync 'set' called
        String response = redisDataService.syncSet(key, value);

        // then response should be key
        assertEquals("Then response should be key", key, response);

        // when 'get' called
        String received = redisDataService.syncGet(key);

        // then value returned
        assertEquals("Get value", value, received);

        // and 2 transactions have been sent
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals("Finished transaction count", 2, introspector.getFinishedTransactionCount(1000));
        Collection<String> transactionNames = introspector.getTransactionNames();
        assertEquals("Transaction name count", 2, transactionNames.size());

        String setTransactionName = "OtherTransaction/Custom/com.nr.lettuce43.instrumentation.helper.RedisDataService/syncSet";
        String getTransactionName = "OtherTransaction/Custom/com.nr.lettuce43.instrumentation.helper.RedisDataService/syncGet";

        // and transaction names are in collection
        assertTrue("Should contain transaction name for 'set'", transactionNames.contains(setTransactionName));
        assertTrue("Should contain transaction name for 'get'", transactionNames.contains(getTransactionName));

        // and required datastore metrics are sent
        DatastoreHelper helper = new DatastoreHelper(DatastoreVendor.Redis.name());
        helper.assertAggregateMetrics();

        assertEquals(1, introspector.getTransactionEvents(setTransactionName).iterator().next().getDatabaseCallCount());
        assertEquals(1, introspector.getTransactionEvents(getTransactionName).iterator().next().getDatabaseCallCount());
        helper.assertUnscopedOperationMetricCount("SET", 1);
        helper.assertUnscopedOperationMetricCount("GET", 1);

        helper.assertInstanceLevelMetric(DatastoreVendor.Redis.name(), redis.getHost(), redis.getFirstMappedPort().toString());
    }

    @Test
    public void testAsync() throws ExecutionException, InterruptedException {
        // given some data
        Data data = new Data("asyncKey1", "asyncValue1");

        // when async 'set' called
        String response = redisDataService.asyncSet(data);

        // then response should be 'OK'
        assertEquals("Then response should be 'OK'", "OK", response);

        // when async 'get' called
        String value = redisDataService.asyncGet(data.key);

        // then value returned
        assertEquals("Get value", data.value, value);

        // and 2 transactions have been sent
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals("Finished transaction count", 2, introspector.getFinishedTransactionCount(1000));
        Collection<String> transactionNames = introspector.getTransactionNames();
        assertEquals("Transaction name count", 2, transactionNames.size());

        String setTransactionName = "OtherTransaction/Custom/com.nr.lettuce43.instrumentation.helper.RedisDataService/asyncSet";
        String getTransactionName = "OtherTransaction/Custom/com.nr.lettuce43.instrumentation.helper.RedisDataService/asyncGet";

        // and transaction names are in collection
        assertTrue("Should contain transaction name for 'set'", transactionNames.contains(setTransactionName));
        assertTrue("Should contain transaction name for 'get'", transactionNames.contains(getTransactionName));

        // and required datastore metrics are sent
        DatastoreHelper helper = new DatastoreHelper("Redis");
        helper.assertAggregateMetrics();
        assertEquals(1, introspector.getTransactionEvents(setTransactionName).iterator().next().getDatabaseCallCount());
        assertEquals(1, introspector.getTransactionEvents(getTransactionName).iterator().next().getDatabaseCallCount());
        helper.assertUnscopedOperationMetricCount("SET", 1);
        helper.assertUnscopedOperationMetricCount("GET", 1);
        helper.assertInstanceLevelMetric(DatastoreVendor.Redis.name(), redis.getHost(), redis.getFirstMappedPort().toString());
    }
}
