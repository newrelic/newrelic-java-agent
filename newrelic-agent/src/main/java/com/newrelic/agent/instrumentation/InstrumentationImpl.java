/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.RateLimiter;
import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionApiImpl;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.config.ClassTransformerConfig;
import com.newrelic.agent.instrumentation.classmatchers.DefaultClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.HashSafeClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.AccessMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.AndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.GetterSetterMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NameMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NotMethodMatcher;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.agent.profile.v2.TransactionProfileSession;
import com.newrelic.agent.reinstrument.PeriodicRetransformer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ClassMethodSignatures;
import com.newrelic.agent.tracers.DefaultSqlTracer;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootSqlTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.agent.tracers.UltraLightTracer;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormats;
import com.newrelic.agent.util.InsertOnlyArray;
import com.newrelic.api.agent.NewRelic;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.Closeable;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;

import static com.newrelic.agent.Transaction.SCALA_API_TRACER_FLAGS;
import static com.newrelic.agent.Transaction.SCALA_API_TXN_CLASS_SIGNATURE_ID;
import static com.newrelic.agent.Transaction.GENERIC_TXN_CLASS_SIGNATURE_ID;

public class InstrumentationImpl implements Instrumentation {

    private final com.newrelic.api.agent.Logger logger;
    private final InsertOnlyArray<Object> objectCache = new InsertOnlyArray<>(16);
    private final Set<StackTraceElement> instrumentedStackTraceElements = Sets.newConcurrentHashSet();
    final Supplier<Boolean> autoInstrumentCheck;

    public InstrumentationImpl(com.newrelic.api.agent.Logger logger) {
        this(logger, ServiceFactory.getConfigService().getDefaultAgentConfig()
                .getClassTransformerConfig());
    }

    public InstrumentationImpl(com.newrelic.api.agent.Logger logger, ClassTransformerConfig classTransformerConfig) {
        this.logger = logger;
        double rateLimitInSeconds = getAutoAsyncLinkRateLimitInSeconds(classTransformerConfig);
        if (rateLimitInSeconds <= 0) {
            autoInstrumentCheck = () -> false;
        } else {
            final RateLimiter rateLimiter = RateLimiter.create(rateLimitInSeconds);
            autoInstrumentCheck = rateLimiter::tryAcquire;
        }
    }

    static double getAutoAsyncLinkRateLimitInSeconds(ClassTransformerConfig classTransformerConfig) {
        long rateLimitInMillis = classTransformerConfig.getAutoAsyncLinkRateLimit();
        return (double)rateLimitInMillis / (double) TimeUnit.SECONDS.toMillis(1);
    }

    /**
     * Optimized createTracer call for e.g. YML instrumentation
     */
    @Override
    public ExitTracer createTracer(Object invocationTarget, int signatureId, boolean dispatcher, String metricName,
            String tracerFactoryName, Object[] args) {
        if (ServiceFactory.getServiceManager().isStopped()) {
            return null;
        }
        // Avoid creating tracers for NoOpTransaction, etc.
        com.newrelic.agent.Transaction transaction = com.newrelic.agent.Transaction.getTransaction(dispatcher);
        if (transaction == null) {
            return noticeTracer(signatureId, TracerFlags.getDispatcherFlags(dispatcher), null);
        }
        if (ServiceFactory.getServiceManager().getCircuitBreakerService().isTripped()) {
            return null;
        }

        try {
            if (!dispatcher && !transaction.isStarted() && tracerFactoryName == null) {
                // if we're not in a transaction and this isn't a dispatcher tracer, bail before we create objects
                return noticeTracer(signatureId, TracerFlags.getDispatcherFlags(dispatcher), null);
            }

            if (transaction.getTransactionActivity().isLeaf()) {
                return null;
            }

            ClassMethodSignature sig = ClassMethodSignatures.get().get(signatureId);
            return noticeTracer(signatureId, TracerFlags.getDispatcherFlags(dispatcher),
                    transaction.getTransactionState().getTracer(transaction, tracerFactoryName, sig, invocationTarget, args));
        } catch (Throwable t) {
            logger.log(Level.FINEST, t, "createTracer({0}, {1}, {2})", invocationTarget, signatureId, metricName);
            return null;
        }
    }

