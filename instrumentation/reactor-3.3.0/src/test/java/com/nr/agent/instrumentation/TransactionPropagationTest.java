package com.nr.agent.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;
import com.nr.instrumentation.reactor.TokenLinkingSubscriber;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.nr.instrumentation.reactor.TokenLinkingSubscriber.tokenLift;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("deprecation")
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"reactor"})
public class TransactionPropagationTest {

    public static final String A_VALUE = "";

    @BeforeClass
    public static void init() {
        Hooks.onEachOperator(TokenLinkingSubscriber.class.getName(), tokenLift());
    }

    @Test
    public void syncPropagationSanityCheck() {
        AtomicBoolean hadTransaction = new AtomicBoolean();
        inTransaction(() ->
                checkTransaction(hadTransaction));
        assertCapturedData(hadTransaction);
    }

    @Test
    public void asyncPropagationSanityCheck() throws InterruptedException {
        AtomicBoolean hadTransaction = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);
        inTransaction(() -> {
            Token token = createToken();
            inAnotherThread(() ->
                    inAnnotatedWithTraceAsync(() -> {
                        token.linkAndExpire();
                        checkTransaction(hadTransaction);
                        done.countDown();
                    })
            );
        });
        done.await();
        assertCapturedData(hadTransaction);
    }

    @Test
    public void testReactorSchedulersInstrumentation() throws InterruptedException {
        AtomicBoolean hadTransaction = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);
        inTransaction(() -> {
            Token token = createToken();
            Schedulers.elastic().schedule(() -> {
//                trace_async(() -> { it is not need as Tasks are instrumented and annotated with @Trace(async = ture)
                token.linkAndExpire();
                checkTransaction(hadTransaction);
                done.countDown();
//                });
            });
        });
        done.await();
        assertCapturedData(hadTransaction);
    }

    @Test
    public void testEmptyMonoOnSuccess() {
        AtomicBoolean hadTransaction = new AtomicBoolean();
        inTransaction(() -> {
            Token token = createToken();
            Mono.empty()
                    .subscribeOn(Schedulers.elastic())
                    .doOnSuccess(v ->
                            checkTransaction(hadTransaction))
                    .subscriberContext(with(token))
                    .block();
            token.expire();
        });
        assertCapturedData(hadTransaction);
    }

    @Test
    public void testEmptyFluxOnComplete() {
        AtomicBoolean hadTransaction = new AtomicBoolean();
        inTransaction(() -> {
            Token token = createToken();
            Flux.empty()
                    .subscribeOn(Schedulers.elastic())
                    .doOnComplete(() ->
                            checkTransaction(hadTransaction))
                    .subscriberContext(with(token))
                    .blockFirst();
            token.expire();
        });
        assertCapturedData(hadTransaction);
    }

    @Test
    public void testMonoOnSuccess() {
        AtomicBoolean hadTransaction = new AtomicBoolean();
        inTransaction(() -> {
            Token token = createToken();
            Mono.just(A_VALUE)
                    .subscribeOn(Schedulers.elastic())
                    .doOnSuccess(v ->
                            checkTransaction(hadTransaction))
                    .subscriberContext(with(token))
                    .block();
            token.expire();
        });
        assertCapturedData(hadTransaction);
    }

    @Test
    public void testMonoRetryOnSuccess() {
        AtomicBoolean hadTransaction = new AtomicBoolean();
        inTransaction(() -> {
            Token token = createToken();
            AtomicBoolean firstCall = new AtomicBoolean(true);
            Mono
                    .create(monoSink ->
                            inAnotherThread(() -> {
                                if (firstCall.getAndSet(false))
                                    monoSink.error(new RuntimeException("failing the first call"));
                                else
                                    monoSink.success(A_VALUE);
                            })
                    )
                    .doOnSuccess(v ->
                            checkTransaction(hadTransaction))
                    .retry(2)
                    .subscriberContext(with(token))
                    .block();
            token.expire();
        });
        assertCapturedData(hadTransaction);
    }

    @Test
    public void testMonoRetryBackoffOnSuccess() {
        AtomicBoolean hadTransaction = new AtomicBoolean();
        inTransaction(() -> {
            Token token = createToken();
            AtomicBoolean firstCall = new AtomicBoolean(true);
            Mono
                    .create(monoSink ->
                            inAnotherThread(() -> {
                                if (firstCall.getAndSet(false))
                                    monoSink.error(new RuntimeException("failing the first call"));
                                else
                                    monoSink.success(A_VALUE);
                            })
                    )
                    .doOnSuccess(v ->
                            checkTransaction(hadTransaction))
                    .retryBackoff(2, Duration.ZERO)
                    .subscriberContext(with(token))
                    .block();
            token.expire();
        });
        assertCapturedData(hadTransaction);
    }

    @Test
    public void testMonoNestedInFlatMap() {
        AtomicBoolean hadTransaction = new AtomicBoolean();
        inTransaction(() -> {
            Token token = createToken();
            Mono.just(A_VALUE)
                    .subscribeOn(Schedulers.elastic())
                    .flatMap(v ->
                            Mono.just(A_VALUE)
                                    .subscribeOn(Schedulers.elastic())
                                    .doOnSuccess(v2 ->
                                            checkTransaction(hadTransaction)))
                    .subscriberContext(with(token))
                    .block();
            token.expire();
        });
        assertCapturedData(hadTransaction);
    }

    @Test(timeout = 10000L)
    public void testLambdaMonoSubscriberOnSuccess() {
        AtomicBoolean hadTransaction = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);
        inTransaction(() -> {
            Token token = createToken();
            Mono.empty()
                    .subscribeOn(Schedulers.elastic())
                    .doOnSuccess(v ->
                            checkTransaction(hadTransaction))

                    // it is not need as LambdaMonoSubscriber instrumentation creates token
                    // and puts it into the context
                    //.subscriberContext(with(token))

                    // Call countDown in onComplete to see that instrumentation code calls original method
                    .subscribe(nil(), nil(), done::countDown);
            await(done);
            token.expire();
        });
        assertCapturedData(hadTransaction);
    }

    @Test(timeout = 10000L)
    public void testLambdaMonoSubscriberOnError() {
        AtomicBoolean hadTransaction = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);
        inTransaction(() -> {
            Token token = createToken();
            Mono.error(new RuntimeException())
                    .subscribeOn(Schedulers.elastic())
                    .doOnError(v ->
                            checkTransaction(hadTransaction))

                    // it is not need as LambdaMonoSubscriber instrumentation creates token
                    // and puts it into the context
                    //.subscriberContext(with(token))

                    // Call countDown in onError to see that instrumentation code calls original method
                    .subscribe(nil(), v -> done.countDown());
            await(done);
            token.expire();
        });
        assertCapturedData(hadTransaction);
    }

    @Test(timeout = 10000L)
    public void testLambdaSubscriberOnComplete() {
        AtomicBoolean hadTransaction = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);
        inTransaction(() -> {
            Token token = createToken();
            Flux.empty()
                    .subscribeOn(Schedulers.elastic())
                    .doOnComplete(() ->
                            checkTransaction(hadTransaction))

                    // it is not need as LambdaSubscriber instrumentation creates token
                    // and puts it into the context
                    //.subscriberContext(with(token))

                    // Call countDown in onComplete to see that instrumentation code calls original method
                    .subscribe(nil(), nil(), done::countDown);
            await(done);
            token.expire();
        });
        assertCapturedData(hadTransaction);
    }

    @Test(timeout = 10000L)
    public void testLambdaSubscriberOnError() {
        AtomicBoolean hadTransaction = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);
        inTransaction(() -> {
            Token token = createToken();
            Flux.error(new RuntimeException())
                    .subscribeOn(Schedulers.elastic())
                    .doOnError(v ->
                            checkTransaction(hadTransaction))

                    // it is not need as LambdaSubscriber instrumentation creates token
                    // and puts it into the context
                    //.subscriberContext(with(token))

                    // Call countDown in onError to see that instrumentation code calls original method
                    .subscribe(nil(), v -> done.countDown());
            await(done);
            token.expire();
        });
        assertCapturedData(hadTransaction);
    }

    @Trace(dispatcher = true)
    public void inTransaction(Runnable actions) {
        actions.run();
    }

    public void inAnotherThread(Runnable runnable) {
        new Thread(runnable).start();
    }

    @Trace(async = true)
    public void inAnnotatedWithTraceAsync(Runnable runnable) {
        runnable.run();
    }

    public Token createToken() {
        return AgentBridge.getAgent().getTransaction(false).getToken();
    }

    public Context with(Token token) {
        return Context.empty().put("newrelic-token", token);
    }

    @Trace
    public void checkTransaction(AtomicBoolean hadTransaction) {
        hadTransaction.set(AgentBridge.getAgent().getTransaction(false) != null);
    }

    private <T> Consumer<T> nil() {
        return v -> {
        };
    }

    private void await(CountDownLatch done) {
        try {
            done.await();
        } catch (InterruptedException ignore) {
        }
    }

    private void assertCapturedData(AtomicBoolean hadTransaction) {
        assertTrue("Did not have transaction", hadTransaction.get());

        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        assertThat("No finished transactions", introspector.getFinishedTransactionCount(),
                is(greaterThan(0)));

        assertThat("Transaction names", introspector.getTransactionNames(), contains(
                "OtherTransaction/Custom/" + getClass().getName() + "/inTransaction"
        ));

        assertThat("Unscoped metrics", introspector.getUnscopedMetrics().keySet(), hasItems(
                "Java/" + getClass().getName() + "/inTransaction",
                "Custom/" + getClass().getName() + "/checkTransaction"
        ));
    }
}
