package org.redisson.command;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RFuture;
import org.redisson.client.RedisException;
import org.redisson.client.codec.Codec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.connection.ConnectionManager;
import org.redisson.connection.NodeSource;
import org.redisson.misc.RPromise;
import org.redisson.misc.RedissonPromise;

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

	public CommandAsyncService(ConnectionManager connectionManager) {

	}


	public <V> RedisException convertException(RFuture<V> RFuture) {
		RedisException e = Weaver.callOriginal();
		if(e != null) {
			NewRelic.noticeError(e);
		}
		return e;
	}

	@Trace
	public boolean await(RFuture<?> RFuture, long timeout, TimeUnit timeoutUnit) {

		return Weaver.callOriginal();
	}

	@Trace(excludeFromTransactionTrace=true)
	public <V, R> void async( boolean readOnlyMode,  NodeSource source,  Codec codec,
             RedisCommand<V> command,  Object[] params,  RPromise<R> mainPromise,  int attempt, 
             boolean ignoreRedirect) {
		String operationName = Utils.getOperation();
		
		String cmdName = operationName != null ? operationName : command.getName();
		if(cmdName == null || cmdName.isEmpty()) {
			cmdName = "UnknownCommandName";
		}

		String subName = command.getSubName();
		
		String oName = Utils.getObjectName();
		
		if(mainPromise instanceof RedissonPromise) {
			
			String collectionName = Utils.typeSet() ? Utils.getType() : objectType != null ? objectType : "?";
			Segment segment = NewRelic.getAgent().getTransaction().startSegment(cmdName + "-" + collectionName);
			segment.addCustomAttribute("Command", cmdName);
			if(subName != null) {
				segment.addCustomAttribute("Sub", subName);
			}
			if(oName != null && !oName.isEmpty()) segment.addCustomAttribute("Redisson Object Name", oName);
			DatastoreParameters dsParams = DatastoreParameters.product("Redisson").collection(collectionName).operation(cmdName).build();
			RedissonPromise<R> promise = (RedissonPromise<R>)mainPromise;
			NRBiConsumer<R> listener = new NRBiConsumer<R>(segment, dsParams);
			promise.onComplete(listener);
		}
		Weaver.callOriginal();
		Utils.unSetOperation();
		Utils.unSetType();
		Utils.unSetObjectName();
	}

}