    /**
     * Optimized createTracer call for weaved and XML instrumentation. We do not know if either a TransactionActivity or
     * a Transaction is present on the thread. If present, we do not know if the Transaction has been started.
     */
    @Override
    public ExitTracer createTracer(Object invocationTarget, int signatureId, String metricName, int flags) {
        try {
            if (ServiceFactory.getServiceManager().isStopped()) {
                return null;
            }
            if (ServiceFactory.getServiceManager().getCircuitBreakerService().isTripped()) {
                return null;
            }

            TransactionActivity txa = TransactionActivity.get();

            if (txa != null) {
                if (txa.getRootTracer() != null && txa.getRootTracer().isAsync() && txa.getTransaction() == null) {
                    txa = null;
                }
            }

            if (!Agent.canFastPath()) { // legacy async instrumentation is in use
                return oldCreateTracer(txa, invocationTarget, signatureId, metricName, flags);
            }

            if (txa == null) {
                AgentBridge.TokenAndRefCount tokenAndRefCount = AgentBridge.activeToken.get();
                if (tokenAndRefCount != null && tokenAndRefCount.token != null) {
                    // Fast path for scala instrumentation (and potentially others in the future)
                    Transaction tx = Transaction.getTransaction(false);
                    if (tx == null) {
                        if (tokenAndRefCount.token.getTransaction() instanceof Transaction) {
                            tx = (Transaction) tokenAndRefCount.token.getTransaction();
                        } else {
                            return null;
                        }
                    }
                    txa = TransactionActivity.create(tx, Integer.MAX_VALUE);
                    flags = flags | TracerFlags.ASYNC;

                    ClassMethodSignature sig = ClassMethodSignatures.get().get(signatureId);
                    MetricNameFormat mnf = MetricNameFormats.getFormatter(invocationTarget, sig, metricName, flags);
                    Tracer tracer;
                    if (TracerFlags.isRoot(flags)) { // Dispatcher || Async
                        tracer = new OtherRootTracer(txa, sig, invocationTarget, mnf, flags);
                    } else {
                        tracer = new DefaultTracer(txa, sig, invocationTarget, mnf, flags);
                    }
                    txa.tracerStarted(tracer);

                    Tracer initiatingTracer = (Tracer) AgentBridge.activeToken.get().tracedMethod.getAndSet(tracer);
                    tx.startFastAsyncWork(txa, initiatingTracer);
                    return noticeTracer(signatureId, flags, tracer);
                } else if (TracerFlags.isDispatcher(flags)) {
                    // Traditional first-time creation of a new transaction
                    com.newrelic.agent.Transaction.getTransaction(true);
                    txa = TransactionActivity.get();
                } else if (TracerFlags.isAsync(flags)) {
                    // Create a transaction activity without a transaction
                    txa = TransactionActivity.create(null, Integer.MAX_VALUE);
                }

                if (txa == null) {
                    // We are neither a Dispatcher nor Async tracer,
                    // or we are running on an Agent thread.
                    return noticeTracer(signatureId, flags, null);
                }
                return noticeTracer(signatureId, flags, startTracer(txa, invocationTarget, signatureId, metricName, flags));
            }

            // There's a TxA on this thread, but it may not be started. It's hard to know
            // how frequently this condition occurs in practice, but for compatibility with
            // traditional tracer creation code, we check it before creating any objects.
            if (!TracerFlags.isRoot(flags) && !txa.isStarted()) {
                return noticeTracer(signatureId, flags, null);
            }

            Tracer result = null;
            if (txa.checkTracerStart()) {

                // Tracer start lock is held:

                try {
                    ClassMethodSignature sig = ClassMethodSignatures.get().get(signatureId);
                    // Metric naming. When we come here from an @Trace that doesn't specify a metric name, a
                    // very common case, getFormatter() will return one of a couple of reusable instances of
                    // MetricNameFormat, so we avoid proliferating small objects. But when we come here from
                    // XML instrumentation, getFormatter() is forced to create a new MNF instance every time.
                    // This proved to be messy to optimize, so unfortunately has been left as-is. 2015-05.
                    MetricNameFormat mnf = MetricNameFormats.getFormatter(invocationTarget, sig, metricName, flags);
                    if (TracerFlags.isDispatcher(flags)
                            || (TracerFlags.isAsync(flags) && txa.getTransaction() != null && !txa.isStarted())) {
                        result = new OtherRootTracer(txa, sig, invocationTarget, mnf);
                    } else {
                        result = new DefaultTracer(txa, sig, invocationTarget, mnf, flags);
                    }
                } finally {
                    txa.unlockTracerStart();
                }

                txa.tracerStarted(result);
            }
            return noticeTracer(signatureId, flags, result);

        } catch (Throwable t) {
            logger.log(Level.FINEST, t, "createTracer({0}, {1}, {2}, {3})", invocationTarget, signatureId, metricName,
                    flags);
            return null;
        }
    }

