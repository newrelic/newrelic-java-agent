package org.redisson.command;

import java.util.concurrent.ExecutionException;

import org.redisson.api.RFuture;
import org.redisson.client.RedisException;
import org.redisson.client.codec.Codec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.connection.ConnectionManager;
import org.redisson.connection.NodeSource;
import org.redisson.liveobject.core.RedissonObjectBuilder;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class CommandAsyncService implements CommandAsyncExecutor {

	@NewField
	public String objectType = null;

	public CommandAsyncService(ConnectionManager connectionManager, RedissonObjectBuilder objectBuilder, RedissonObjectBuilder.ReferenceType referenceType)  {

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

		if(!Utils.typeSet()) {
			if(objectType != null) {
				Utils.setType(objectType);
			} else {
				Utils.setType("?");
			}
		}

		RFuture<R> mainPromise = Weaver.callOriginal();
		
		Utils.unSetOperation();
		Utils.unSetType();
		Utils.unSetObjectName();
		return mainPromise;
	}

}
