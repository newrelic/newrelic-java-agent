package io.netty.channel;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import io.lettuce.core.protocol.RedisCommand;

@Weave(type=MatchType.BaseClass,originalName="io.netty.channel.AbstractChannel")
public abstract class AbstractChannel_Instrumentation  {

	public abstract ChannelPipeline_Instrumentation pipeline();
	
    public ChannelFuture write(Object msg) {
    	if (msg instanceof RedisCommand) {
			if (pipeline().lettuceLayerToken == null) {
				Token token = NewRelic.getAgent().getTransaction().getToken();
				if (token != null && token.isActive()) {
					pipeline().lettuceLayerToken = token;
				}
			} 
		}
		return Weaver.callOriginal();
    }

    public ChannelFuture write(Object msg, ChannelPromise promise) {
    	if (msg instanceof RedisCommand) {
			if (pipeline().lettuceLayerToken == null) {
				Token token = NewRelic.getAgent().getTransaction().getToken();
				if (token != null && token.isActive()) {
					pipeline().lettuceLayerToken = token;
				}
			} 
		}
    	return Weaver.callOriginal();
    }
    
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
    	if (msg instanceof RedisCommand) {
			if (pipeline().lettuceLayerToken == null) {
				Token token = NewRelic.getAgent().getTransaction().getToken();
				if (token != null && token.isActive()) {
					pipeline().lettuceLayerToken = token;
				}
			} 
		}
    	return Weaver.callOriginal();
    }

    public ChannelFuture writeAndFlush(Object msg) {
    	if (msg instanceof RedisCommand) {
			if (pipeline().lettuceLayerToken == null) {
				Token token = NewRelic.getAgent().getTransaction().getToken();
				if (token != null && token.isActive()) {
					pipeline().lettuceLayerToken = token;
				}
			} 
		}
    	return Weaver.callOriginal();
    }

	
}