    // I don't like having this method be a copy/paste of the createTracer method above but I do not
    // want to introduce a performance hit for the default tracer path just to support sql tracers.
    @Override
    public ExitTracer createSqlTracer(Object invocationTarget, int signatureId, String metricName, int flags) {
        try {
            if (ServiceFactory.getServiceManager().isStopped()) {
                return null;
            }
            if (ServiceFactory.getServiceManager().getCircuitBreakerService().isTripped()) {
                return null;
            }

            TransactionActivity txa = TransactionActivity.get();

            if (txa != null) {
                if (txa.getRootTracer() != null && txa.getRootTracer().isAsync() && txa.getTransaction() == null) {
                    txa = null;
                }
            }

            if (!Agent.canFastPath()) { // legacy async instrumentation is in use
                return oldCreateSqlTracer(txa, invocationTarget, signatureId, metricName, flags);
            }

            if (txa == null) {
                AgentBridge.TokenAndRefCount tokenAndRefCount = AgentBridge.activeToken.get();
                if (tokenAndRefCount != null && tokenAndRefCount.token != null) {
                    // Fast path for scala instrumentation (and potentially others in the future)
                    Transaction tx = Transaction.getTransaction(false);
                    if (tx == null) {
                        if (tokenAndRefCount.token.getTransaction() instanceof Transaction) {
                            tx = (Transaction) tokenAndRefCount.token.getTransaction();
                        } else {
                            return null;
                        }
                    }
                    txa = TransactionActivity.create(tx, Integer.MAX_VALUE);
                    flags = flags | TracerFlags.ASYNC;

                    ClassMethodSignature sig = ClassMethodSignatures.get().get(signatureId);
                    MetricNameFormat mnf = MetricNameFormats.getFormatter(invocationTarget, sig, metricName, flags);
                    Tracer tracer;

                    if (TracerFlags.isRoot(flags)) { // Dispatcher || Async
                        tracer = new OtherRootSqlTracer(txa, sig, invocationTarget, mnf, flags);
                    } else if (overSegmentLimit(txa)) {
                        logger.log(Level.FINEST, "Transaction has exceeded tracer segment limit. Returning ultralight sql tracer.");
                        return UltraLightTracer.createClampedSegment(txa, sig);
                    } else {
                        tracer = new DefaultSqlTracer(txa, sig, invocationTarget, mnf, flags);
                    }
                    txa.tracerStarted(tracer);

                    Tracer initiatingTracer = (Tracer) tokenAndRefCount.tracedMethod.getAndSet(tracer);
                    tx.startFastAsyncWork(txa, initiatingTracer);
                    return tracer;
                } else if (TracerFlags.isDispatcher(flags)) {
                    // Traditional first-time creation of a new transaction
                    com.newrelic.agent.Transaction.getTransaction(true);
                    txa = TransactionActivity.get();
                } else if (TracerFlags.isAsync(flags)) {
                    // Create a transaction activity without a transaction
                    txa = TransactionActivity.create(null, Integer.MAX_VALUE);
                }

                if (txa == null) {
                    // We are neither a Dispatcher nor Async tracer,
                    // or we are running on an Agent thread.
                    return null;
                }

                return startSqlTracer(txa, invocationTarget, signatureId, metricName, flags);
            }

            // There's a TxA on this thread, but it may not be started. It's hard to know
            // how frequently this condition occurs in practice, but for compatibility with
            // traditional tracer creation code, we check it before creating any objects.
            if (!TracerFlags.isRoot(flags) && !txa.isStarted()) {
                return null;
            }

            Tracer result = null;
            if (txa.checkTracerStart()) {

                // Tracer start lock is held:

                try {
                    ClassMethodSignature sig = ClassMethodSignatures.get().get(signatureId);
                    // Metric naming. When we come here from an @Trace that doesn't specify a metric name, a
                    // very common case, getFormatter() will return one of a couple of reusable instances of
                    // MetricNameFormat, so we avoid proliferating small objects. But when we come here from
                    // XML instrumentation, getFormatter() is forced to create a new MNF instance every time.
                    // This proved to be messy to optimize, so unfortunately has been left as-is. 2015-05.
                    MetricNameFormat mnf = MetricNameFormats.getFormatter(invocationTarget, sig, metricName, flags);
                    if (TracerFlags.isDispatcher(flags)
                            || (TracerFlags.isAsync(flags) && txa.getTransaction() != null && !txa.isStarted())) {
                        result = new OtherRootSqlTracer(txa, sig, invocationTarget, mnf);
                    } else if (overSegmentLimit(txa)) {
                        logger.log(Level.FINEST, "Transaction has exceeded tracer segment limit. Returning ultralight sql tracer.");
                        result = UltraLightTracer.createClampedSegment(txa, sig);
                    } else {
                        result = new DefaultSqlTracer(txa, sig, invocationTarget, mnf, flags);
                    }
                } finally {
                    txa.unlockTracerStart();
                }

                txa.tracerStarted(result);
            }
            return result;

        } catch (Throwable t) {
            logger.log(Level.FINEST, t, "createTracer({0}, {1}, {2}, {3})", invocationTarget, signatureId, metricName,
                    flags);
            return null;
        }
    }

