package com.nr.agent.mongo;

import com.mongodb.connection.ServerDescription;
import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.binding.AsyncConnectionSource;

public class NRConnectionCallback implements SingleResultCallback<AsyncConnectionSource> {
	
	private String host = null;
	
	private Integer port = null;
	

	@Override
	public void onResult(AsyncConnectionSource result, Throwable t) {

		ServerDescription serverDesc = result.getServerDescription();
		host = serverDesc.getAddress().getHost();
		port = serverDesc.getAddress().getPort();
	}

	public String getHost() {
		return host;
	}
	
	public Integer getPort() {
		return port;
	}
}
