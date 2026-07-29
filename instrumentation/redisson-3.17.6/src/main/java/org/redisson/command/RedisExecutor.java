package org.redisson.command;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

import org.redisson.client.RedisClient;
import org.redisson.client.RedisConnection;
import org.redisson.client.protocol.RedisCommand;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.NRBiConsumer;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type = MatchType.BaseClass)
public abstract class RedisExecutor<V, R> {
	
	final RedisCommand<V> command = Weaver.callOriginal();

	protected void sendCommand(CompletableFuture<R> attemptPromise, RedisConnection connection) {
		String operationName = Utils.getOperation();
		
		String cmdName = operationName != null ? operationName : command.getName();
		if(cmdName == null || cmdName.isEmpty()) {
			cmdName = "UnknownCommandName";
		}

		String subName = command.getSubName();
		String oName = Utils.getObjectName();
		String collectionName = Utils.typeSet() ? Utils.getType() : "?";
		Segment segment = NewRelic.getAgent().getTransaction().startSegment(cmdName + "-" + collectionName);
		segment.addCustomAttribute("Command", cmdName);
		if(subName != null) {
			segment.addCustomAttribute("Sub", subName);
		}
		if(oName != null && !oName.isEmpty()) segment.addCustomAttribute("Redisson Object Name", oName);
		
		RedisClient client = connection.getRedisClient();
		InetSocketAddress address = client.getAddr();
		String host = null;
		int port = -1;
		
		if(address != null) {
			host = address.getHostName();
			port = address.getPort();
		}
		
		DatastoreParameters dsParams;
		
		DatastoreParameters.InstanceParameter iParams = DatastoreParameters.product("Redisson").collection(collectionName).operation(cmdName);
		if(host != null) {
			dsParams = iParams.instance(host, port).build();
		} else {
			dsParams = iParams.build();
		}
		NRBiConsumer<R> listener = new NRBiConsumer<R>(segment, dsParams);
		attemptPromise.whenComplete(listener);
		Weaver.callOriginal();
	}
}