    @Override
    public ExitTracer createScalaTxnTracer() {
      return createTracer(null, SCALA_API_TXN_CLASS_SIGNATURE_ID, null, SCALA_API_TRACER_FLAGS);
    }

    @Override
    public ExitTracer createTracer(String metricName, int flags) {
        return createTracer(null, GENERIC_TXN_CLASS_SIGNATURE_ID, metricName, flags);
    }

    private boolean overSegmentLimit(TransactionActivity transactionActivity) {
        Transaction transaction;
        if (transactionActivity == null) {
            transaction = Transaction.getTransaction(false);
        } else {
            transaction = transactionActivity.getTransaction();
        }
        return transaction != null && transaction.getTransactionCounts().isOverTracerSegmentLimit();
    }

    private ExitTracer noticeTracer(int signatureId, int tracerFlags, Tracer result) {
        try {
            TransactionProfileSession transactionProfileSession = ServiceFactory.getProfilerService()
                    .getTransactionProfileService()
                    .getTransactionProfileSession();
            if (transactionProfileSession.isActive()) {
                transactionProfileSession.noticeTracerStart(signatureId, tracerFlags, result);
            }
        } catch (Throwable t) {
            logger.log(Level.FINEST, t, "exception in noticeTracer: {0}. This may affect thread profile v2.", result);
        }
        return result;
    }

    // Preconditions are met: start a new tracer on this thread. There may not be a Transaction on this thread. Code
    // in this path MUST NOT attempt to touch the Transaction or do anything that might bring one into existence. This
    // inconvenience is absolutely critical to the performance of our async instrumentation.
    private Tracer startTracer(TransactionActivity txa, Object target, int signatureId, String metricName, int flags) {
        ClassMethodSignature sig = ClassMethodSignatures.get().get(signatureId);
        MetricNameFormat mnf = MetricNameFormats.getFormatter(target, sig, metricName, flags);
        Tracer tracer;
        if (TracerFlags.isRoot(flags)) { // Dispatcher || Async
            tracer = new OtherRootTracer(txa, sig, target, mnf, flags, System.nanoTime());
        } else {
            tracer = new DefaultTracer(txa, sig, target, mnf, flags);
        }
        txa.tracerStarted(tracer);
        return tracer;
    }

