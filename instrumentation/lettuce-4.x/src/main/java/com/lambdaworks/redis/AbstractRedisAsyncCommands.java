package com.lambdaworks.redis;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.protocol.AsyncCommand;
import com.lambdaworks.redis.protocol.ProtocolKeyword;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.fit.lettuce.instrumentation.NRBiConsumer;

@Weave(type = MatchType.BaseClass)
public abstract class AbstractRedisAsyncCommands<K, V> {

	public abstract StatefulConnection<K, V> getConnection();

	@SuppressWarnings("unchecked")
	@Trace
	public <T> AsyncCommand<K, V, T> dispatch(RedisCommand<K, V, T> cmd) {
		AsyncCommand<K, V, T> acmd = Weaver.callOriginal();
		String collName = "?";
		RedisURI uri = null;

		StatefulConnection<K, V> conn = getConnection();
		if (StatefulRedisConnectionImpl.class.isInstance(conn)) {
			StatefulRedisConnectionImpl<K, V> connImpl = (StatefulRedisConnectionImpl<K, V>) conn;
			if (connImpl.redisURI != null) {
				uri = connImpl.redisURI;
			}
		}
		String operation = "UnknownOp";
		ProtocolKeyword t = cmd.getType();
		if (t != null && t.name() != null && !t.name().isEmpty()) {
			operation = t.name();
		}
		DatastoreParameters params = null;
		if (uri != null) {

			params = DatastoreParameters.product("Redis").collection(collName).operation(operation)
					.instance(uri.getHost(), uri.getPort()).noDatabaseName().build();
		} else {
			params = DatastoreParameters.product("Redis").collection(collName).operation("").noInstance()
					.noDatabaseName().noSlowQuery().build();
		}
		Segment segment = NewRelic.getAgent().getTransaction().startSegment("Lettuce", operation);

		NRBiConsumer<T> nrBiConsumer = new NRBiConsumer<T>(segment, params);

		return (AsyncCommand<K, V, T>) acmd.whenComplete(nrBiConsumer);
	}
}
