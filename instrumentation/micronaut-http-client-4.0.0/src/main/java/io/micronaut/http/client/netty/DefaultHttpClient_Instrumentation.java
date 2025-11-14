package io.micronaut.http.client.netty;

import org.reactivestreams.Publisher;

import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.micronaut.http.client4.MicronautHeaders;
import com.newrelic.instrumentation.micronaut.http.client4.ReactorListener;
import com.newrelic.instrumentation.micronaut.http.client4.ResponseConsumer;
import com.newrelic.instrumentation.micronaut.http.client4.Utils;

import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.Argument;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.sse.Event;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Weave(originalName = "io.micronaut.http.client.netty.DefaultHttpClient")
public abstract class DefaultHttpClient_Instrumentation {

	@Trace(dispatcher = true)
	private <I> Publisher<Event<ByteBuffer<?>>> eventStreamOrError(io.micronaut.http.HttpRequest<I> request, Argument<?> errorType) {
		MicronautHeaders headers = new MicronautHeaders(request);
		NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
		return Weaver.callOriginal();
	}
	
	@Trace
	public <I> Publisher<ByteBuffer<?>> dataStream(io.micronaut.http.HttpRequest<I> request) {
		MicronautHeaders headers = new MicronautHeaders(request);
		NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
		Publisher<ByteBuffer<?>> result =  Weaver.callOriginal();
		boolean isFlux = Utils.isFlux(result);
		boolean isMono = Utils.isMono(result);
		if(isFlux || isMono) {
			HttpParameters params = HttpParameters.library("Micronaut").uri(Utils.getRequestURI(request)).procedure(request.getMethodName()).noInboundHeaders().build();
			Transaction txn = NewRelic.getAgent().getTransaction();
			ReactorListener listener = new ReactorListener(txn, params);
			if(result instanceof Mono) {
				Mono<ByteBuffer<?>> mono = (Mono<ByteBuffer<?>>)result;
				result = mono.doOnSubscribe(listener).doOnCancel(listener).doOnTerminate(listener);
			} else if(result instanceof Flux) {
				Flux<ByteBuffer<?>> flux = (Flux<ByteBuffer<?>>)result;
				result = flux.doOnSubscribe(listener).doOnCancel(listener).doOnTerminate(listener);
			}
		
		}
		return result;
	}
	
	@Trace
	public <I, O, E> Publisher<io.micronaut.http.HttpResponse<O>> exchange(io.micronaut.http.HttpRequest<I> request, Argument<O> bodyType, Argument<E> errorType) {
		MicronautHeaders headers = new MicronautHeaders(request);
		NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
		
		Publisher<io.micronaut.http.HttpResponse<O>> result =  Weaver.callOriginal();
		boolean isFlux = Utils.isFlux(result);
		boolean isMono = Utils.isMono(result);
		if(isFlux || isMono) {
			HttpParameters params = HttpParameters.library("Micronaut").uri(Utils.getRequestURI(request)).procedure(request.getMethodName()).noInboundHeaders().build();
			Transaction txn = NewRelic.getAgent().getTransaction();
			ReactorListener listener = new ReactorListener(txn, params);
			ResponseConsumer respConsumer = new ResponseConsumer(txn);
			if(result instanceof Mono) {
				Mono<io.micronaut.http.HttpResponse<O>> mono = (Mono<io.micronaut.http.HttpResponse<O>>)result;
				result = mono.doOnSubscribe(listener).doOnCancel(listener).doOnSuccess(respConsumer).doOnTerminate(listener);
			} else if(result instanceof Flux) {
				Flux<io.micronaut.http.HttpResponse<O>> flux = (Flux<io.micronaut.http.HttpResponse<O>>)result;
				result = flux.doOnSubscribe(listener).doOnCancel(listener).doOnNext(respConsumer).doOnTerminate(listener);
			}
			
		}
		return result;
	}
	