    private Tracer startSqlTracer(TransactionActivity txa, Object target, int signatureId, String metricName, int flags) {
        ClassMethodSignature sig = ClassMethodSignatures.get().get(signatureId);
        MetricNameFormat mnf = MetricNameFormats.getFormatter(target, sig, metricName, flags);
        Tracer tracer;
        if (TracerFlags.isRoot(flags)) { // Dispatcher || Async
            tracer = new OtherRootSqlTracer(txa, sig, target, mnf, flags, System.nanoTime());
        } else {
            tracer = new DefaultSqlTracer(txa, sig, target, mnf, flags);
        }
        txa.tracerStarted(tracer);
        return tracer;
    }

    // This code path is similar to the 3.16.1 and earlier tracer creation path. It is retained for use by legacy async
    // instrumentation: Play1 and async servlet 3.0 instrumentation. The key difference from the "fast path" is that
    // this path switches on the TransactionState during creation.
    private ExitTracer oldCreateTracer(TransactionActivity txa, Object invocationTarget, int signatureId,
            String metricName, int flags) {

        // ASSERT: the circuit breaker was checked by the caller and doesn't need to be checked again.

        // If we're here it means that some legacy instrumentation is in play (servlet 3.0, play 1, etc) and the fast
        // path has been disabled. If we're in a code path that has async=true set (and we're not currently in a
        // transaction) we want to create a new transaction activity and start this tracer manually, overriding the
        // oldCreateTracer behavior below.
        if (txa == null) {
            AgentBridge.TokenAndRefCount tokenAndRefCount = AgentBridge.activeToken.get();
            if (tokenAndRefCount != null && tokenAndRefCount.token != null) {
                // Fast path for scala instrumentation (and potentially others in the future)
                Transaction tx = Transaction.getTransaction(false);
                if (tx == null) {
                    if (tokenAndRefCount.token.getTransaction() instanceof Transaction) {
                        tx = (Transaction) tokenAndRefCount.token.getTransaction();
                    } else {
                        return null;
                    }
                }
                txa = TransactionActivity.create(tx, Integer.MAX_VALUE);
                flags = flags | TracerFlags.ASYNC;

                ClassMethodSignature sig = ClassMethodSignatures.get().get(signatureId);
                MetricNameFormat mnf = MetricNameFormats.getFormatter(invocationTarget, sig, metricName, flags);
                Tracer tracer;
                if (TracerFlags.isRoot(flags)) { // Dispatcher || Async
                    tracer = new OtherRootTracer(txa, sig, invocationTarget, mnf, flags);
                } else {
                    tracer = new DefaultTracer(txa, sig, invocationTarget, mnf, flags);
                }
                txa.tracerStarted(tracer);

                Tracer initiatingTracer = (Tracer) tokenAndRefCount.tracedMethod.getAndSet(tracer);
                tx.startFastAsyncWork(txa, initiatingTracer);
                return noticeTracer(signatureId, flags, tracer);
            } else if (TracerFlags.isAsync(flags)) {
                txa = TransactionActivity.create(null, Integer.MAX_VALUE);
                return startTracer(txa, invocationTarget, signatureId, metricName, flags);
            }
        }

        // Avoid creating tracers for NoOpTransaction, etc.
        com.newrelic.agent.Transaction transaction = com.newrelic.agent.Transaction.getTransaction(TracerFlags.isDispatcher(flags));
        if (transaction == null) {
            return null;
        }

        try {
            if (!TracerFlags.isDispatcher(flags) && !transaction.isStarted()) {
                // if we're not in a transaction and this isn't a dispatcher tracer, bail before we create objects
                return noticeTracer(signatureId, flags, null);
            }
            if (transaction.getTransactionActivity().isLeaf()) {
                return null;
            }
            ClassMethodSignature sig = ClassMethodSignatures.get().get(signatureId);
            return transaction.getTransactionState().getTracer(transaction, invocationTarget, sig, metricName, flags);
        } catch (Throwable t) {
            logger.log(Level.FINEST, t, "createTracer({0}, {1}, {2}, {3})", invocationTarget, signatureId, metricName,
                    flags);
            return null;
        }
    }

