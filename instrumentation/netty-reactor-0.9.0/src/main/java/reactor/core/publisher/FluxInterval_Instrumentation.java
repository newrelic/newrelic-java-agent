package reactor.core.publisher;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;

@Weave(originalName = "reactor.core.publisher.FluxInterval")
final class FluxInterval_Instrumentation extends Flux<Long> implements SourceProducer<Long> {

    @Override
    public void subscribe(CoreSubscriber<? super Long> actual) {
        Weaver.callOriginal();
    }

    @Weave(originalName = "reactor.core.publisher.FluxInterval$IntervalRunnable")
    static final class IntervalRunnable_Instrumentation implements Runnable, Subscription,
            InnerProducer<Long> {

        final CoreSubscriber<? super Long> actual = Weaver.callOriginal();

        @Override
        @Trace(async = true)
        public void run() {
            Token token = actual.currentContext().get("newrelic-token");
            token.link();
            System.out.println("LINKING IN RUN");
            Weaver.callOriginal();
        }

        @Override
        public CoreSubscriber<? super Long> actual() {
            return Weaver.callOriginal();
        }

        @Override
        public void request(long l) {
            Weaver.callOriginal();
        }

        @Override
        public void cancel() {
            Weaver.callOriginal();
        }
    }
}
