/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.lettuce43.instrumentation.helper;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.newrelic.api.agent.Trace;
import org.testcontainers.containers.GenericContainer;

import java.util.concurrent.ExecutionException;

public class RedisDataService {
    private final GenericContainer genericContainer;
    private RedisAsyncCommands<String, String> asyncCommands;
    private RedisCommands<String, String> syncCommands;

    public RedisDataService(GenericContainer container) {
        this.genericContainer = container;
    }

    @Trace(dispatcher = true)
    public String syncSet(String key, String value) {
        syncCommands.set(key, value);
        return key;
    }

    @Trace(dispatcher = true)
    public String syncGet(String key) {
        return syncCommands.get(key);
    }

    @Trace(dispatcher = true)
    public String asyncSet(Data data) throws ExecutionException, InterruptedException {
        String value = asyncCommands.set(data.key, data.value).get();
        return value;
    }

    @Trace(dispatcher = true)
    public String asyncGet(String key) throws ExecutionException, InterruptedException {
        return asyncCommands.get(key).get();
    }

    public void init() {
        setupRedisClient();
    }

    private void setupRedisClient() {
        RedisClient redisClient = RedisClient.create("redis://" + genericContainer.getHost() + ":" + genericContainer.getMappedPort(6379));
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        this.syncCommands = connection.sync();
        this.asyncCommands = connection.async();
    }

}
