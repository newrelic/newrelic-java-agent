/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.lettuce.core;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.lettuce6.instrumentation.*;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.RedisCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Weave(originalName = "io.lettuce.core.AbstractRedisReactiveCommands")
public abstract class AbstractRedisReactiveCommands_Instrumentation<K, V> {
    private final StatefulConnection<K, V> connection = Weaver.callOriginal();

    public <T> Mono<T> createMono(Supplier<RedisCommand<K, V, T>> commandSupplier) {
        Mono<T> result = Weaver.callOriginal();
        RedisCommand<K, V, T> cmd = commandSupplier.get();
        if (cmd != null) {
            ProtocolKeyword type = cmd.getType();
            String name = type.toString();

            String collName = null;
            RedisURI uri = null;
            if (StatefulRedisConnectionImpl_Instrumentation.class.isInstance(connection)) {
                StatefulRedisConnectionImpl_Instrumentation<K, V> connImpl = (StatefulRedisConnectionImpl_Instrumentation<K, V>) connection;
                if (connImpl.redisURI != null) {
                    uri = connImpl.redisURI;
                }
            }
            String operation = "UnknownOp";
            ProtocolKeyword t = cmd.getType();
            if ((t != null) && (t.toString() != null) && (!t.toString().isEmpty())) {
                operation = t.toString();
            }
            DatastoreParameters params = RedisDatastoreParameters.from(uri, operation);
            NRHolder holder = new NRHolder(name, params);
            NRSubscribeConsumer subscriberConsumer = new NRSubscribeConsumer(holder);

            NRErrorConsumer errorConsumer = new NRErrorConsumer(holder);
            Consumer<SignalType> onFinally = new NRSignalTypeConsumer(holder);
            return result.doOnSubscribe(subscriberConsumer).doOnError(errorConsumer).doFinally(onFinally);
        }
        return result;
    }

    public <T, R> Flux<R> createDissolvingFlux(Supplier<RedisCommand<K, V, T>> commandSupplier) {
        Flux<R> result = Weaver.callOriginal();
        RedisCommand<K, V, T> cmd = commandSupplier.get();
        if (cmd != null) {
            ProtocolKeyword type = cmd.getType();
            String name = type.toString();

            String collName = null;
            RedisURI uri = null;
            if (StatefulRedisConnectionImpl_Instrumentation.class.isInstance(connection)) {
                StatefulRedisConnectionImpl_Instrumentation<K, V> connImpl = (StatefulRedisConnectionImpl_Instrumentation<K, V>) connection;
                if (connImpl.redisURI != null) {
                    uri = connImpl.redisURI;
                }
            }
            String operation = "UnknownOp";
            ProtocolKeyword t = cmd.getType();
            if ((t != null) && (t.toString() != null) && (!t.toString().isEmpty())) {
                operation = t.toString();
            }
            DatastoreParameters params = null;
            if (uri != null) {
                params = DatastoreParameters.product("Redis")
                        .collection(collName)
                        .operation(operation)
                        .instance(uri.getHost(), Integer.valueOf(uri.getPort()))
                        .noDatabaseName()
                        .build();
            } else {
                params = DatastoreParameters.product("Redis").collection(collName).operation("").noInstance().noDatabaseName().noSlowQuery().build();
            }
            NRHolder holder = new NRHolder(name, params);
            NRSubscribeConsumer subscriberConsumer = new NRSubscribeConsumer(holder);

            NRErrorConsumer errorConsumer = new NRErrorConsumer(holder);
            Consumer<SignalType> onFinally = new NRSignalTypeConsumer(holder);
            return result.doOnSubscribe(subscriberConsumer).doOnError(errorConsumer).doFinally(onFinally);
        }
        return result;
    }
}