    // This code path is similar to the 3.16.1 and earlier tracer creation path. It is retained for use by legacy
    // async instrumentation, including NAPS (Netty, Akka, Play, Scala) and async servlet instrumentation.
    private ExitTracer oldCreateSqlTracer(TransactionActivity txa, Object invocationTarget, int signatureId,
            String metricName, int flags) {

        // ASSERT: the circuit breaker was checked by the caller and doesn't need to be checked again.

        if (txa == null) {
            AgentBridge.TokenAndRefCount tokenAndRefCount = AgentBridge.activeToken.get();
            if (tokenAndRefCount != null && tokenAndRefCount.token != null) {
                // Fast path for scala instrumentation (and potentially others in the future)
                Transaction tx = Transaction.getTransaction(false);
                if (tx == null) {
                    if (tokenAndRefCount.token.getTransaction() instanceof Transaction) {
                        tx = (Transaction) tokenAndRefCount.token.getTransaction();
                    } else {
                        return null;
                    }
                }
                txa = TransactionActivity.create(tx, Integer.MAX_VALUE);
                flags = flags | TracerFlags.ASYNC;

                ClassMethodSignature sig = ClassMethodSignatures.get().get(signatureId);
                MetricNameFormat mnf = MetricNameFormats.getFormatter(invocationTarget, sig, metricName, flags);
                Tracer tracer;
                if (TracerFlags.isRoot(flags)) { // Dispatcher || Async
                    tracer = new OtherRootSqlTracer(txa, sig, invocationTarget, mnf, flags);
                } else if (overSegmentLimit(txa)) {
                    logger.log(Level.FINEST, "Transaction has exceeded tracer segment limit. Returning ultralight sql tracer.");
                    return UltraLightTracer.createClampedSegment(txa, sig);
                } else {
                    tracer = new DefaultSqlTracer(txa, sig, invocationTarget, mnf, flags);
                }
                txa.tracerStarted(tracer);

                Tracer initiatingTracer = (Tracer) tokenAndRefCount.tracedMethod.getAndSet(tracer);
                tx.startFastAsyncWork(txa, initiatingTracer);
                return tracer;
            } else if (TracerFlags.isAsync(flags)) {
                txa = TransactionActivity.create(null, Integer.MAX_VALUE);
                return startTracer(txa, invocationTarget, signatureId, metricName, flags);
            }
        }

        // Avoid creating tracers for NoOpTransaction, etc.
        com.newrelic.agent.Transaction transaction = com.newrelic.agent.Transaction.getTransaction(TracerFlags.isDispatcher(flags));
        if (transaction == null) {
            return null;
        }

        try {
            if (!TracerFlags.isDispatcher(flags) && !transaction.isStarted()) {
                // if we're not in a transaction and this isn't a dispatcher tracer, bail before we create objects
                return null;
            }
            if (transaction.getTransactionActivity().isLeaf()) {
                return null;
            }
            ClassMethodSignature sig = ClassMethodSignatures.get().get(signatureId);
            return transaction.getTransactionState().getSqlTracer(transaction, invocationTarget, sig, metricName, flags);
        } catch (Throwable t) {
            logger.log(Level.FINEST, t, "createTracer({0}, {1}, {2}, {3})", invocationTarget, signatureId, metricName,
                    flags);
            return null;
        }
    }

    @Override
    public void noticeInstrumentationError(final Throwable throwable, final String libraryName) {
        if (Agent.LOG.isFinestEnabled()) {
            logger.log(Level.FINEST, throwable, "An error was thrown from instrumentation library {0}", libraryName);
        } else if (Agent.LOG.isFinerEnabled()) {
            logger.log(Level.FINER, "An error was thrown from instrumentation library {0} -- {1}", libraryName,
                    throwable.getMessage());
        }
    }

