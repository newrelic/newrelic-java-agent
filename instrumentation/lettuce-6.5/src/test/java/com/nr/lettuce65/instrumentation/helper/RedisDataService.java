/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.lettuce65.instrumentation.helper;

import com.newrelic.api.agent.Trace;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.testcontainers.containers.GenericContainer;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class RedisDataService {
    private final GenericContainer genericContainer;
    private RedisCommands<String, String> syncCommands;
    private RedisAsyncCommands<String, String> asyncCommands;
    private RedisStringReactiveCommands<String, String> reactiveCommands;

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

    @Trace(dispatcher = true)
    public List<String> reactiveSet(Flux<Data> dataFlux) {
        List<String> values = new ArrayList<>();
        dataFlux.map(data -> reactiveCommands.set(data.key, data.value))
                .flatMap(Function.identity())
                .toIterable()
                .forEach(values::add);
        return values;
    }

    @Trace(dispatcher = true)
    public List<String> reactiveGet(Flux<String> keys) {
        List<String> values = new ArrayList<>();
        keys.map(key -> reactiveCommands.get(key))
                .flatMap(Function.identity())
                .toIterable()
                .forEach(values::add);
        return values;
    }

    public void init() {
        setupRedisClient();
    }

    private void setupRedisClient() {
        RedisClient redisClient = RedisClient.create("redis://" + genericContainer.getHost() + ":" + genericContainer.getMappedPort(6379));
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        this.syncCommands = connection.sync();
        this.asyncCommands = connection.async();
        this.reactiveCommands = connection.reactive();
    }

}
