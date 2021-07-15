package io.lettuce.core;

import java.time.Duration;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.codec.RedisCodec;

@Weave(type = MatchType.BaseClass)
public abstract class StatefulRedisConnectionImpl<K, V> implements StatefulConnection<K, V> {

	@NewField
	public RedisURI redisURI = null;

	public StatefulRedisConnectionImpl(RedisChannelWriter writer, RedisCodec<K, V> codec, Duration timeout) {

	}
}