    @Override
    public void instrument(String className, String metricPrefix) {
        DefaultClassAndMethodMatcher matcher = new HashSafeClassAndMethodMatcher(new ExactClassMatcher(className),
                AndMethodMatcher.getMethodMatcher(new AccessMethodMatcher(Opcodes.ACC_PUBLIC), new NotMethodMatcher(
                        GetterSetterMethodMatcher.getGetterSetterMethodMatcher())));
        ServiceFactory.getClassTransformerService().addTraceMatcher(matcher, metricPrefix);
    }

    @Override
    public void instrument() {
        final boolean check = autoInstrumentCheck.get();
        if (check) {
            final StackTraceElement stackTraceElement = getApplicationStackTraceElement(
                    new Exception().fillInStackTrace().getStackTrace());
            if (stackTraceElement != null) {
                final boolean instrumented = instrument(stackTraceElement, TraceDetailsBuilder.newBuilder().setAsync(true).build());
                NewRelic.recordMetric("Supportability/InstrumentationImpl/instrument", instrumented ? 1f : 0f);
            }
        } else {
            NewRelic.recordMetric("Supportability/InstrumentationImpl/instrument",0f);
        }
    }

    private static StackTraceElement getApplicationStackTraceElement(StackTraceElement[] stackTraces) {
        for (StackTraceElement element : stackTraces) {
            if (!element.getClassName().contains("newrelic")) {
                return element;
            }
        }
        // right here, if we wanted to support this feature for New Relic services, we could try to identify a stack
        // element that does not belong to the agent
        return null;
    }

    private boolean instrument(StackTraceElement stackTraceElement, TraceDetails traceDetails) {
        if (instrumentedStackTraceElements.contains(stackTraceElement)) {
            return false;
        }
        instrumentedStackTraceElements.add(stackTraceElement);
        final DefaultClassAndMethodMatcher matcher = new HashSafeClassAndMethodMatcher(
                new ExactClassMatcher(stackTraceElement.getClassName().replace('.', '/')),
                new NameMethodMatcher(stackTraceElement.getMethodName()));
        boolean shouldRetransform = ServiceFactory.getClassTransformerService().addTraceMatcher(matcher, traceDetails);
        if (shouldRetransform) {
            logger.log(Level.FINE, "Retransforming {0}.{1} for instrumentation.", stackTraceElement.getClassName(), stackTraceElement.getMethodName());
            try {
                PeriodicRetransformer.INSTANCE.queueRetransform(ImmutableSet.of(ClassLoader.getSystemClassLoader().loadClass(stackTraceElement.getClassName())));
                logger.log(Level.FINE, "Retransformed {0}", stackTraceElement.getClassName());
            } catch (ClassNotFoundException e) {
                // the system classloader may not be able to see the class - try to find the class in loaded classes
                queueRetransform(stackTraceElement.getClassName());
            }
        }
        return shouldRetransform;
    }

    private static void queueRetransform(String... classNames) {
        final Set<Class<?>> classesToRetransform = findClasses(false, classNames);
        if (!classesToRetransform.isEmpty()) {
            PeriodicRetransformer.INSTANCE.queueRetransform(classesToRetransform);
        }
    }

    static Set<Class<?>> findClasses(boolean findAll, String... classNames) {
        final Set<String> nameSet = new HashSet<>(Arrays.asList(classNames));
        final Set<Class<?>> found = new HashSet<>();
        final Predicate<String> match = findAll ? nameSet::contains : nameSet::remove;
        for (Class clazz : ServiceFactory.getCoreService().getInstrumentation().getAllLoadedClasses()) {
            if (match.test(clazz.getName())) {
                found.add(clazz);
            }
            if (!findAll && nameSet.isEmpty()) {
                return found;
            }
        }
        return found;
    }

    @Override
    public void instrument(Method methodToInstrument, String metricPrefix) {
        instrument(methodToInstrument, TraceDetailsBuilder.newBuilder().setMetricPrefix(metricPrefix).build());
    }

