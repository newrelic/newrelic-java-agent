package com.lambdaworks.redis;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@SuppressWarnings("deprecation")
@Weave(originalName="com.lambdaworks.redis.RedisClient")
public abstract class RedisClient_instrumentation extends AbstractRedisClient {

	public static RedisClient_instrumentation create(String uri) {
		return Weaver.callOriginal();
	}

	public abstract StatefulRedisConnection<String, String> connect();

	private final RedisURI redisURI = Weaver.callOriginal();

	protected <K, V> StatefulRedisConnectionImpl<K, V> newStatefulRedisConnection(CommandHandler<K, V> commandHandler,
			RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
		StatefulRedisConnectionImpl<K, V> connection = Weaver.callOriginal();
		connection.redisURI = redisURI;
		return connection;
	}
}
