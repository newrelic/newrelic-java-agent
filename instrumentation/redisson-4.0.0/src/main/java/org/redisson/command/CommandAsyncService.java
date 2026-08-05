package org.redisson.command;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.nr.redisson40.instrumentation.RedissonUtil;
import org.redisson.api.RFuture;
import org.redisson.client.RedisException;
import org.redisson.client.codec.Codec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.connection.ConnectionManager;
import org.redisson.connection.NodeSource;
import org.redisson.misc.CompletableFutureWrapper;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.redisson40.instrumentation.NRBiConsumer;

@Weave(type=MatchType.BaseClass, originalName = "org.redisson.command.CommandAsyncService")
public abstract class CommandAsyncService implements CommandAsyncExecutor {

	final ConnectionManager connectionManager = Weaver.callOriginal();

	public RedisException convertException(ExecutionException ee) {
		RedisException e = Weaver.callOriginal();
		if(e != null) {
			NewRelic.noticeError(e);
		}
		return e;
	}

	@Trace(excludeFromTransactionTrace = true)
	 public <V, R> RFuture<R> async(boolean readOnlyMode, NodeSource source, Codec codec,
	            RedisCommand<V> command, Object[] params, boolean ignoreRedirect, boolean noRetry) {
		RFuture<R> mainPromise = Weaver.callOriginal();
		if (this instanceof CommandBatchService) {
			return mainPromise;
		}
		if(mainPromise instanceof CompletableFutureWrapper) {

			String collection = params.length > 0 ? String.valueOf(params[0]) : "other";

			String operationName = command.getName();


			Segment segment = RedissonUtil.createSegment("Redisson", operationName);

			RedissonUtil.NrRedisUri nrRedisUri = RedissonUtil.extractUri(connectionManager);

			DatastoreParameters dsParams = RedissonUtil.createDatastoreParameters(operationName, nrRedisUri);
			CompletableFutureWrapper<R> promise = (CompletableFutureWrapper<R>)mainPromise;
			NRBiConsumer<R> listener = new NRBiConsumer<R>(segment, dsParams);
			promise.whenComplete(listener);
		}
		return mainPromise;
	}

	@Trace(excludeFromTransactionTrace = true)
	public <T, R> RFuture<R> evalAsync(NodeSource nodeSource, boolean readOnlyMode, Codec codec, RedisCommand<T> evalCommandType,
			String script, List<Object> keys, boolean noRetry, Object... params) {

		RFuture<R> mainPromise = Weaver.callOriginal();
		if(mainPromise instanceof CompletableFutureWrapper) {

			String operationName = evalCommandType.getName();

			Segment segment = RedissonUtil.createSegment("Redisson", operationName);
			RedissonUtil.NrRedisUri nrRedisUri = RedissonUtil.extractUri(connectionManager);

			DatastoreParameters dsParams = RedissonUtil.createDatastoreParameters(operationName, nrRedisUri);

			CompletableFutureWrapper<R> promise = (CompletableFutureWrapper<R>)mainPromise;
			NRBiConsumer<R> listener = new NRBiConsumer<R>(segment, dsParams);
			promise.whenComplete(listener);
		}
		return mainPromise;
	}

}
