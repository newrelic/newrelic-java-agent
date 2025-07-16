/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.lettuce6.instrumentation;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.introspec.DatastoreHelper;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.nr.lettuce6.instrumentation.helper.Data;
import com.nr.lettuce6.instrumentation.helper.RedisDataService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"io.lettuce.core", "io.lettuce.core.protocol", "io.netty.channel"})
public class Lettuce6InstrumentationTest {

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

        String setTransactionName = "OtherTransaction/Custom/com.nr.lettuce6.instrumentation.helper.RedisDataService/syncSet";
        String getTransactionName = "OtherTransaction/Custom/com.nr.lettuce6.instrumentation.helper.RedisDataService/syncGet";

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

        String setTransactionName = "OtherTransaction/Custom/com.nr.lettuce6.instrumentation.helper.RedisDataService/asyncSet";
        String getTransactionName = "OtherTransaction/Custom/com.nr.lettuce6.instrumentation.helper.RedisDataService/asyncGet";

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

    @Test
    public void testReactive() {
        // given some data
        Data data1 = new Data("key1", "value1");
        Data data2 = new Data("key2", "value3");
        Data data3 = new Data("key3", "value3");

        // when reactive 'set' called
        List<String> ids = redisDataService.reactiveSet(Flux.just(data1, data2, data3));

        // then all 'OK'
        assertArrayEquals("All responses should be 'OK'",
                new String[]{"OK", "OK", "OK"}, ids.toArray());

        // when reactive 'get' called
        List<String> values = redisDataService
                .reactiveGet(Flux.just(data1.key, data2.key, data3.key));

        // then 3 values returned
        String[] expectedValues = new String[]{data1.value, data2.value, data3.value};
        assertEquals("Get values size did not math the amount set", 3, values.size());
        assertArrayEquals("Values returned should equal sent", expectedValues, values.toArray());

        // and 2 transactions have been sent
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(2, introspector.getFinishedTransactionCount(1000));
        Collection<String> transactionNames = introspector.getTransactionNames();
        assertEquals(2, transactionNames.size());

        String setTransactionName = "OtherTransaction/Custom/com.nr.lettuce6.instrumentation.helper.RedisDataService/reactiveSet";
        String getTransactionName = "OtherTransaction/Custom/com.nr.lettuce6.instrumentation.helper.RedisDataService/reactiveGet";

        // and transaction names are in collection
        assertTrue("Should contain transaction name for 'set'", transactionNames.contains(setTransactionName));
        assertTrue("Should contain transaction name for 'get'", transactionNames.contains(getTransactionName));

        // and required datastore metrics are sent
        DatastoreHelper helper = new DatastoreHelper("Redis");
        helper.assertAggregateMetrics();
        assertEquals(3, introspector.getTransactionEvents(setTransactionName).iterator().next().getDatabaseCallCount());
        assertEquals(3, introspector.getTransactionEvents(getTransactionName).iterator().next().getDatabaseCallCount());
        helper.assertUnscopedOperationMetricCount("SET", 3);
        helper.assertUnscopedOperationMetricCount("GET", 3);
        helper.assertInstanceLevelMetric("Redis", "localhost", redis.getFirstMappedPort().toString());
    }

}