    private void instrument(Method methodToInstrument, TraceDetails traceDetails) {
        if (methodToInstrument.isAnnotationPresent(InstrumentedMethod.class)) {
            return;
        }
        if (OptimizedClassMatcher.METHODS_WE_NEVER_INSTRUMENT.contains(org.objectweb.asm.commons.Method.getMethod(methodToInstrument))) {
            return;
        }
        int modifiers = methodToInstrument.getModifiers();
        if (Modifier.isNative(modifiers) || Modifier.isAbstract(modifiers)) {
            return; // Can't instrument, so don't bother.
        }

        Class<?> declaringClass = methodToInstrument.getDeclaringClass();
        DefaultClassAndMethodMatcher matcher = new HashSafeClassAndMethodMatcher(new ExactClassMatcher(
                declaringClass.getName()), new ExactMethodMatcher(methodToInstrument.getName(),
                Type.getMethodDescriptor(methodToInstrument)));
        boolean shouldRetransform = ServiceFactory.getClassTransformerService().addTraceMatcher(matcher, traceDetails);
        if (shouldRetransform) {
            logger.log(Level.FINE, "Retransforming {0} for instrumentation.", methodToInstrument);
            PeriodicRetransformer.INSTANCE.queueRetransform(Sets.<Class<?>>newHashSet(declaringClass));
        }
    }

    @Override
    public void retransformUninstrumentedClass(Class<?> classToRetransform) {
        if (!classToRetransform.isAnnotationPresent(InstrumentedClass.class)) {
            retransformClass(classToRetransform);
        } else {
            logger.log(Level.FINER, "Class ", classToRetransform, " already instrumented.");
        }
    }

    private void retransformClass(Class<?> classToRetransform) {
        try {
            ServiceFactory.getCoreService().getInstrumentation().retransformClasses(classToRetransform);
        } catch (UnmodifiableClassException e) {
            logger.log(Level.FINE, "Unable to retransform class ", classToRetransform, " : ", e.getMessage());
        } catch (Throwable t) {
            logger.log(Level.FINE, "An exception occurred while retransforming class ", classToRetransform, " : ",
                    t.getMessage());
        }
    }

    @Override
    public Class<?> loadClass(ClassLoader classLoader, Class<?> theClass) throws ClassNotFoundException {
        logger.log(Level.FINE, "Loading class ", theClass.getName(), " using class loader ", classLoader.toString());

        try {
            return classLoader.loadClass(theClass.getName());
        } catch (ClassNotFoundException e) {
            logger.log(Level.FINEST, "Unable to load {0}", theClass.getName());
        }

        throw new ClassNotFoundException("Unable to load " + theClass.getName());
    }

    @Override
    public com.newrelic.agent.bridge.Transaction getTransaction() {
        try {
            // Bring the transaction into existence on the thread if it doesn't exist and this is not an Agent thread.
            // If this succeeds, return the standard unbound wrapper to the Transaction instance on this thread. If not,
            // (e.g. on an Agent thread), return something so callers don't have to around a null value.
            com.newrelic.agent.Transaction innerTx = com.newrelic.agent.Transaction.getTransaction(true);
            if (innerTx != null) {
                return TransactionApiImpl.INSTANCE;
            }
        } catch (Throwable t) {
            logger.log(Level.FINE, t, "Unable to get transaction, using no-op transaction instead");
        }

        return NoOpTransaction.INSTANCE;
    }

    @Override
    public com.newrelic.agent.bridge.Transaction getTransactionOrNull() {
        try {
            // If there is already a transaction on the thread, return the unbound wrapper so the caller can
            // grab it. If not, just return null. Caller must check. JAVA-2675.
            com.newrelic.agent.Transaction innerTx = com.newrelic.agent.Transaction.getTransaction(false);
            if (innerTx != null) {
                return TransactionApiImpl.INSTANCE;
            }
        } catch (Throwable t) {
            logger.log(Level.FINE, t, "getTransactionOrNull");
        }

        return null;
    }

    @Override
    public int addToObjectCache(Object object) {
        return objectCache.add(object);
    }

    @Override
    public Object getCachedObject(int id) {
        return objectCache.get(id);
    }

    @Override
    public void registerCloseable(String instrumentationName, Closeable closeable) {
        if (instrumentationName != null && closeable != null) {
            ServiceFactory.getClassTransformerService().getContextManager().getClassWeaverService().registerInstrumentationCloseable(
                    instrumentationName, closeable);
        }
    }
}