	@Trace
	public <I> Publisher<io.micronaut.http.HttpResponse<ByteBuffer<?>>> exchangeStream(io.micronaut.http.HttpRequest<I> request) {
		MicronautHeaders headers = new MicronautHeaders(request);
		NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
		
		Publisher<io.micronaut.http.HttpResponse<ByteBuffer<?>>> result =  Weaver.callOriginal();
		boolean isFlux = Utils.isFlux(result);
		boolean isMono = Utils.isMono(result);
		if(isFlux || isMono) {
			HttpParameters params = HttpParameters.library("Micronaut").uri(Utils.getRequestURI(request)).procedure(request.getMethodName()).noInboundHeaders().build();
			Transaction txn = NewRelic.getAgent().getTransaction();
			ReactorListener listener = new ReactorListener(txn, params);
			ResponseConsumer respConsumer = new ResponseConsumer(txn);
			if(result instanceof Mono) {
				Mono<io.micronaut.http.HttpResponse<ByteBuffer<?>>> mono = (Mono<io.micronaut.http.HttpResponse<ByteBuffer<?>>>)result;
				result = mono.doOnSubscribe(listener).doOnCancel(listener).doOnSuccess(respConsumer).doOnTerminate(listener);
			} else if(result instanceof Flux) {
				Flux<io.micronaut.http.HttpResponse<ByteBuffer<?>>> flux = (Flux<io.micronaut.http.HttpResponse<ByteBuffer<?>>>)result;
				result = flux.doOnSubscribe(listener).doOnCancel(listener).doOnNext(respConsumer).doOnTerminate(listener);
			}
			
		}
		return result;
	}
	
	@Trace(dispatcher = true)
	public <I, O> Publisher<O> jsonStream(io.micronaut.http.HttpRequest<I> request, Argument<O> type, Argument<?> errorType) {
		MicronautHeaders headers = new MicronautHeaders(request);
		NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
		Publisher<O> result = Weaver.callOriginal();
		boolean isFlux = Utils.isFlux(result);
		boolean isMono = Utils.isMono(result);
		if(isFlux || isMono) {
			HttpParameters params = HttpParameters.library("Micronaut").uri(Utils.getRequestURI(request)).procedure(request.getMethodName()).noInboundHeaders().build();
			Transaction txn = NewRelic.getAgent().getTransaction();
			ReactorListener listener = new ReactorListener(txn, params);
			
			if(isMono) {
				Mono<O> mono = (Mono<O>)result;
				result = mono.doOnSubscribe(listener).doOnTerminate(listener);
			} else if(isFlux) {
				Flux<O> flux = (Flux<O>)result;
				result = flux.doOnSubscribe(listener).doOnTerminate(listener);
			}
		}
		
		return result;
	}
	
	@Trace(dispatcher = true)
	public Publisher<MutableHttpResponse<?>> proxy(io.micronaut.http.HttpRequest<?> request) {
		MicronautHeaders headers = new MicronautHeaders(request);
		NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
		
		Publisher<MutableHttpResponse<?>> result =  Weaver.callOriginal();
		boolean isFlux = Utils.isFlux(result);
		boolean isMono = Utils.isMono(result);
		if(isFlux || isMono) {
			HttpParameters params = HttpParameters.library("Micronaut").uri(Utils.getRequestURI(request)).procedure(request.getMethodName()).noInboundHeaders().build();
			Transaction txn = NewRelic.getAgent().getTransaction();
			ReactorListener listener = new ReactorListener(txn, params);
			ResponseConsumer respConsumer = new ResponseConsumer(txn);
			if(result instanceof Mono) {
				Mono<MutableHttpResponse<?>> mono = (Mono<MutableHttpResponse<?>>)result;
				result = mono.doOnSubscribe(listener).doOnCancel(listener).doOnSuccess(respConsumer).doOnTerminate(listener);
			} else if(result instanceof Flux) {
				Flux<MutableHttpResponse<?>> flux = (Flux<MutableHttpResponse<?>>)result;
				result = flux.doOnSubscribe(listener).doOnCancel(listener).doOnNext(respConsumer).doOnTerminate(listener);
			}
			
		}
		return result;
	}
	
}
