package io.lettuce.core;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.fit.lettuce.instrumentation.NRErrorConsumer;
import com.nr.fit.lettuce.instrumentation.NRHolder;
import com.nr.fit.lettuce.instrumentation.NRSignalTypeConsumer;
import com.nr.fit.lettuce.instrumentation.NRSubscribeConsumer;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.RedisCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

@Weave
public abstract class AbstractRedisReactiveCommands<K, V>
{
	private final StatefulConnection<K, V> connection = Weaver.callOriginal();

	public <T> Mono<T> createMono(Supplier<RedisCommand<K, V, T>> commandSupplier)
	{
		Mono<T> result = Weaver.callOriginal();
		RedisCommand<K, V, T> cmd = commandSupplier.get();
		if (cmd != null)
		{
			ProtocolKeyword type = cmd.getType();
			String name = type.name();

			String collName = "?";
			RedisURI uri = null;
			if (StatefulRedisConnectionImpl.class.isInstance(connection))
			{
				StatefulRedisConnectionImpl<K, V> connImpl = (StatefulRedisConnectionImpl<K, V>) connection;
				if (connImpl.redisURI != null) {
					uri = connImpl.redisURI;
				}
			}
			String operation = "UnknownOp";
			ProtocolKeyword t = cmd.getType();
			if ((t != null) && (t.name() != null) && (!t.name().isEmpty())) {
				operation = t.name();
			}
			DatastoreParameters params = null;
			if (uri != null) {
				params = DatastoreParameters.product("Lettuce").collection(collName).operation(operation).instance(uri.getHost(), Integer.valueOf(uri.getPort())).noDatabaseName().build();
			} else {
				params = DatastoreParameters.product("Lettuce").collection(collName).operation(operation).noInstance().noDatabaseName().noSlowQuery().build();
			}
			NRHolder holder = new NRHolder(name, params);
			NRSubscribeConsumer subscriberConsumer = new NRSubscribeConsumer(holder);

			NRErrorConsumer errorConsumer = new NRErrorConsumer(holder);
			Consumer<SignalType> onFinally = new NRSignalTypeConsumer(holder);
			return result.doOnSubscribe(subscriberConsumer).doOnError(errorConsumer).doFinally(onFinally);
		}
		return result;
	}

	public <T, R> Flux<R> createDissolvingFlux(Supplier<RedisCommand<K, V, T>> commandSupplier)
	{
		Flux<R> result = Weaver.callOriginal();
		RedisCommand<K, V, T> cmd = commandSupplier.get();
		if (cmd != null)
		{
			ProtocolKeyword type = cmd.getType();
			String name = type.name();

			String collName = "?";
			RedisURI uri = null;
			if (StatefulRedisConnectionImpl.class.isInstance(connection))
			{
				StatefulRedisConnectionImpl<K, V> connImpl = (StatefulRedisConnectionImpl<K, V>) connection;
				if (connImpl.redisURI != null) {
					uri = connImpl.redisURI;
				}
			}
			String operation = "UnknownOp";
			ProtocolKeyword t = cmd.getType();
			if ((t != null) && (t.name() != null) && (!t.name().isEmpty())) {
				operation = t.name();
			}
			DatastoreParameters params = null;
			if (uri != null) {
				params = DatastoreParameters.product("Lettuce").collection(collName).operation(operation).instance(uri.getHost(), Integer.valueOf(uri.getPort())).noDatabaseName().build();
			} else {
				params = DatastoreParameters.product("Lettuce").collection(collName).operation("").noInstance().noDatabaseName().noSlowQuery().build();
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
