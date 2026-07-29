package org.redisson.command;

import java.util.concurrent.ExecutionException;

import org.redisson.api.RFuture;
import org.redisson.api.options.ObjectParams;
import org.redisson.client.RedisException;
import org.redisson.client.codec.Codec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.connection.ConnectionManager;
import org.redisson.connection.NodeSource;
import org.redisson.liveobject.core.RedissonObjectBuilder;
import org.redisson.misc.CompletableFutureWrapper;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.NRBiConsumer;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class CommandAsyncService implements CommandAsyncExecutor {

	@NewField
	public String objectType = null;

	protected CommandAsyncService(ConnectionManager connectionManager, RedissonObjectBuilder objectBuilder, RedissonObjectBuilder.ReferenceType referenceType)  {

	}
	
	protected CommandAsyncService(CommandAsyncExecutor executor, boolean trackChanges) {
		
	}

	protected CommandAsyncService(CommandAsyncExecutor executor, ObjectParams objectParams) {
		
	}

	public RedisException convertException(ExecutionException ee) {
		RedisException e = Weaver.callOriginal();
		if(e != null) {
			NewRelic.noticeError(e);
		}
		return e;
	}

	@Trace(excludeFromTransactionTrace=true)
	 public <V, R> RFuture<R> async(boolean readOnlyMode, NodeSource source, Codec codec,
	            RedisCommand<V> command, Object[] params, boolean ignoreRedirect, boolean noRetry) {
		String operationName = Utils.getOperation();
		
		String cmdName = operationName != null ? operationName : command.getName();
		if(cmdName == null || cmdName.isEmpty()) {
			cmdName = "UnknownCommandName";
		}

		String subName = command.getSubName();
		
		String oName = Utils.getObjectName();

		RFuture<R> mainPromise = Weaver.callOriginal();

		if(mainPromise instanceof CompletableFutureWrapper) {
			
			String collectionName = Utils.typeSet() ? Utils.getType() : objectType != null ? objectType : "?";
			Segment segment = NewRelic.getAgent().getTransaction().startSegment(cmdName + "-" + collectionName);
			segment.addCustomAttribute("Command", cmdName);
			if(subName != null) {
				segment.addCustomAttribute("Sub", subName);
			}
			if(oName != null && !oName.isEmpty()) segment.addCustomAttribute("Redisson Object Name", oName);
			DatastoreParameters dsParams = DatastoreParameters.product("Redisson").collection(collectionName).operation(cmdName).build();
			CompletableFutureWrapper<R> promise = (CompletableFutureWrapper<R>)mainPromise;
			NRBiConsumer<R> listener = new NRBiConsumer<R>(segment, dsParams);
			promise.whenComplete(listener);
		}
		Utils.unSetOperation();
		Utils.unSetType();
		Utils.unSetObjectName();
		return mainPromise;
	}

}
