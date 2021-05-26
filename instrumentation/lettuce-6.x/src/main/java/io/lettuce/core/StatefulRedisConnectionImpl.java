package io.lettuce.core;

import java.time.Duration;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.PushHandler;

@Weave(type = MatchType.BaseClass)
public abstract class StatefulRedisConnectionImpl<K, V> implements StatefulConnection<K, V> {

	@NewField
	public RedisURI redisURI = null;

	public StatefulRedisConnectionImpl(RedisChannelWriter writer, PushHandler pushHandler, RedisCodec<K, V> codec, Duration timeout) {

	}
}
