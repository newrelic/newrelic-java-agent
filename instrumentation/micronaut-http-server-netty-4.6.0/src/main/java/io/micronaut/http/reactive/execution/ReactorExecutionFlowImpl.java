package io.micronaut.http.reactive.execution;

import java.util.function.BiConsumer;

import org.reactivestreams.Publisher;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.micronaut.netty_45.NRBiConsumerWrapper;

import reactor.core.publisher.Mono;

@Weave
abstract class ReactorExecutionFlowImpl {

	@NewField
	protected Token token = null;

    <K> ReactorExecutionFlowImpl(Publisher<K> value) {
    }

    <K> ReactorExecutionFlowImpl(Mono<K> value) {
    }

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Trace(async = true)
    public void onComplete(BiConsumer<? super Object, Throwable> fn) {
		if(token != null) {
			token.link();
			NRBiConsumerWrapper wrapper = new NRBiConsumerWrapper(fn, token);
			token = null;
			fn = wrapper;
		}
		Weaver.callOriginal();
    }
}
