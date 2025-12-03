/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import com.newrelic.agent.ThreadService.AgentThread;
import com.newrelic.agent.application.ApplicationNamingPolicy;
import com.newrelic.agent.application.HigherPriorityApplicationNamingPolicy;
import com.newrelic.agent.application.PriorityApplicationName;
import com.newrelic.agent.application.SameOrHigherPriorityApplicationNamingPolicy;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.CrossProcessState;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.NoOpToken;
import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.bridge.WebResponse;
import com.newrelic.agent.browser.BrowserTransactionState;
import com.newrelic.agent.browser.BrowserTransactionStateImpl;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.CrossProcessConfig;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.CachingDatabaseStatementParser;
import com.newrelic.agent.database.DatabaseStatementParser;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.dispatchers.WebRequestDispatcher;
import com.newrelic.agent.errors.ErrorAnalyzer;
import com.newrelic.agent.errors.ErrorAnalyzerImpl;
import com.newrelic.agent.messaging.MessagingUtil;
import com.newrelic.agent.model.TimeoutCause;
import com.newrelic.agent.normalization.Normalizer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceUtils;
import com.newrelic.agent.sql.SlowQueryListener;
import com.newrelic.agent.stats.AbstractMetricAggregator;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionGuidFactory;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ClassMethodSignatures;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.agent.tracers.TransactionActivityInitiator;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracing.DistributedTracePayloadImpl;
import com.newrelic.agent.tracing.DistributedTraceService;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.tracing.Sampled;
import com.newrelic.agent.tracing.SpanProxy;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.transaction.TransactionCache;
import com.newrelic.agent.transaction.TransactionCounts;
import com.newrelic.agent.transaction.TransactionErrorTracker;
import com.newrelic.agent.transaction.TransactionErrorTrackerImpl;
import com.newrelic.agent.transaction.TransactionNamingPolicy;
import com.newrelic.agent.transaction.TransactionNamingScheme;
import com.newrelic.agent.transaction.TransactionNamingUtility;
import com.newrelic.agent.transaction.TransactionThrowable;
import com.newrelic.agent.transaction.TransactionTimer;
import com.newrelic.agent.util.LazyAtomicReference;
import com.newrelic.agent.util.Strings;
import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.DistributedTracePayload;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.Logs;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.security.schema.SecurityMetaData;
import com.newrelic.api.agent.TransportType;
import org.objectweb.asm.Opcodes;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import static com.newrelic.agent.TransactionStaticsHolder.ASYNC_TIMEOUT_NANO;
import static com.newrelic.agent.TransactionStaticsHolder.ASYNC_TIMEOUT_SECONDS;
import static com.newrelic.agent.TransactionStaticsHolder.SEGMENT_TIMEOUT_MILLIS;
import static com.newrelic.agent.tracing.DistributedTraceUtil.isSampledPriority;

/**
 * Represents a single transaction in the instrumented application. A transaction is a single top-level activity, such
 * as servicing a single web request or a large-grained unit of background work.<br/>
 * <br/>
 * Transactions are composed of one or more per-task work units, each of which is represented by a TransactionActivity
 * object. The Transaction and the TransactionActivity are each referenced by a thread local variable.<br/>
 * <br/>
 * Transactions are created by instrumented method invocations. Creation of the Transaction always creates a
 * TransactionActivity. If an instrumented thread declares itself asynchronous, its Transaction is discarded and its
 * TransactionActivity becomes associated with the top-level Transaction that originated it.<br/>
 * <br/>
 * TransactionActivities are closed when the last tracer on their call stack is popped off. Transactions are closed when
 * their last child TransactionActivity is finished and no further activities are pending.<br/>
 * <br/>
 * This class is thread-safe.
 */
public class Transaction {
    static final ClassMethodSignature REQUEST_INITIALIZED_CLASS_SIGNATURE = new ClassMethodSignature(
            "javax.servlet.ServletRequestListener", "requestInitialized", "(Ljavax/servlet/ServletRequestEvent;)V");
    static final int REQUEST_INITIALIZED_CLASS_SIGNATURE_ID = ClassMethodSignatures.get().add(REQUEST_INITIALIZED_CLASS_SIGNATURE);
    static final ClassMethodSignature SCALA_API_TXN_CLASS_SIGNATURE = new ClassMethodSignature(
    "newrelic.scala.api.TraceOps$", "txn", null);
    public static final int SCALA_API_TXN_CLASS_SIGNATURE_ID =
    ClassMethodSignatures.get().add(SCALA_API_TXN_CLASS_SIGNATURE);
    private static final String THREAD_ASSERTION_FAILURE = "Thread assertion failed!";

    private static final ThreadLocal<Transaction> transactionHolder = new ThreadLocal<>();

    private static volatile DatabaseStatementParser databaseStatementParser;

    // (1) State that is final, so synchronization is not required (and in the
    // case of references, either the referenced
    // object is known to be threadsafe, or the object is thread-confined and
    // surrounding logic enforces its single-threadedness.)
    //
    // Caution: since the integration of the fix for JAVA-1088, Transaction
    // objects are often created on threads a very
    // long time before the transaction actually starts. Final variables are
    // initialized at object creation time, not
    // at transaction start time. This means, among other things, that
    // timing-related information cannot be captured
    // at object creation time; such variables must be volatile and must be
    // captured at transaction start time.

    private final String guid;
    private final boolean ttEnabled;
    private final TransactionCounts counts;
    private final boolean autoAppNamingEnabled;
    private final boolean transactionNamingEnabled;
    private final boolean ignoreErrorPriority;
    private final Object lock = new Object();
    private final Set<TransactionActivity> finishedChildren = Sets.newConcurrentHashSet();
    private final AtomicInteger nextActivityId = new AtomicInteger(0);
    private final long initiatingThreadId;
    private final AtomicReference<Float> priority = new AtomicReference<>(null);
    private final TransactionErrorTracker errorTracker = new TransactionErrorTrackerImpl();

    // Non-final state - synchronization required

    // (2) State that is guarded by using concurrent collections

    // Agent internal parameters
    private final Map<String, Object> internalParameters;

    // these will eventually go into the agent attributes map - however they go
    // in here first so that we can append
    // prefixes during harvest instead of during user's thread
    // key: prefix to be appended value: the map of attributes which need to get
    // the prefixes
    private final Map<String, Map<String, String>> prefixedAgentAttributes;

    // attributes set by the agent, these are user configurable
    private final Map<String, Object> agentAttributes;

    // these fundamental for the UI to work, users cannot configure them - they
    // go to errors and tts
    private final Map<String, Object> intrinsicAttributes;

    // attributes added by the user, these are user configurable
    private final Map<String, Object> userAttributes;

    // attributes added by the user for errors only, these are user configurable
    private final Map<String, Object> errorAttributes;

    // Insights events added by the user during this transaction
    private final AtomicReference<Insights> insights;

    // Log events added by the user during this transaction
    private final AtomicReference<Logs> logEvents;

    // contains all work currently running
    private final Map<Integer, TransactionActivity> runningChildren;

    // (3) State that is guarded using volatility or atomics. Reference objects must be threadsafe.

    private volatile long wallClockStartTimeMs;
    private volatile long startGCTimeInMillis;
    private volatile boolean ignore;
    private volatile boolean ignoreErrors = false;
    private volatile Dispatcher dispatcher;
    private volatile Tracer rootTracer;
    private volatile TransactionTimer transactionTime;
    private volatile TransactionState transactionState = new TransactionStateImpl();
    private volatile TransactionActivity initialActivity = null;
    private volatile PriorityTransactionName priorityTransactionName = PriorityTransactionName.NONE;
    private volatile TimeoutCause timeoutCause;

    // This is only used if we reach our tracer limit or we are not recording
    // transaction activities
    private volatile TransactionStats txStats = null;

    // The appNameAndConfig combo has a deceptively complex behavior: we want both threadsafe lazy
    // initialization with lock-free updates. Setters must have priority over initialization.

    private Callable<AppNameAndConfig> appNameAndConfigInitializer = AppNameAndConfig::getDefault;
    private LazyAtomicReference<AppNameAndConfig> appNameAndConfig = new LazyAtomicReference<>(appNameAndConfigInitializer);

    // (4) State that is guarded using the lock. Referenced objects must be threadsafe.

    private volatile CrossProcessTransactionState crossProcessTransactionState;
    private String normalizedUri;
    private SlowQueryListener slowQueryListener;
    private BrowserTransactionState browserTransactionState;
    private InboundHeaders providedHeaders;
    private volatile InboundHeaderState inboundHeaderState;

    // cache for this transactions tokens, which allows for timing out tokens with custom on-removal logic
    private final AtomicReference<TimedSet<TokenImpl>> activeTokensCache;

    // count of active tokens and tracers
    private final AtomicInteger activeCount;

    private final SecurityMetaData securityMetaData;

    private final MetricAggregator metricAggregator = new AbstractMetricAggregator() {
        @Override
        protected void doRecordResponseTimeMetric(String name, long totalTime, long exclusiveTime, TimeUnit timeUnit) {
            getTransactionActivity().getTransactionStats()
                    .getUnscopedStats()
                    .getOrCreateResponseTimeStats(name)
                    .recordResponseTime(totalTime, exclusiveTime, timeUnit);
        }

        @Override
        protected void doRecordMetric(String name, float value) {
            getTransactionActivity().getTransactionStats().getUnscopedStats().getStats(name).recordDataPoint(value);
        }

        @Override
        protected void doIncrementCounter(String name, int count) {
            getTransactionActivity().getTransactionStats().getUnscopedStats().getStats(name).incrementCallCount(count);
        }
    };

    private static DummyTransaction dummyTransaction;

    private final AtomicReference<SpanProxy> spanProxy = new AtomicReference<>(new SpanProxy());

    private volatile long transportDurationInMillis = -1;
    private volatile long largestTransportDurationInMillis = -1;
    private volatile TransportType transportType = TransportType.Unknown;

    public void setTimeoutCause(TimeoutCause cause) {
        this.timeoutCause = cause;
    }

    public TimeoutCause getTimeoutCause() {
        return timeoutCause;
    }

    public long getTransportDurationInMillis() {
        return Math.max(transportDurationInMillis, 0);
    }

    // WARNING: Mutates this instance by mutating the span proxy
    public DistributedTracePayloadImpl createDistributedTracePayload(String spanId) {
        assignPriorityRootIfNotSet();
        SpanProxy spanProxy = this.spanProxy.get();
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - this.getTransactionTimer().getStartTimeInNanos());
        long txnStartTimeSinceEpochInMillis = System.currentTimeMillis() - elapsedMillis;
        spanProxy.setTimestamp(txnStartTimeSinceEpochInMillis);
        DistributedTracePayloadImpl payload = (DistributedTracePayloadImpl) spanProxy
                .createDistributedTracePayload(priority.get(), spanId, getGuid());

        if (payload != null) {
            this.setPriorityIfNotNull(payload.priority);
        }

        return payload;
    }

    public boolean acceptDistributedTracePayload(String payload) {
        if (getAgentConfig().getDistributedTracingConfig().isEnabled()) {
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - this.getTransactionTimer().getStartTimeInNanos());
            long txnStartTimeSinceEpochInMillis = System.currentTimeMillis() - elapsedMillis;
            spanProxy.get().setTimestamp(txnStartTimeSinceEpochInMillis);
            boolean accepted = spanProxy.get().acceptDistributedTracePayload(payload);
            if (accepted) {
                this.transportDurationInMillis = spanProxy.get().getTransportDurationInMillis();
                this.setPriorityIfNotNull(spanProxy.get().getInboundDistributedTracePayload().priority);
            }
            return accepted;
        } else {
            Agent.LOG.log(Level.FINE, "Not accepting payload, distributed tracing disabled");
            return false;
        }
    }

    public boolean acceptDistributedTracePayload(DistributedTracePayload payload) {
        if (getAgentConfig().getDistributedTracingConfig().isEnabled()) {
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - this.getTransactionTimer().getStartTimeInNanos());
            long txnStartTimeSinceEpochInMillis = System.currentTimeMillis() - elapsedMillis;
            spanProxy.get().setTimestamp(txnStartTimeSinceEpochInMillis);
            boolean accepted = spanProxy.get().acceptDistributedTracePayload(payload);
            if (accepted) {
                this.transportDurationInMillis = spanProxy.get().getTransportDurationInMillis();
                this.setPriorityIfNotNull(spanProxy.get().getInboundDistributedTracePayload().priority);
            }
            return accepted;
        } else {
            Agent.LOG.log(Level.FINE, "Not accepting payload, distributed tracing disabled");
            return false;
        }
    }

    /**
     * Assigns priority to this transaction using sampling and priority data from a remote parent.
     *
     * There are two kinds of parent data that are used:
     * - The remote parent sampled flag, which indicates whether the remote parent is sampled or not. This
     * determines which parent sampler (remote_parent_sampled or remote_parent_not_sampled) to use when making the priority assignment.
     * - Inbound priority data, which is taken from the span proxy's inbound payload if available. This
     * will be used by the adaptive sampler if configured, and ignored by all other sampler types.
     *
     * @param remoteParentSampled whether the remote parent was sampled or not
     */
    public void assignPriorityFromRemoteParent(boolean remoteParentSampled) {
        DistributedTraceService dtService = ServiceFactory.getDistributedTraceService();
        float priority = dtService.calculatePriorityRemoteParent(this, remoteParentSampled, getPriorityFromInboundSamplingDecision());
        this.priority.set(priority);
    }

    /**
     * Assigns priority to this transaction (unless previously assigned) without any information from a remote parent.
     *
     * If Distributed Tracing is enabled, and no priority has been set on this transaction, the configured root sampler
     * will be used to obtain a priority for this transaction. No inbound priority data is read (because if an inbound
     * payload was processed, it should have made a priority assignment earlier).
     *
     * If a priority assignment has already been made, this call is ignored. This is a required check to avoid
     * overwriting any priority decision that was made earlier, either because a remote parent was processed or a previous
     * call to this method was made earlier in the txn's lifecycle. It is also required to avoid accidentally running the
     * adaptive sampler twice on the same transaction.
     *
     * If Distributed Tracing is not enabled, or this is a synthetic transaction, a random priority in [0,1) is assigned.
     */
    public void assignPriorityRootIfNotSet(){
        if (getAgentConfig().getDistributedTracingConfig().isEnabled()){
            if (priority.get() == null){
                //The "if" check above is required even though we do compareAndSet(null) below.
                //Its purpose is to avoid running the sampler more than once for the same txn.
                Float samplerPriority = ServiceFactory.getDistributedTraceService().calculatePriorityRoot(this);
                priority.compareAndSet(null, samplerPriority);
            }
        } else {
            priority.compareAndSet(null, DistributedTraceServiceImpl.nextTruncatedFloat());
        }
    }

    /***
     * Retrieve priority from the inbound payload (using both sampling and priority-related information).
     *
     * First, check to see if there is a sampling decision available on the inbound payload.
     * - If there is a sampling decision, use the inbound priority if it exists or compute a new priority.
     * - If there is no sampling decision, return null.
     *
     * In the case of W3C headers, this is distinct from the remoteParentSampled decision we get from the trace parent header.
     * The sampling and priority values in the payload come from the trace state header (and they may be missing, even if we got a sampled
     * flag on the trace parent header).
     *
     * @return a float in [0, 2) if priority-related information was found, or null if a new decision needs to be made
     */

    @VisibleForTesting
    protected Float getPriorityFromInboundSamplingDecision(){
        DistributedTracePayloadImpl payload = spanProxy.get().getInboundDistributedTracePayload();
        if (payload != null && payload.sampled != Sampled.UNKNOWN) {
            if (payload.priority != null) {
                return payload.priority;
            } else {
                return (payload.sampled.booleanValue() ? 1.0f : 0.0f) + DistributedTraceServiceImpl.nextTruncatedFloat();
            }
        }
        return null;
    }

    public TransportType getTransportType() {
        return transportType;
    }

    public long getLargestTransportDurationInMillis() {
        return Math.max(largestTransportDurationInMillis, 0);
    }

    public SpanProxy getSpanProxy() {
        return spanProxy.get();
    }

    public String getOrCreateTraceId() {
        SpanProxy spanProxy = getSpanProxy();
        return spanProxy.getOrCreateTraceId();
    }

    /**
     * This class is used to allow appName and config to be atomically updated while keeping them linked together. Since
     * the name is immutable and the binding from a name to a config is invariant at runtime, we can get the config
     * lazily without locking.
     */
    private static class AppNameAndConfig {
        private final PriorityApplicationName appName;
        private volatile AgentConfig appConfig;

        AppNameAndConfig(PriorityApplicationName name) {
            this(name, null);
        }

        AppNameAndConfig(PriorityApplicationName name, AgentConfig config) {
            this.appName = name;
            this.appConfig = config;
        }

        PriorityApplicationName getName() {
            return appName;
        }

        AgentConfig getConfig() {
            if (this.appConfig == null) {
                // Benign race here: multiple threads may reach this point and
                // call into the service.
                // But all of them will produce the same result. Eventually one
                // will cache it in the
                // volatile and further calls to the service will not occur for
                // the life of this instance.
                this.appConfig = ServiceFactory.getConfigService().getAgentConfig(appName.getName());
            }
            return this.appConfig;
        }

        /**
         * Get a default instance representing the default configuration and the default name found there.
         *
         * @return an instance wrapped around the current default configuration. We don't cache the default instance
         * because we presume the default configuration can change during an Agent run. The default must have
         * the lowest possible naming priority and must not be installed into a transaction instance if the app
         * name has been set by any other code path.
         */
        public static AppNameAndConfig getDefault() {
            AgentConfig defaultConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
            String appName = defaultConfig.getApplicationName();
            PriorityApplicationName pan = PriorityApplicationName.create(appName, ApplicationNamePriority.NONE);
            return new AppNameAndConfig(pan, defaultConfig);
        }
    }

    // Note: Transaction can be created only via Transaction.getTransaction() or
    // Transaction.getTransaction(true).
    // Calling the constructor is not sufficient to completely initialize the
    // object. And to repeat a warning above:
    // construction of this object can occur a very long time before a
    // transaction begins, so timing-related values
    // (among other things?) must not be recorded here; see JAVA-1088 and its
    // related commits.

    protected Transaction() {
        Agent.LOG.log(Level.FINE, "create Transaction {0}", this);
        if (Agent.LOG.isFinestEnabled() && Agent.isDebugEnabled()) {
            Agent.LOG.log(Level.FINEST, "backtrace: {0}", Arrays.toString(Thread.currentThread().getStackTrace()));
        }

        AgentConfig defaultConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        guid = TransactionGuidFactory.generate16CharGuid();
        autoAppNamingEnabled = defaultConfig.isAutoAppNamingEnabled();
        transactionNamingEnabled = initializeTransactionNamingEnabled(defaultConfig);
        ignoreErrorPriority = defaultConfig.getErrorCollectorConfig().isIgnoreErrorPriority();
        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();
        ttEnabled = ttService.isEnabled();
        counts = new TransactionCounts(defaultConfig);
        initiatingThreadId = Thread.currentThread().getId();

        // does not permit null keys or values
        MapMaker factory = new MapMaker().initialCapacity(8).concurrencyLevel(4);
        internalParameters = new LazyMapImpl<>(factory);
        prefixedAgentAttributes = new LazyMapImpl<>(factory);
        agentAttributes = new LazyMapImpl<>(factory);
        intrinsicAttributes = new LazyMapImpl<>(factory);
        userAttributes = new LazyMapImpl<>(factory);
        errorAttributes = new LazyMapImpl<>(factory);
        insights = new AtomicReference<>();
        logEvents = new AtomicReference<>();
        runningChildren = new LazyMapImpl<>(factory);
        activeTokensCache = new AtomicReference<>();
        activeCount = new AtomicInteger(0);
        securityMetaData = new SecurityMetaData();
    }

    // This method must be called after construction. This is ugly, but avoids
    // an even uglier early publication of the
    // Transaction object to the constructor of the TransactionActivity and
    // resulting reliance on the
    // partially-initialized object. The setter on the thread local ensures this
    // method has been called.
    //
    // All TransactionActivity objects are created here. Initially, the object
    // is keyed by itself. This is intentional
    // and prevents the initial activity (or sole activity, if the transaction is
    // single-threaded) from being ignored
    // via the published Transaction.ignoreAsyncActivity(key) method, since the
    // key is inaccessible to customer code.
    // If the activity is later started via Transaction.startAsyncActivity(),
    // the key will be entered into the maps
    // of its new owning transaction using the context key under which it was
    // registered.
    private void postConstruct() {
        this.initialActivity = TransactionActivity.create(this, nextActivityId.getAndIncrement());;
    }

    private static long getGCTime() {
        long gcTime = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcTime += gcBean.getCollectionTime();
        }
        return gcTime;
    }

    private boolean initializeTransactionNamingEnabled(AgentConfig config) {
        if (!config.isAutoTransactionNamingEnabled()) {
            return false;
        }
        return true;
    }

    public MetricAggregator getMetricAggregator() {
        return metricAggregator;
    }

    public long getInitiatingThreadId() {
        return initiatingThreadId;
    }

    /**
     * Get the lock on the object. This allows classes that interact heavily with the Transaction, e.g.
     * CrossProcessTransactionStateImpl, to avoid the deadlock scenarios that would creep in if they defined their own
     * locks. The consequence is possible making the Transaction's lock a hot lock. Please do not add this method to any
     * interfaces. The fact that it's public is bad enough, but it must be for the sake of a badly-designed unit test.
     *
     * @return this object's lock
     */
    public Object getLock() {
        return lock;
    }

    /**
     * Return the GUID for this transaction.
     *
     * @return the GUID for this transaction.
     */
    public String getGuid() {
        return guid;
    }

    /**
     * Return the config for the current application name. Lazily but atomically initializes the default config if
     * necessary.
     */
    public AgentConfig getAgentConfig() {
        return appNameAndConfig.get().getConfig();
    }

    /**
     * The start time of this transaction (wall clock time). More precisely, the earlier of the time of the first
     * request for this value and the actual start time of the transaction. <br>
     *
     * Rationale: code in the Agent (or at least in the unit tests) sometimes queries this value after this object is
     * created, but before the transaction is actually "started" (as we define "started"). If this happens, we snapshot
     * the current time in order to ensure we give a consistent start time for this transaction.
     */
    public long getWallClockStartTimeMs() {
        captureWallClockStartTime();
        return wallClockStartTimeMs;
    }

    private void captureWallClockStartTime() {
        if (wallClockStartTimeMs == 0) {
            wallClockStartTimeMs = System.currentTimeMillis();
        }
    }

    /**
     * Parameters used for internal agent processing. Internal parameters are never sent to the server.
     */
    public Map<String, Object> getInternalParameters() {
        return internalParameters;
    }

    public boolean markFirstByteOfResponse(long endTimeNs) {
        return transactionTime.markTimeToFirstByte(endTimeNs);
    }

    public boolean markLastByteOfResponse(long endTimeNs) {
        return transactionTime.markTimeToLastByte(endTimeNs);
    }

    /**
     * The HTTP request parameters.
     */
    public Map<String, Map<String, String>> getPrefixedAgentAttributes() {
        return prefixedAgentAttributes;
    }

    /**
     * Custom parameters added by these API calls:
     *
     * {@link NewRelic#setAccountName(String)} {@link NewRelic#setProductName(String)}
     * {@link NewRelic#setUserName(String)} {@link NewRelic#addCustomParameter(String, Number)}
     * {@link NewRelic#addCustomParameter(String, String)}
     */
    public Map<String, Object> getUserAttributes() {
        return userAttributes;
    }

    /**
     * Custom parameters added by the Agent. These can be turned off by the customer.
     */
    public Map<String, Object> getAgentAttributes() {
        return agentAttributes;
    }

    /**
     * Custom parameters added by the Agent which cannot be turned off by the customer. They are required for the site
     * to function.
     */
    public Map<String, Object> getIntrinsicAttributes() {
        return intrinsicAttributes;
    }

    /**
     * Custom attributes for an error added by this API call:
     *
     * {@link NewRelic#noticeError(String, Map)}
     */
    public Map<String, Object> getErrorAttributes() {
        return errorAttributes;
    }

    public Insights getInsightsData() {
        Insights insightsData = insights.get();
        if (insightsData == null) {
            AgentConfig defaultConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
            insights.compareAndSet(null, ServiceFactory.getServiceManager().getInsights().getTransactionInsights(defaultConfig));
            insightsData = insights.get();
        }
        return insightsData;
    }

    public Logs getLogEventData() {
        Logs logEventData = logEvents.get();
        if (logEventData == null) {
            AgentConfig defaultConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
            logEvents.compareAndSet(null, ServiceFactory.getServiceManager().getLogSenderService().getTransactionLogs(defaultConfig));
            logEventData = logEvents.get();
        }
        return logEventData;
    }

    public TransactionTracerConfig getTransactionTracerConfig() {
        if (dispatcher == null) {
            return getAgentConfig().getTransactionTracerConfig();
        }
        return dispatcher.getTransactionTracerConfig();
    }

    public CrossProcessConfig getCrossProcessConfig() {
        return getAgentConfig().getCrossProcessConfig();
    }

    // Transaction naming

    public boolean setTransactionName(com.newrelic.api.agent.TransactionNamePriority namePriority, boolean override,
            String category, String... parts) {
        return setTransactionName(TransactionNamePriority.convert(namePriority), override, category, parts);
    }

    public boolean setTransactionName(TransactionNamePriority namePriority, boolean override, String category,
            String... parts) {
        return doSetTransactionName(namePriority, override, category, parts);
    }

    /**
     * Return true if the name has been set. This method is inherently unreliable in a multithreaded environment and
     * should not be used. There is no good substitute. Unfortunately, the method is exposed on the public API and
     * cannot be removed.
     *
     * @return true if the name has been set at the moment the caller asks.
     */
    @Deprecated
    public boolean isTransactionNameSet() {
        return TransactionNamingUtility.isGreaterThan(getPriorityTransactionName().getPriority(),
                TransactionNamePriority.NONE, getNamingScheme());
    }

    private boolean doSetTransactionName(TransactionNamePriority namePriority, boolean override, String category,
            String... parts) {
        if (TransactionNamingUtility.isLessThan(namePriority, TransactionNamePriority.CUSTOM_HIGH, getNamingScheme())
                && !isTransactionNamingEnabled()) {
            return false;
        }

        String name = Strings.join('/', parts);
        if (dispatcher == null) {
            if (Agent.LOG.isFinestEnabled()) {
                Agent.LOG.finest(MessageFormat.format("Unable to set the transaction name to \"{0}\" - no transaction",
                        name));
            }
            return false;
        }

        TransactionNamingPolicy policy = override ? TransactionNamingPolicy.getSameOrHigherPriorityTransactionNamingPolicy()
                : TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();

        return policy.setTransactionName(this, name, category, namePriority);
    }

    public PriorityTransactionName getPriorityTransactionName() {
        return priorityTransactionName;
    }

    /**
     * This has the side-effect of possibly ignoring the transaction as a result of applying server-side rules.
     */
    public void freezeTransactionName() {
        synchronized (lock) {
            if (priorityTransactionName.isFrozen()) {
                return;
            }
            if (dispatcher != null) {
                // Make sure we have a transaction name. Hold lock for callers
                // making logical decisions about the state
                // of this object during the process of setting the name.
                dispatcher.setTransactionName();
            }
            renameTransaction();
            priorityTransactionName = priorityTransactionName.freeze();
        }
    }

    private void renameTransaction() {
        if (Agent.LOG.isFinestEnabled()) {
            threadAssertion();
        }
        String appName = getApplicationName();

        // 1. Apply metric_name_rules (aka, "regex rules") sent by the collector.
        Normalizer metricDataNormalizer = ServiceFactory.getNormalizationService().getMetricNormalizer(appName);
        String txName = metricDataNormalizer.normalize(priorityTransactionName.getName());

        // 2. Apply transaction_segment_terms rules (aka, "white list rules") and transaction_name_rules.
        Normalizer txNormalizer = ServiceFactory.getNormalizationService().getTransactionNormalizer(appName);
        txName = txNormalizer.normalize(txName);
        if (txName == null) {
            setIgnore(true);
            return;
        }
        if (!txName.equals(priorityTransactionName.getName())) {
            setPriorityTransactionNameLocked(PriorityTransactionName.create(txName,
                    isWebTransaction() ? PriorityTransactionName.WEB_TRANSACTION_CATEGORY
                            : PriorityTransactionName.UNDEFINED_TRANSACTION_CATEGORY,
                    TransactionNamePriority.REQUEST_URI));
        }
    }

    public boolean conditionalSetPriorityTransactionName(TransactionNamingPolicy policy, String name, String category,
            TransactionNamePriority priority) {
        synchronized (lock) {
            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_TRANSACTION_NAME);

            if (NewRelic.getAgent().getLogger().isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, "newrelic.agent.Transaction::conditionalSetPriorityTransactionName: Attempting to set txn name: " +
                                "TxnNamingPolicy: {0}, name: {1}, category: {2}, TxnNamePriority {3}",
                        policy.toString(), name, category, priority.toString());
            }

            if (policy.canSetTransactionName(this, priority)) {
                if (Agent.LOG.isFinestEnabled()) {
                    Agent.LOG.log(Level.FINEST,
                            "Setting transaction name to \"{0}\" for transaction {1} using {2} scheme", name, this,
                            getNamingScheme());
                } else {
                    Agent.LOG.log(Level.FINER, "Setting transaction name to \"{0}\" for transaction {1}", name, this);
                }
                return setPriorityTransactionNameLocked(policy.getPriorityTransactionName(this, name, category,
                        priority));
            } else {
                if (Agent.LOG.isFinestEnabled()) {
                    Agent.LOG.log(
                            Level.FINEST,
                            "Not setting the transaction name to  \"{0}\" for transaction {1} using {2} scheme: a higher priority name is already in "
                                    + "place. Current transaction name is {3}",
                            name, this, getNamingScheme(), getTransactionName());
                } else {
                    Agent.LOG.log(
                            Level.FINER,
                            "Not setting the transaction name to  \"{0}\" for transaction {1}: a higher priority name is already in place. Current "
                                    + "transaction name is {2}",
                            name, this, getTransactionName());
                }
                return false;
            }
        }
    }

    /**
     * Forcibly set the priority transaction name, subverting the usual priority policy. Callers already holding the
     * lock should call {@link #setPriorityTransactionNameLocked} directly.
     *
     * @param ptn the new name
     * @return true if the name can be set
     */
    public boolean setPriorityTransactionName(PriorityTransactionName ptn) {
        synchronized (lock) {
            return setPriorityTransactionNameLocked(ptn);
        }
    }

    /**
     * Set the priority transaction name. Caller must hold lock. Policy for priority naming must be enforced by calling
     * code.
     *
     * @return true if the name can be set (which means the argument is not null).
     */
    private boolean setPriorityTransactionNameLocked(PriorityTransactionName ptn) {
        if (Agent.LOG.isFinestEnabled()) {
            threadAssertion();
        }
        if (ptn == null) {
            return false;
        }
        this.priorityTransactionName = ptn;
        return true;
    }

    // End transaction naming

    public SlowQueryListener getSlowQueryListener(boolean createIfNotExists) {
        synchronized (lock) {
            if (slowQueryListener == null && createIfNotExists) {
                String appName = getApplicationName();
                slowQueryListener = ServiceFactory.getSqlTraceService().getSlowQueryListener(appName);
            }
            return slowQueryListener;
        }
    }

    /**
     * Get a per-thread cache to store objects for the life of the transaction.
     */
    public TransactionCache getTransactionCache() {
        return getTransactionActivity().getTransactionCache();
    }

    /**
     * Return true if the transaction has ever been started.
     *
     * @return true if the transaction has ever been started. This method will continue to return true when the
     * transaction has finished. A transaction is started when a dispatcher is set on it.
     */
    public boolean isStarted() {
        return getDispatcher() != null;
    }

    /**
     * Return true if this transaction has been started and also finished.
     *
     * @return true if this transaction has been started and also finished.
     */
    public boolean isFinished() {
        return isStarted() && activeCount.get() == 0;
    }

    /**
     * Return true if this transaction has been started and is not finished.
     *
     * @return true if this transaction has been started and is not finished.
     */
    public boolean isInProgress() {
        return isStarted() && activeCount.get() != 0;
    }

    /**
     * Get the dispatcher for this transaction.
     *
     * @return the dispatcher for this transaction, or null if the dispatcher has never been set.
     */
    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    /**
     * @return the external time in milliseconds
     */
    public long getExternalTime() {
        if (dispatcher instanceof WebRequestDispatcher) {
            return ((WebRequestDispatcher) dispatcher).getQueueTime();
        }
        return 0L;
    }

    /**
     * Returns the root tracer - the first tracer in the transaction
     *
     * @return the root tracer for this transaction. The return value may be null. Note: it is not possible to deduce
     * anything about the state of the transaction from the state of the root tracer on the current thread. Do
     * not use this method to determine transaction state.
     */
    public Tracer getRootTracer() {
        return rootTracer;
    }

    /**
     * Get the completed tracers for this transaction.<br>
     * <br>
     * In Agent versions through 3.10.x, this method could be called during a transaction in progress to get the current
     * state of the tracer stack. The Agent async support post-3.10 introduced an ambiguity in the meaning of this
     * method: did it return the current state of the tracers for the current async activity, or the entire set of
     * tracers for the multiple activities that now compose a transaction? Most callers were interested in the latter,
     * so this method now returns only the tracers associated with activities that have completed. In order to obtain
     * the current state of the tracer stack for the currently-executing activity, use TransactionActivity.getTracers().
     *
     * @return the completed tracers for this transaction.
     */
    public List<Tracer> getTracers() {
        return new TracerList(this.getRootTracer(), getFinishedChildren());
    }

    public TransactionStats getOverLimitTxStatsforTesting() {
        return txStats;
    }

    /**
     * Get this task's TransactionActivity
     *
     * @return this asynchronous task's TransactionActivity
     */
    public TransactionActivity getTransactionActivity() {
        // For legacy suspend/resume instrumentation (aka play1) it's possible
        // for the tx to not be in the holder. In
        // those cases we want to return the initial activity of this tx, not
        // the one that's in the txa thread local
        if (this != Transaction.getTransaction(false)) {
            return initialActivity;
        }

        TransactionActivity result = TransactionActivity.get();
        if (result == null) {
            AgentBridge.TokenAndRefCount activeToken = AgentBridge.activeToken.get();
            if (activeToken != null && activeToken.token.isActive()) {
                return ((TokenImpl) activeToken.token).getInitiatingTracer().getTransactionActivity();
            }

            throw new IllegalStateException("TransactionActivity is gone");
        }
        return result;
    }

    void activityStarted(TransactionActivity activity) {
        Agent.LOG.log(Level.FINER, "activity {0} starting", activity);
        startTransactionIfBeginning(activity.getRootTracer());
        synchronized (lock) {
            runningChildren.put(activity.hashCode(), activity);
            activeCount.incrementAndGet();
        }
    }

    public void startTransactionIfBeginning(Tracer tracer) {
        // this is getting called for every txa, we really only need it called
        // for the first txa
        if (tracer instanceof TransactionActivityInitiator && rootTracer == null) {
            Agent.LOG.log(Level.FINER, "Starting transaction {0}", this);

            captureWallClockStartTime();
            if (ServiceFactory.getTransactionTraceService().isEnabled()) {
                AgentConfig defaultConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
                startGCTimeInMillis = defaultConfig.getTransactionTracerConfig().isGCTimeEnabled() ? getGCTime() : -1;
            } else {
                startGCTimeInMillis = -1;
            }

            if (rootTracer == null) {
                rootTracer = tracer;
            }
            if (transactionTime == null) {
                transactionTime = new TransactionTimer(tracer.getStartTime());
                Agent.LOG.log(Level.FINER, "Set timer for transaction {0}", this);
            }
            if (dispatcher == null) {
                setDispatcher(((TransactionActivityInitiator) tracer).createDispatcher());
            }

            ServiceFactory.getTransactionService().transactionStarted(this);
        }
    }

    public void setDispatcher(Dispatcher dispatcher) {
        synchronized (lock) {
            if (this.dispatcher instanceof WebRequestDispatcher) {
                Agent.LOG.log(Level.FINER,
                        "Not setting dispatcher for transaction {0}. Dispatcher is already a web dispatcher.", this);
            } else {
                this.dispatcher = dispatcher;
                Agent.LOG.log(Level.FINER, "Set dispatcher for transaction {0} to {1}", this, dispatcher);
                if (isWebRequestSet()) {
                    getInboundHeaderState();
                }
            }
        }
    }

    public void setTransportType(TransportType transportType) {
        this.transportType = transportType;
    }

    public TransactionTimer getTransactionTimer() {
        return transactionTime;
    }

    private void finishTransaction() {
        try {
            synchronized (lock) {
                // this may have the side-effect of ignoring the transaction
                freezeTransactionName();
                assignPriorityRootIfNotSet();
                if (ignore) {
                    Agent.LOG.log(Level.FINER,
                            "Transaction {0} was cancelled: ignored. This is not an error condition.", this);
                    ServiceFactory.getTransactionService().transactionCancelled(this);
                    return;
                }

                if (finishedChildren.isEmpty()) {
                    Agent.LOG.log(Level.FINER,
                            "Transaction {0} was cancelled: no activities. This is not an error condition.", this);
                    ServiceFactory.getTransactionService().transactionCancelled(this);
                    return;
                }

                // this needs to go before dispatcher.transactionFinished so that all of
                // the time metrics are correct
                TransactionStats transactionStats = transactionFinishedActivityMerging();
                transactionTime.markTransactionAsDone();
                recordFinalGCTime(transactionStats);
                handleTokenTimeout(transactionStats);

                String txName = priorityTransactionName.getName();
                // parse headers in dispatcher request before we get rid of request/response objects
                getInboundHeaderState();
                // this may trigger this dispatcher to record extra metrics like apdex & HttpDispatcher
                dispatcher.transactionFinished(txName, transactionStats);

                if (Agent.LOG.isFinerEnabled()) {
                    String requestURI = dispatcher == null ? "No Dispatcher Defined" : dispatcher.getUri();
                    Agent.LOG.log(Level.FINER, "Transaction {0} for request: {1} finished {2}ms {3}",
                            txName, requestURI, transactionTime.getResponseTimeInMilliseconds(), this);
                }

                if (!ServiceFactory.getServiceManager().isStarted()) {
                    Agent.LOG.log(Level.INFO, "Transaction {0} tried to finish but ServiceManager not started",
                            this);
                    return;
                }

                // Some parts of the code below are only required if this transaction's TT is selected
                // for sending upstream. Unfortunately we don't know at this point in the harvest whether
                // this transaction's trace will be selected, and there's no obvious way to know that can
                // be built with maintainable code. There was a previous effort at this, but the obvious
                // happened: it sat broken in the code (i.e. it did some checks, but always returned true)
                // for 18 months before we noticed and ripped it out. Please don't repeat this mistake.

                TransactionTracerConfig ttConfig = getTransactionTracerConfig();
                TransactionCounts rootCounts = getTransactionCounts();
                if (rootCounts.isOverTracerSegmentLimit()) {
                    getIntrinsicAttributes().put(AttributeNames.SEGMENT_CLAMP, rootCounts.getSegmentCount());

                    // Record supportability metric to track when a segment clamp occurs
                    transactionStats.getUnscopedStats().getStats(MetricNames.SUPPORTABILITY_TRANSACTION_SEGMENT_CLAMP)
                            .recordDataPoint(rootCounts.getSegmentCount());
                }
                if (rootCounts.isOverTransactionSize()) {
                    getIntrinsicAttributes().put(AttributeNames.SIZE_LIMIT_PARAMETER_NAME,
                            "The transaction size limit was reached");
                }
                int count = rootCounts.getStackTraceCount();
                if (count >= ttConfig.getMaxStackTraces()) {
                    getIntrinsicAttributes().put(AttributeNames.STACK_TRACE_CLAMP, count);
                }
                if (rootCounts.isOverTokenLimit()) {
                    getIntrinsicAttributes().put(AttributeNames.TOKEN_CLAMP, rootCounts.getTokenCount());
                }
                count = rootCounts.getExplainPlanCount();
                if (count >= ttConfig.getMaxExplainPlans()) {
                    getIntrinsicAttributes().put(AttributeNames.EXPLAIN_PLAN_CLAMP, count);
                }

                DistributedTracingConfig distributedTracingConfig = getAgentConfig().getDistributedTracingConfig();
                if (!distributedTracingConfig.isEnabled()) {
                    if (getInboundHeaderState().isTrustedCatRequest()) {
                        String id = getInboundHeaderState().getClientCrossProcessId();
                        getIntrinsicAttributes().put(AttributeNames.CLIENT_CROSS_PROCESS_ID_PARAMETER_NAME, id);
                    }
                    String referrerGuid = getInboundHeaderState().getReferrerGuid();
                    if (referrerGuid != null) {
                        getIntrinsicAttributes().put(AttributeNames.REFERRING_TRANSACTION_TRACE_ID_PARAMETER_NAME, referrerGuid);
                    }

                    String tripId = getCrossProcessTransactionState().getTripId();
                    if (tripId != null) {
                        getIntrinsicAttributes().put(AttributeNames.TRIP_ID_PARAMETER_NAME, tripId);
                        int pathHash = getCrossProcessTransactionState().generatePathHash();
                        getIntrinsicAttributes().put(AttributeNames.PATH_HASH_PARAMETER_NAME,
                                ServiceUtils.intToHexString(pathHash));
                    }
                }

                if (isSynthetic()) {
                    Agent.LOG.log(Level.FINEST, "Completing Synthetics transaction for monitor {0}",
                            getInboundHeaderState().getSyntheticsMonitorId());
                    getIntrinsicAttributes().put(AttributeNames.SYNTHETICS_RESOURCE_ID,
                            this.getInboundHeaderState().getSyntheticsResourceId());
                    getIntrinsicAttributes().put(AttributeNames.SYNTHETICS_MONITOR_ID,
                            this.getInboundHeaderState().getSyntheticsMonitorId());
                    getIntrinsicAttributes().put(AttributeNames.SYNTHETICS_JOB_ID,
                            this.getInboundHeaderState().getSyntheticsJobId());
                    getIntrinsicAttributes().put(AttributeNames.SYNTHETICS_VERSION,
                            String.valueOf(this.getInboundHeaderState().getSyntheticsVersion()));
                    if (this.getInboundHeaderState().getSyntheticsType() != null) {
                        getIntrinsicAttributes().put(AttributeNames.SYNTHETICS_TYPE,
                                this.getInboundHeaderState().getSyntheticsType());
                    }
                    if (this.getInboundHeaderState().getSyntheticsInitiator() != null) {
                        getIntrinsicAttributes().put(AttributeNames.SYNTHETICS_INITIATOR,
                                this.getInboundHeaderState().getSyntheticsInitiator());
                    }
                    if (this.getInboundHeaderState().getSyntheticsAttrs() != null) {
                        Map<String, String> attrsMap = this.getInboundHeaderState().getSyntheticsAttrs();
                        String attrName;

                        for (String key : attrsMap.keySet()) {
                            attrName = String.format("synthetics_%s", key);
                            getIntrinsicAttributes().put(attrName, attrsMap.get(key));
                        }
                    }
                }

                if (timeoutCause != null && timeoutCause.cause != null) {
                    getIntrinsicAttributes().put(AttributeNames.TIMEOUT_CAUSE, timeoutCause.cause);
                }

                String displayHost = getAgentConfig().getValue("process_host.display_name", null);
                if (displayHost != null) {
                    getAgentAttributes().put(AttributeNames.DISPLAY_HOST, displayHost);
                }
                String instanceName = ServiceFactory.getEnvironmentService().getEnvironment().getAgentIdentity().getInstanceName();
                if (instanceName != null) {
                    getAgentAttributes().put(AttributeNames.INSTANCE_NAME, instanceName);
                }

                // Only add jvm.thread_name if transaction  was not timed out and
                // if it's *NOT* a multi-threaded transaction
                TimedSet<TokenImpl> tokenCache = activeTokensCache.get();
                if ((tokenCache == null || tokenCache.timedOutCount() == 0) && finishedChildren.size() == 1) {
                    if (!ServiceFactory.getThreadService().isAgentThreadId(Thread.currentThread().getId())) {
                        getAgentAttributes().put(AttributeNames.THREAD_NAME, Thread.currentThread().getName());
                    }
                }

                getIntrinsicAttributes().put(AttributeNames.PRIORITY, getPriority());

                TransactionData transactionData = new TransactionData(this, rootCounts.getTransactionSize());
                ServiceFactory.getTransactionService().transactionFinished(transactionData, transactionStats);

                // In serverless mode, trigger immediate harvest when transaction completes
                if (ServiceFactory.getConfigService().getDefaultAgentConfig().getServerlessConfig().isEnabled()) {
                    Agent.LOG.log(Level.FINEST, "Serverless mode: Beginning harvest cycle for completed transaction");
                    ServiceFactory.getHarvestService().harvestNow();
                }
            }
        } catch (Throwable th) {
            Agent.LOG.log(Level.WARNING, th, "Transaction {0} was not reported because of an internal error.", this);
            ServiceFactory.getTransactionService().transactionCancelled(this);
        }
    }

    private TransactionStats transactionFinishedActivityMerging() {
        // here the last transaction activity is not on the completed list

        TransactionStats transactionStats = txStats;
        long totalCpuTime = 0;
        boolean reportingCpu = true;
        // this is for the legacy async
        Object val = getIntrinsicAttributes().remove(AttributeNames.CPU_TIME_PARAMETER_NAME);
        if (val instanceof Long) {
            totalCpuTime = (Long) val;
            if (totalCpuTime < 0) {
                reportingCpu = false;
            }
        }

        // iterate over transaction activities
        for (TransactionActivity kid : getFinishedChildren()) {
            // merge activity stats
            if (transactionStats == null) {
                transactionStats = kid.getTransactionStats();
            } else {
                TransactionStats stats = kid.getTransactionStats();
                transactionStats.getScopedStats().mergeStats(stats.getScopedStats());
                transactionStats.getUnscopedStats().mergeStats(stats.getUnscopedStats());
            }

            // merge totalTime, set end time
            if (kid.getRootTracer() != null) {
                Tracer rootTracer = kid.getRootTracer();
                transactionTime.markTransactionActivityAsDone(rootTracer.getEndTime(), rootTracer.getDuration());

                if (Agent.LOG.isFinestEnabled()) {
                    Map<String, Object> tracerAtts = rootTracer.getAgentAttributes();
                    if (tracerAtts != null && !tracerAtts.isEmpty()) {
                        Agent.LOG.log(Level.FINEST, "Tracer Attributes for {0} are {1}", rootTracer, tracerAtts);
                    }
                }
            }

            // merge activity cpuTime, -1 means not reported, this should be an
            // all or nothing
            if (reportingCpu) {
                if (kid.getTotalCpuTime() >= 0) {
                    totalCpuTime += kid.getTotalCpuTime();
                } else {
                    reportingCpu = false;
                }
            }
        }

        if (reportingCpu && totalCpuTime > 0) { // -1 means it was not computed
            getIntrinsicAttributes().put(AttributeNames.CPU_TIME_PARAMETER_NAME, totalCpuTime);
        }

        return transactionStats;
    }

    public synchronized void addTotalCpuTimeForLegacy(long time) {
        long totalCpuTime;
        Object val = getIntrinsicAttributes().remove(AttributeNames.CPU_TIME_PARAMETER_NAME);
        if (val instanceof Long) {
            totalCpuTime = (Long) val;
        } else {
            totalCpuTime = 0;
        }
        if (totalCpuTime != TransactionActivity.NOT_REPORTED) {
            totalCpuTime += time;
        }
        getIntrinsicAttributes().put(AttributeNames.CPU_TIME_PARAMETER_NAME, totalCpuTime);
    }

    public void recordFinalGCTime(TransactionStats stats) {
        // Only attempt to record GC time if we're capturing a transaction trace
        if (isTransactionTraceEnabled() && (getRunningDurationInNanos() > getTransactionTracerConfig().getTransactionThresholdInNanos())) {
            Long totalGCTime = (Long) getIntrinsicAttributes().get(AttributeNames.GC_TIME_PARAMETER_NAME);
            if (totalGCTime == null) {
                // if gc time is turned off, startGCTimeInMillis == -1
                if (startGCTimeInMillis > -1) {
                    long gcTime = getGCTime();
                    // We'll only ever get in here if a GC happens to occur during the tracing of a transaction
                    if (gcTime != startGCTimeInMillis) {
                        totalGCTime = gcTime - startGCTimeInMillis;
                        getIntrinsicAttributes().put(AttributeNames.GC_TIME_PARAMETER_NAME, totalGCTime);
                        stats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.GC_CUMULATIVE).recordResponseTime(totalGCTime, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }

    /**
     * Must be called under the transaction lock
     */
    private void handleTokenTimeout(TransactionStats stats) {
        TimedSet<TokenImpl> tokenCache = activeTokensCache.get();
        int timedOutTokens = tokenCache != null ? tokenCache.timedOutCount() : 0;
        if (timedOutTokens > 0) {
            stats.getUnscopedStats().getStats(MetricNames.SUPPORTABILITY_ASYNC_TOKEN_TIMEOUT).incrementCallCount(timedOutTokens);
        }
    }

    /**
     * Utility method that TransactionService can use to tell a transaction to process its cache, and possibly do any
     * maintenance work, like timing out expired entries.
     */
    void cleanUp() {
        checkExpireTracedActivities();
        TimedSet<TokenImpl> tokenCache = activeTokensCache.get();
        if (tokenCache != null) {
            tokenCache.cleanUp();
        }
    }

    public boolean isTransactionTraceEnabled() {
        return ttEnabled;
    }

    public boolean isAutoAppNamingEnabled() {
        return autoAppNamingEnabled;
    }

    public boolean isTransactionNamingEnabled() {
        return transactionNamingEnabled;
    }

    public TransactionNamingScheme getNamingScheme() {
        String appName = getPriorityApplicationName().getName();
        AgentConfig appConfig = ServiceFactory.getConfigService().getAgentConfig(appName);
        return appConfig.getTransactionNamingScheme();
    }

    /**
     * Return true if this transaction is the result of a valid request from New Relic Synthetics.
     *
     * @return true if this transaction contains the Synthetics request header, the account ID can be verified as
     * trusted, and the version is within the range of supported versions.
     */
    public boolean isSynthetic() {
        return getInboundHeaderState().isTrustedSyntheticsRequest();
    }

    /**
     * Although the Transaction infrastructure was originally constructed to instrument transactions originated by web
     * requests, the Java agent also supports transaction origination from JMS messages. Both Cross application tracing
     * (CAT) and cross-process tracking of synthetic transactions are fully supported for both JMS-originated
     * transactions and across JMS-based external requests. This method allows instrumentation, e.g. the JMS
     * instrumentation, to provide an InboundHeaders instance as the request headers for this transaction. In unusual
     * cases, we may be within an HTTP transaction and synchronously execute JMS instrumentation that provides a second
     * set of headers. In such cases, the HTTP headers "win".
     *
     * @param headers the proposed request headers
     */
    public void provideHeaders(InboundHeaders headers) {
        if (headers != null) {
            String encodingKey = getCrossProcessConfig().getEncodingKey();
            provideRawHeaders(new DeobfuscatedInboundHeaders(headers, encodingKey));
        }
    }

    /**
     * @param headers deobfuscated request headers.
     */
    public void provideRawHeaders(InboundHeaders headers) {
        if (headers != null) {
            synchronized (lock) {
                Agent.LOG.log(Level.FINEST, "Provided headers in transaction {0}", this);
                this.providedHeaders = headers;
            }
        }
    }

    /**
     * Get the inbound header state for this transaction, bringing it into existence if it does not already exist. If
     * the transaction is not a web transaction and instrumentation (e.g. JMS instrumentation) has provided headers, we
     * process the instrumentation-provided headers as our inbound request headers.
     *
     * @return an InboundHeaderState for this transaction. This method does not return null.
     */
    public InboundHeaderState getInboundHeaderState() {
        if (inboundHeaderState == null) {
            synchronized (lock) {
                if (inboundHeaderState == null) {
                    InboundHeaders requestHeaders = getRequestHeaders(this);
                    if (requestHeaders == null) {
                        // e.g. from JMS instrumentation
                        if (providedHeaders != null) {
                            Agent.LOG.log(Level.FINEST, "Using provided headers in transaction {0}", this);
                        }
                        requestHeaders = (providedHeaders == null) ? null : providedHeaders;
                    } else {
                        Agent.LOG.log(Level.FINEST, "Using request headers in transaction {0}", this);
                    }
                    try {
                        inboundHeaderState = new InboundHeaderState(this, requestHeaders);
                    } catch (RuntimeException rex) {
                        Agent.LOG.log(Level.FINEST, "Unable to parse inbound headers in transaction {0}: {1}", this, rex);
                        inboundHeaderState = new InboundHeaderState(this, null);
                    }
                }
            }
        }
        return this.inboundHeaderState;
    }


    /**
     * package visibility for testing only.
     */
    static InboundHeaders getRequestHeaders(Transaction tx) {
        if (tx.dispatcher != null) {
            if (tx.dispatcher.getRequest() != null) {
                String encodingKey = tx.getCrossProcessConfig().isCrossApplicationTracing()
                        ? tx.getCrossProcessConfig().getEncodingKey()
                        : tx.getCrossProcessConfig().getSyntheticsEncodingKey();
                return new DeobfuscatedInboundHeaders(tx.dispatcher.getRequest(), encodingKey);
            }
        }
        return null;
    }

    public IRPMService getRPMService() {
        return ServiceFactory.getRPMServiceManager().getOrCreateRPMService(getPriorityApplicationName());
    }

    /**
     * Clear this thread's thread local reference to its transaction.<br>
     * <br>
     * Caution: in order to meet the functional expectations of legacy code, this method also clears the
     * TransactionActivity from its thread-local variable on the current thread.
     */
    public static void clearTransaction() {
        transactionHolder.remove();
        TransactionActivity.clear();
        AgentBridge.activeToken.remove();
    }

    /**
     * Set this thread's thread local reference to the transaction.<br>
     * <br>
     * Caution: in order to meet the functional expectations of legacy code, this method also sets the thread-local
     * variable holding the TransactionActivity to the TransactionActivity instance originally created with this
     * Transaction instance. As a result, this method must not be used by code that changes the binding between
     * Transaction and TransactionActivity, for example the implementation of the "start async" API method.
     */
    public static void setTransaction(Transaction tx) {
        TransactionActivity.set(tx.initialActivity);
        transactionHolder.set(tx);
    }

    /**
     * Returns this thread's reference to the transaction.
     *
     * @return this thread's reference to the transaction. The return value can be null, but only if this method is
     * called from an Agent thread that is not already running within a Transaction. When called from a
     * non-Agent thread, the result is not null.
     */
    public static Transaction getTransaction() {
        return getTransaction(true);
    }

    /**
     * The token cache is expire after access, so the cache needs to be refreshed each time the token is used, which
     * only matters in the link case.
     */
    public void refreshToken(TokenImpl token) {
        TimedSet<TokenImpl> tokenCache = activeTokensCache.get();
        if (tokenCache != null) {
            tokenCache.refresh(token);
        }
    }

    public static boolean linkTxOnThread(TokenImpl token) {
        WeakRefTransaction weakRefTransaction = token.getTransaction();
        Transaction newTx = weakRefTransaction == null ? null : weakRefTransaction.getTransactionIfExists();
        if (newTx != null) {
            newTx.refreshToken(token);
        }

        Transaction oldTx = null;
        AgentBridge.TokenAndRefCount activeToken = AgentBridge.activeToken.get();
        if (activeToken != null && activeToken.token.isActive()) {
            oldTx = ((TokenImpl) activeToken.token).getTransaction().getTransactionIfExists();
        }

        if (oldTx == null) {
            oldTx = transactionHolder.get();
        }
        if (newTx == null || newTx == oldTx) {
            Agent.LOG.log(Level.FINER, "Transaction {0}: ignoring link call because transaction already on thread.", newTx);
            return false;
        }

        synchronized (newTx.lock) {
            if (!newTx.isInProgress()) {
                Agent.LOG.log(Level.FINER, "Transaction {0}: ignoring link call because transaction not in progress.", newTx);
                AgentBridge.instrumentation.instrument();
                return false;
            } else if (!token.isActive()) {
                Agent.LOG.log(Level.FINER, "Transaction {0}: ignoring link call because token is no longer active {1}.", newTx, token);
                return false;
            } else {
                TransactionActivity oldTxa = TransactionActivity.get();
                if (oldTxa == null || !oldTxa.isStarted()) {
                    Agent.LOG.log(Level.FINER, "Transaction {0}: ignoring link call because there is no started txa to link to: {1}.", newTx, oldTxa);
                    AgentBridge.instrumentation.instrument();
                    return false;
                } else {
                    // don't worry about a race with expire, if tracer is not null we're fine because tracer is
                    // passed to startAsyncActivity and not the token
                    Tracer tracer = token.getInitiatingTracer();
                    if (tracer == null || !token.isActive()) {
                        return false;
                    }

                    int oldTxaId = oldTxa.hashCode();
                    oldTxa.startAsyncActivity(newTx, newTx.nextActivityId.getAndIncrement(), tracer);
                    if (oldTx != null) {
                        // We migrate state from the source to the target transactions of the migration.
                        // It's never been completely clear whether or not this is the correct behavior.
                        PriorityApplicationName pan = oldTx.getPriorityApplicationName();
                        if (pan != PriorityApplicationName.NONE) {
                            newTx.setApplicationName(pan.getPriority(), pan.getName(), true);
                        }
                        PriorityTransactionName ptn = oldTx.getPriorityTransactionName();
                        if (ptn != PriorityTransactionName.NONE) {
                            newTx.setTransactionName(ptn.getPriority(), true, ptn.getCategory(),
                                    oldTx.getTransactionName());
                        }

                        newTx.getInternalParameters().putAll(oldTx.getInternalParameters());
                        newTx.getPrefixedAgentAttributes().putAll(oldTx.getPrefixedAgentAttributes());
                        newTx.getAgentAttributes().putAll(oldTx.getAgentAttributes());
                        newTx.getIntrinsicAttributes().putAll(oldTx.getIntrinsicAttributes());
                        newTx.getUserAttributes().putAll(oldTx.getUserAttributes());
                        newTx.getErrorAttributes().putAll(oldTx.getErrorAttributes());

                        // Now allow the "old" transaction to execute the cancellation
                        // cleanup path. In the trivial case where the "old" transaction
                        // was created solely to enable the link call we're currently
                        // processing, this will be our only chance to remove the "old"
                        // transaction from the Map maintained in the Transaction Service.
                        // We set the holder to null in hopes that future release(s) of the
                        // Agent may be able to assert against leaking transaction, i.e.
                        // to assert that we never set a transaction on the thread unless
                        // the holder is null when we do it. JAVA-2647.
                        oldTx.ignore = true;
                        oldTx.checkExpire();
                        oldTx.checkFinishTransaction(oldTxa, oldTxaId);
                        transactionHolder.set(null);
                    }
                }

                transactionHolder.set(newTx);
                return true;
            }
        }
    }

    public void startFastAsyncWork(TransactionActivity txa, Tracer tracer) {
        txa.startAsyncActivity(this, nextActivityId.getAndIncrement(), tracer);
    }

    /**
     * Get this thread's reference to a transaction, creating it if this thread has no Transaction reference and
     * creation is requested and allowable.
     *
     * @param createIfNotExists true to request creation of the object
     * @return the transaction. Return null if createIfNotExists is false and the transaction has not previously been
     * created, or if the current thread is an Agent thread.
     */
    public static Transaction getTransaction(boolean createIfNotExists) {
        Transaction tx = transactionHolder.get();
        AgentBridge.TokenAndRefCount activeToken = AgentBridge.activeToken.get();
        if (activeToken != null && activeToken.token != null && activeToken.token.isActive() && tx == null) {
            WeakRefTransaction weakRefTx = (WeakRefTransaction) activeToken.token.getTransaction();
            tx = weakRefTx.getTransactionIfExists();
        }

        if (tx == null && createIfNotExists && !(Thread.currentThread() instanceof AgentThread)) {
            // This code path is the *only* correct way to create a transaction.
            // Note: it's possible
            // that several tests would be greatly simplified if this code
            // checked for the presence
            // of the TransactionService before adding the transaction to it. In
            // other words there
            // are tests that initialize a bunch of services merely to avoid NPE
            // on addTransaction().
            // It might be worthwhile to check for that here despite the small
            // runtime perf impact.
            if (ServiceFactory.getServiceManager().getCircuitBreakerService().isTripped()) {
                return getOrCreateDummyTransaction();
            }
            try {
                tx = new Transaction();
                tx.postConstruct();
                transactionHolder.set(tx);
            } catch (RuntimeException rex) {
                // The exception might have been thrown after the Activity was
                // created (in postConstruct()). Let's be careful to avoid the
                // weird case where there is an Activity on the thread without
                // a corresponding Transaction.
                Agent.LOG.log(Level.FINEST, rex, "while creating Transaction");
                TransactionActivity.clear();
                throw rex;
            }
        }
        return tx;
    }

    protected static synchronized Transaction getOrCreateDummyTransaction() {
        if (dummyTransaction == null) {
            dummyTransaction = new DummyTransaction();
        }
        return dummyTransaction;
    }

    /**
     * Use NewRelic#setTransactionName(String, String)
     */
    @Deprecated
    public void setNormalizedUri(String normalizedUri) {
        synchronized (lock) {
            if (normalizedUri == null || normalizedUri.length() == 0) {
                return;
            }
            TransactionNamingPolicy policy = TransactionNamingPolicy.getSameOrHigherPriorityTransactionNamingPolicy();
            if (Agent.LOG.isLoggable(Level.FINER)) {
                if (policy.canSetTransactionName(this, TransactionNamePriority.CUSTOM_HIGH)) {
                    String msg = MessageFormat.format(
                            "Setting transaction name to normalized URI \"{0}\" for transaction {1}", normalizedUri,
                            this);
                    Agent.LOG.finer(msg);
                }
            }
            policy.setTransactionName(this, normalizedUri, MetricNames.NORMALIZED_URI,
                    TransactionNamePriority.CUSTOM_HIGH);
            this.normalizedUri = normalizedUri;
        }
    }

    /**
     * Use {@link #getPriorityTransactionName()}
     *
     * @return the normalized URI for this transaction, or null if not set
     */
    @Deprecated
    public String getNormalizedUri() {
        synchronized (lock) {
            return normalizedUri;
        }
    }

    public TransactionThrowable getThrowable() {
        return errorTracker.getThrowable();
    }

    /**
     * This method should only be called at the END of a transaction, AFTER the application name is finalized.
     */
    public boolean isErrorReportableAndNotIgnored() {
        ErrorAnalyzer errorAnalyzer = new ErrorAnalyzerImpl(getAgentConfig().getErrorCollectorConfig());

        int responseStatus = getStatus();
        boolean isReportable = errorAnalyzer.isReportable(responseStatus, getThrowable());

        return isReportable && !ignoreErrors && !errorAnalyzer.isIgnoredError(responseStatus,
                getThrowable() == null ? null : getThrowable().throwable);
    }

    /**
     * This method should only be called at the END of a transaction, AFTER the application name is finalized.
     */
    public boolean isErrorNotExpected() {
        int responseStatus = getStatus();
        ErrorAnalyzer errorAnalyzer = new ErrorAnalyzerImpl(getAgentConfig().getErrorCollectorConfig());
        return !errorAnalyzer.isExpectedError(responseStatus, getThrowable());
    }

    public int getStatus() {
        return getWebResponse().getStatus();
    }

    public String getStatusMessage() {
        return getWebResponse().getStatusMessage();
    }

    public void freezeStatus() {
        getWebResponse().freezeStatus();
    }

    /**
     * Set the throwable with given priority. Calls with null throwable are ignored. Unless on initial
     * TransactionActivity, calls with non-API priority are ignored.
     *
     * This means if an async TransactionActivity throws an exception, you must report via the API as the tracer will
     * ignore it. This behavior is intentional in order to properly support the majority of async cases where a common
     * pattern is to spin off work to another thread, catch any exceptions thrown from that thread and then handle them
     * in the initial thread. There are cases where this pattern may fall apart but we should treat those as special
     * cases which can use the API in order to report the "correct" error.
     *
     * @param throwable the throwable that should be reported
     * @param priority the priority of the throwable. If set to {@link TransactionErrorPriority#TRACER} and not reported
     * @param expected
     */
    public void setThrowable(Throwable throwable, TransactionErrorPriority priority, boolean expected) {
        if (throwable == null) {
            return;
        }

        // See the javadoc comment on this method for more information about why we're doing this
        if (TransactionActivity.get() != this.initialActivity && priority != TransactionErrorPriority.API) {
            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.log(Level.FINER,
                        "Non-API call to setThrowable from asynchronous activity ignored: {0} with priority {1}",
                        throwable, priority);
            }
            return;
        }
        if (ignoreErrorPriority) {
            errorTracker.setThrowable(throwable, priority, expected, safeGetMostRecentSpanId());
            return;
        }

        if (Agent.LOG.isFinerEnabled()) {
            Agent.LOG.log(Level.FINER,
                    "Attempting to set throwable in transaction: {0} having priority {1} with priority {2}",
                    throwable.getClass().getName(), errorTracker.getPriority(), priority);
        }

        if (errorTracker.tryUpdatePriority(priority)) {
            errorTracker.setThrowable(throwable, priority, expected, safeGetMostRecentSpanId());
        }
    }

    private String safeGetMostRecentSpanId() {
        try {
            return getTransactionActivity().getLastTracer().getGuid();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Capture the first tracer that had this unhandled exception.
     *
     * @param throwable The unhandled exception from the tracer
     * @param spanId The Tracer GUID
     */
    public void noticeTracerException(Throwable throwable, String spanId) {
        errorTracker.noticeTracerException(throwable, spanId);
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void ignore() {
        setIgnore(true);
    }

    public void setIgnore(boolean ignore) {
        if (dispatcher != null) {
            synchronized (lock) {
                this.ignore = ignore;
                for (TransactionActivity child : this.runningChildren.values()) {
                    child.setOwningTransactionIsIgnored(true);
                }
                for (TransactionActivity finishedChild : this.finishedChildren) {
                    finishedChild.setOwningTransactionIsIgnored(true);
                }
            }
            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_IGNORE);
        } else {
            Agent.LOG.log(Level.FINEST, "setIgnore called outside of an open transaction");
        }
    }

    public void ignoreApdex() {
        if (isStarted()) {
            dispatcher.setIgnoreApdex(true);

            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_IGNORE_APDEX);
        } else {
            Agent.LOG.finer("ignoreApdex invoked with no transaction");
        }
    }

    public void ignoreErrors() {
        if (isStarted()) {
            ignoreErrors = true;
            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_IGNORE_ERRORS);
        } else {
            Agent.LOG.finer("ignoreErrors invoked with no transaction");
        }
    }

    public TransactionCounts getTransactionCounts() {
        return counts;
    }

    public boolean shouldGenerateTransactionSegment() {
        return ttEnabled && getTransactionCounts().shouldGenerateTransactionSegment();
    }

    public DatabaseStatementParser getDatabaseStatementParser() {
        if (databaseStatementParser == null) {
            synchronized (lock) {
                if (databaseStatementParser == null) {
                    databaseStatementParser = createDatabaseStatementParser();
                }
            }
        }
        return databaseStatementParser;
    }

    private DatabaseStatementParser createDatabaseStatementParser() {
        return new CachingDatabaseStatementParser(ServiceFactory.getDatabaseService().getDatabaseStatementParser());
    }

    public BrowserTransactionState getBrowserTransactionState() {
        synchronized (lock) {
            if (browserTransactionState == null) {
                browserTransactionState = BrowserTransactionStateImpl.create(this);
            }
            return browserTransactionState;
        }
    }

    public CrossProcessState getCrossProcessState() {
        return getCrossProcessTransactionState();
    }

    public CrossProcessTransactionState getCrossProcessTransactionState() {
        if (crossProcessTransactionState == null) {
            synchronized (lock) {
                if (crossProcessTransactionState == null) {
                    crossProcessTransactionState = CrossProcessTransactionStateImpl.create(this);
                }
            }
        }
        return crossProcessTransactionState;
    }

    public TransactionState getTransactionState() {
        return transactionState;
    }

    public void setTransactionState(TransactionState transactionState) {
        // This method is used by legacy async instrumentation (play1 and
        // servlet 3.0) to install alternative
        // implementations for certain operations on the transaction. When this
        // occurs, the Agent
        // must stop using the "fast path" when creating tracers and instead
        // delegate to the
        // TransactionState implementation. We handle this as a one-time,
        // one-way, agent-wide state
        // change. The disableFastPath() method tests before writing, so it's
        // safe for us to call
        // this every time without risk of creating a "hot" (highly contended)
        // volatile variable.
        Agent.disableFastPath();
        this.transactionState = transactionState;
    }

    /**
     * Instruct the transaction to write the outbound response headers. This must be called before response headers are
     * sent and the response is committed. Successive calls will have no effect (first wins).
     *
     * This must be called after {@link #setWebRequest(Request)}} and {@link #setWebResponse(Response)}.
     */
    public void addOutboundResponseHeaders() {
        getCrossProcessTransactionState().writeResponseHeaders();
    }

    /**
     * Returns the web response associated with this transaction that tracks the response status code, message, etc.
     * This method will always return an object so there's no need for a null check.
     */
    public WebResponse getWebResponse() {
        if (dispatcher instanceof WebResponse) {
            return (WebResponse) dispatcher;
        }
        return DEFAULT_RESPONSE;
    }

    /**
     * Returns true if in a web transaction.
     */
    public boolean isWebTransaction() {
        return dispatcher != null && dispatcher.isWebTransaction();
    }

    /**
     * Turns the current transaction from a background transaction into a web transaction.
     */
    public void convertToWebTransaction() {
        if (!isWebTransaction()) {
            setDispatcher(new WebRequestDispatcher(DUMMY_REQUEST, DUMMY_RESPONSE, this));
        }
    }

    public void setRequestAndResponse(Request request, Response response) {
        Request req = request == null ? DUMMY_REQUEST : request;
        Response res = response == null ? DUMMY_RESPONSE : response;
        setDispatcher(new WebRequestDispatcher(req, res, this));
    }

    public boolean isWebRequestSet() {
        if (dispatcher instanceof WebRequestDispatcher) {
            return !DUMMY_REQUEST.equals(dispatcher.getRequest());
        }
        return false;
    }

    public boolean isWebResponseSet() {
        if (dispatcher instanceof WebRequestDispatcher) {
            return !DUMMY_RESPONSE.equals(dispatcher.getResponse());
        }
        return false;
    }

    /**
     * Sets the request for the current transaction.
     * Setting the request will convert the current transaction into a web transaction.
     * Successive calls will have no effect (first wins).
     *
     * @param req The current transaction's request.
     */
    public void setWebRequest(Request req) {
        final Request request = req == null ? DUMMY_REQUEST : req;

        synchronized (lock) {
            // Set web request at most once.
            if (dispatcher instanceof WebRequestDispatcher) {
                if (DUMMY_REQUEST.equals(dispatcher.getRequest())) {
                    dispatcher.setRequest(request);
                    Agent.LOG.log(Level.FINEST, "Set web request for transaction {0} to {1}", this, request);
                    getInboundHeaderState();
                } else {
                    Agent.LOG.log(Level.FINEST,
                            "Not setting web request for transaction {0}. Web request is already set.", this);
                }
            } else {
                Agent.LOG.log(Level.FINEST, "Set web request for transaction {0}", this);
                setDispatcher(new WebRequestDispatcher(request, DUMMY_RESPONSE, this));
            }
        }
    }

    /**
     * Sets the response for the current transaction.
     * Setting the response will convert the current transaction into a web transaction.
     * Successive calls will have no effect (first wins).
     *
     * @param resp The current transaction's response.
     */
    public void setWebResponse(Response resp) {
        final Response response = resp == null ? DUMMY_RESPONSE : resp;

        synchronized (lock) {
            // Set web response at most once.
            if (dispatcher instanceof WebRequestDispatcher) {
                if (DUMMY_RESPONSE.equals(dispatcher.getResponse())) {
                    dispatcher.setResponse(response);
                    Agent.LOG.log(Level.FINEST, "Set web response for transaction {0} to {1}", this, response);
                } else {
                    Agent.LOG.log(Level.FINEST,
                            "Not setting web response for transaction {0}. Web response is already set.", this);
                }
            } else {
                Agent.LOG.log(Level.FINEST,
                        "Set web response for transaction {0}. Transaction does not have a corresponding request", this);
                setDispatcher(new WebRequestDispatcher(DUMMY_REQUEST, response, this));
            }
        }
    }

    protected static final WebResponse DEFAULT_RESPONSE = new WebResponse() {
        @Override
        public void setStatusMessage(String message) {
        }

        @Override
        public void setStatus(int statusCode) {
        }

        @Override
        public int getStatus() {
            return 0;
        }

        @Override
        public String getStatusMessage() {
            return null;
        }

        @Override
        public void freezeStatus() {
        }
    };

    private static final Request DUMMY_REQUEST = new Request() {
        @Override
        public String[] getParameterValues(String name) {
            return null;
        }

        @Override
        public Enumeration<?> getParameterNames() {
            return null;
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public String getRequestURI() {
            return "/";
        }

        @Override
        public String getRemoteUser() {
            return null;
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public String getCookieValue(String name) {
            return null;
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }
    };

    private static final Response DUMMY_RESPONSE = new Response() {
        @Override
        public int getStatus() throws Exception {
            Agent.LOG.log(Level.FINEST, "Web response is not set. Using default status code 0.");
            return 0;
        }

        @Override
        public String getStatusMessage() throws Exception {
            Agent.LOG.log(Level.FINEST, "Web response is not set. No status message.");
            return null;
        }

        @Override
        public void setHeader(String name, String value) {
            Agent.LOG.log(Level.FINEST, "Web response is not set. Not setting header {0} : {1}.", name, value);
        }

        @Override
        public String getContentType() {
            Agent.LOG.log(Level.FINEST, "Web response is not set. No content type.");
            return null;
        }

        @Override
        public HeaderType getHeaderType() {
            Agent.LOG.log(Level.FINEST, "Web response is not set. Header type is HTTP.");
            return HeaderType.HTTP;
        }
    };

    private static final int REQUEST_TRACER_FLAGS = TracerFlags.GENERATE_SCOPED_METRIC
            | TracerFlags.TRANSACTION_TRACER_SEGMENT | TracerFlags.DISPATCHER;

  public static final int SCALA_API_TRACER_FLAGS = TracerFlags.GENERATE_SCOPED_METRIC
                                                    | TracerFlags.TRANSACTION_TRACER_SEGMENT
                                                    | TracerFlags.DISPATCHER
                                                    | TracerFlags.CUSTOM;

    // There exist broken servlet frameworks that spew multiple
    // requestInitialized and Destroyed
    // calls. Since requestDestroyed can result in finishing a transaction and
    // clearing out the
    // the ThreadLocal that refers to the TransactionActivity, repeated calls
    // can cause exceptions.
    // And since these extra Initialized and Destroyed calls might possibly
    // occur on different threads,
    // we need to serialize all this processing on a single lock.
    private final Object requestStateChangeLock = new Object();

    public void requestInitialized(Request request, Response response) {
        Agent.LOG.log(Level.FINEST, "Request initialized: {0}", request.getRequestURI());

        synchronized (requestStateChangeLock) {
            ServiceFactory.getStatsService().doStatsWork(
                    StatsWorks.getIncrementCounterWork(MetricNames.SUPPORTABILITY_TRANSACTION_REQUEST_INITIALIZED, 1),
                    MetricNames.SUPPORTABILITY_TRANSACTION_REQUEST_INITIALIZED );
            if (this.isFinished()) {
                return;
            }
            if (dispatcher == null) {
                ExitTracer tracer = AgentBridge.instrumentation.createTracer(null,
                        REQUEST_INITIALIZED_CLASS_SIGNATURE_ID, null, REQUEST_TRACER_FLAGS);

                if (tracer != null) {
                    if (response == null) {
                        response = DUMMY_RESPONSE;
                    }
                    setDispatcher(new WebRequestDispatcher(request, response, this));
                }
            } else {
                // JAVA-825. Ignore multiple requestInitialized() callbacks.
                ServiceFactory.getStatsService().doStatsWork(StatsWorks.getIncrementCounterWork(
                        MetricNames.SUPPORTABILITY_TRANSACTION_REQUEST_INITIALIZED_STARTED, 1),
                        MetricNames.SUPPORTABILITY_TRANSACTION_REQUEST_INITIALIZED_STARTED);
                Agent.LOG.finer("requestInitialized(): transaction already started.");
            }
        }
    }

    public void requestDestroyed() {
        Agent.LOG.log(Level.FINEST, "Request destroyed");

        synchronized (requestStateChangeLock) {
            ServiceFactory.getStatsService().doStatsWork(
                    StatsWorks.getIncrementCounterWork(MetricNames.SUPPORTABILITY_TRANSACTION_REQUEST_DESTROYED, 1),
                    MetricNames.SUPPORTABILITY_TRANSACTION_REQUEST_DESTROYED);
            if (!this.isInProgress()) {
                return;
            }

            Tracer rootTracer = getTransactionActivity().getRootTracer();
            Tracer lastTracer = getTransactionActivity().getLastTracer();
            if (lastTracer != null && rootTracer == lastTracer) {
                Transaction currentTxn = getTransaction(false);
                if (currentTxn != null) {
                    currentTxn.addOutboundResponseHeaders();
                    lastTracer.finish(Opcodes.RETURN, null);
                }
            } else {
                Agent.LOG.log(Level.FINER, "Inconsistent state!  tracer != last tracer for {0} ({1} != {2})", this,
                        rootTracer, lastTracer);
            }
        }
    }

    public static boolean isDummyRequest(Request request) {
        return request == DUMMY_REQUEST;
    }

    public String getApplicationName() {
        return getPriorityApplicationName().getName();
    }

    public PriorityApplicationName getPriorityApplicationName() {
        return appNameAndConfig.get().getName();
    }

    /**
     * Set the application name using a first setter (of a given priority) wins policy. This is the legacy behavior for
     * the public API.
     *
     * @param priority the priority
     * @param appName the name to set if the argument priority is higher than that existing priority.
     */
    public void setApplicationName(ApplicationNamePriority priority, String appName) {
        setApplicationName(priority, appName, false);
    }

    // Set the application name using overridable setter (of a given priority)
    // wins policy. If override is false, the
    // legacy first setter wins policy is applied. If override is true, last
    // setter wins.
    private void setApplicationName(ApplicationNamePriority priority, String appName, boolean override) {
        if (appName == null || appName.length() == 0) {
            return;
        }

        ApplicationNamingPolicy policy = (override) ? SameOrHigherPriorityApplicationNamingPolicy.getInstance()
                : HigherPriorityApplicationNamingPolicy.getInstance();

        // The purpose of the lock is make atomic the "canSetApplicationName()"
        // test with the actual set on the atomic.
        // We are racing with the initialization code; but it only ever sets the
        // default value on appNameAndConfig.
        // Since the default value has the lowest possible application name
        // priority, the initialization code is in
        // effect hard-wired to behave as the lowest-priority setter.
        synchronized (lock) {
            if (policy.canSetApplicationName(this, priority)) {
                String name = stripLeadingForwardSlash(appName);
                PriorityApplicationName pan = PriorityApplicationName.create(name, priority);
                if (pan.equals(getPriorityApplicationName())) {
                    return;
                }
                Agent.LOG.log(Level.FINE, "Set application name to {0}", pan.getName());
                appNameAndConfig.set(new AppNameAndConfig(pan));
            }
        }
    }

    private static String stripLeadingForwardSlash(String appName) {
        final String FORWARD_SLASH = "/";
        if (appName.length() > 1 && appName.startsWith(FORWARD_SLASH)) {
            return appName.substring(1);
        }
        return appName;
    }

    // End application naming

    public long getRunningDurationInNanos() {
        if (dispatcher == null) {
            return 0;
        }
        return transactionTime.getRunningDurationInNanos();
    }

    public void saveMessageParameters(Map<String, String> parameters) {
        MessagingUtil.recordParameters(this, parameters);
    }

    /**
     * This should be the only place that increments the counter.
     */
    public Token getToken() {
        if (ServiceFactory.getServiceManager().getCircuitBreakerService().isTripped()) {
            Agent.LOG.log(Level.FINER, "Transaction {0}: cannot create token, circuit breaker is tripped.", this);
            return NoOpToken.INSTANCE;
        } else if (isIgnore()) {
            Agent.LOG.log(Level.FINER, "Transaction {0}: cannot create token, transaction is ignored.", this);
            return NoOpToken.INSTANCE;
        } else if (!isStarted()) {
            Agent.LOG.log(Level.FINER, "Transaction {0}: cannot create token, transaction not started.", this);
            return NoOpToken.INSTANCE;
        }

        Tracer parent = getTransactionActivity().getLastTracer();
        if (parent == null || parent.isLeaf()) {
            // If we don't have a parent tracer or the parent is a leaf node, we don't want to allow Token creation
            Agent.LOG.log(Level.FINER, "Transaction {0}: cannot create token, no last tracer on {1}.", this,
                    getTransactionActivity());
            return NoOpToken.INSTANCE;
        }

        if (counts.isOverTokenLimit()) {
            Agent.LOG.log(Level.FINER, "Transaction {0}: async token limit {1} exceeded. Ignoring all further async activity",
                    this, counts.getMaxTokens());
            return NoOpToken.INSTANCE;
        }

        TokenImpl token = null;
        boolean wasAdded = false;
        synchronized (lock) {
            if (!isFinished()) {
                token = new TokenImpl(parent);
                activeCount.incrementAndGet();
                counts.getToken();
                TimedSet<TokenImpl> tokenCache = activeTokensCache.get();
                if (tokenCache == null) {
                    activeTokensCache.compareAndSet(null, new TimedTokenSet(ASYNC_TIMEOUT_SECONDS(), TimeUnit.SECONDS, ServiceFactory.getExpirationService()));
                    tokenCache = activeTokensCache.get();
                }
                tokenCache.put(token);
                wasAdded = true;
            }
        }

        if (wasAdded) {
            Agent.LOG.log(Level.FINEST, "Transaction {0}: created active token {1}", this, token);
        } else {
            Agent.LOG.log(Level.FINER, "Transaction {0}: already finished. cannot create token", this);
            // return NoOpToken here to prevent case where execution gets to the synchronized block above but the
            // transaction finishes before that block executes isFinished() resulting in a null token returned below
            return NoOpToken.INSTANCE;
        }

        // Record Token API usage supportability metric
        getMetricAggregator().incrementCounter(AgentBridge.currentApiSource.get().getSupportabilityMetric(
                MetricNames.SUPPORTABILITY_API_TOKEN));

        return token;
    }

    /**
     * This should only ever be called by the token. Otherwise the flag on the token will not get set correctly
     *
     * @param token The token to expire.
     * @return True if the token was expired.
     */
    public static boolean expireToken(TokenImpl token) {
        boolean tokenWasActive = false;
        Transaction tx = token.getTransaction().getTransactionIfExists();
        if (tx != null) {
            synchronized (tx.lock) {
                TimedSet<TokenImpl> tokenCache = tx.activeTokensCache.get();
                if (!tx.isFinished() && tokenCache != null) {
                    tokenWasActive = tokenCache.remove(token);
                    Agent.LOG.log(Level.FINEST, "Transaction {0}: expired token {1}", tx, token);
                } else {
                    Agent.LOG.log(Level.FINER, "Transaction {0}: token {1} is not active and so cannot be expired", tx, token);
                }
            }
        }
        return tokenWasActive;
    }

    public void expireAllTokensForCurrentTransaction() {
        int count = activeCount.get();
        TimedSet<TokenImpl> tokenCache = activeTokensCache.get();
        if (!isFinished() && count > 0 && tokenCache != null) {
            Agent.LOG.log(Level.FINER, "Transaction {0}: forcibly expiring all {1} active tokens", this, count);
            tokenCache.removeAll();
        }
    }

    /**
     * No matter how the token is removed from the cache, check if we need to finish the transaction. This should be
     * the ONLY place that decrements the counter (via checkFinishTransactionFromToken())
     */
    public void onRemoval() {
        checkFinishTransactionFromToken();
    }

    /**
     * Check for any expired tokens or transaction activities.
     */
    public void checkExpire() {
        checkExpireTracedActivities();
        checkExpireTokens();
    }

    private void checkExpireTokens() {
        if (!isFinished()) {
            if (runningChildren.size() == 0) {
                int count = activeCount.get();
                if (count > 0) {
                    long time = transactionTime.getTimeLastTxaFinished() + ASYNC_TIMEOUT_NANO();
                    if (System.nanoTime() >= time) {
                        Agent.LOG.log(Level.FINE, "Transaction {0}: forcibly expiring {1} token(s) due to time out.", this, count);
                        expireAllTokensForCurrentTransaction();
                    }
                }
            }
        }
    }

    private static final String SEGMENT_TXA_DEFAULT_ASYNC_CONTEXT = "activity";
    private static final Object SEGMENT_INVOKER = null;
    // We don't really need a uri, but we create one to build an OtherRootTracer and guarantee that we won't get an NPE.
    // The uri is only used when the tracer starts a transaction. com.newrelic.agent.api.Segments do not start transactions.
    // This should not show up in the UI.
    private static final MetricNameFormat SEGMENT_URI = new SimpleMetricNameFormat(SEGMENT_TXA_DEFAULT_ASYNC_CONTEXT);

    /**
     * Internal implementation of {@link com.newrelic.agent.bridge.Transaction#createAndStartTracedActivity()} . This
     * has to happen inside the transaction class because it requires updates to an async map (runningChildren)
     */
    public Segment startSegment(String category, String segmentName) {
        if (counts.isOverTracerSegmentLimit() || ServiceFactory.getServiceManager().getCircuitBreakerService().isTripped() || isIgnore()) {
            return null;
        }

        Tracer parent = getTransactionActivity().getLastTracer();
        if (parent == null || parent.isLeaf()) {
            // If we don't have a parent tracer or the parent is a leaf node, we don't want to allow a Segment
            Agent.LOG.log(Level.FINER, "Transaction {0}: cannot create event, no last tracer on {1}", this,
                    getTransactionActivity());
            return null;
        }

        // async_context will be set to the name of the thread that finishes this TracedActivity
        TransactionActivity txa = TransactionActivity.createWithoutHolder(this, nextActivityId.getAndIncrement(), SEGMENT_TXA_DEFAULT_ASYNC_CONTEXT);

        ClassMethodSignature cms = new ClassMethodSignature(segmentName, "", "");
        Tracer tracer = new OtherRootTracer(txa, cms, SEGMENT_INVOKER, SEGMENT_URI);
        tracer.setMetricName(category, segmentName);

        AgentBridge.TokenAndRefCount tokenAndRefCount = AgentBridge.activeToken.get();
        if (tokenAndRefCount != null) {
            parent = (Tracer) tokenAndRefCount.tracedMethod.getAndSet(tracer);
        }

        Segment segment = new Segment(parent, tracer);
        txa.setSegment(segment);
        tracer.setParentTracer(parent);
        txa.tracerStarted(tracer);

        Agent.LOG.log(Level.FINEST, "Transaction {0}: startSegment(): {1} created and started with tracer {2}", this, segment, tracer);

        // Record Segment API usage supportability metric
        getMetricAggregator().incrementCounter(AgentBridge.currentApiSource.get().getSupportabilityMetric(
                MetricNames.SUPPORTABILITY_API_SEGMENT));

        return segment;
    }

    /**
     * Internal implementation of {@link com.newrelic.agent.bridge.TracedActivity#ignoreIfUnfinished()} and {@link Segment#ignore()}.
     *
     * @param event The unfinished activity to ignore.
     */
    public void ignoreSegmentIfUnfinished(Segment event) {
        Tracer segmentTracer = event.getTracer();

        MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SEGMENT_IGNORE);

        if (segmentTracer.getTransactionActivity().isFinished()) {
            Agent.LOG.log(Level.FINEST, "Transaction {0}: segment {1} already finished. Not ignoring it.");
        } else {
            segmentTracer.getTransactionActivity().setToIgnore();
            segmentTracer.finish(Opcodes.ARETURN, null);
        }
    }

    /**
     * Internal implementation of {@link com.newrelic.agent.bridge.TracedActivity#finish()} and {@link Segment#end()}.
     * This has to happen inside the transaction class because it requires updates to finishedChildren
     *
     * @param segment The Segment to finish.
     * @param throwable A throwable to finish the activity tracer with. May be null.
     */
    public void finishSegment(Segment segment, Throwable throwable, Tracer parent, String endThreadName) {
        Tracer tracer = segment.getTracer();

        if (!segment.getInitiatingThread().equals(endThreadName)) {
            tracer.setAgentAttribute("async_context", "segment-api");
            tracer.setAgentAttribute(Segment.START_THREAD, segment.getInitiatingThread());
            tracer.setAgentAttribute(Segment.END_THREAD, endThreadName);
        } else {
            tracer.removeAgentAttribute("async_context");
        }

        finishedChildren.add(tracer.getTransactionActivity());

        // set CPU time on txa to zero
        tracer.getTransactionActivity().setTotalCpuTime(0L);
        Agent.LOG.log(Level.FINEST, "{0}--finish segment(): {1} async finish with tracer {2}", this, segment, tracer);
        MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SEGMENT_END);

        // This may have the side-effect of finishing the tx if this is the last tracer. We need to run this last
        finishTracer(throwable, tracer);
    }

    private void finishTracer(final Throwable throwable, final Tracer tracer) {
        if (throwable == null) {
            tracer.finish(Opcodes.ARETURN, null);
        } else {
            tracer.finish(throwable);
        }
    }

    // this should really only be needed for testing
    public int getRunningTransactionActivityCount() {
        return runningChildren.size();
    }

    /**
     * The activity failed to complete normally, typically due to an internal error. Do not use this method to handle
     * errors detected in the instrumented application. All information associated with the activity is lost.
     *
     * @param activity the activity that failed
     * @param opcode
     */
    public void activityFailedOrIgnored(TransactionActivity activity, int opcode) {
        String occurred = activity.isIgnored() ? "IGNORED" : "FAILED";
        Agent.LOG.log(Level.FINER, "Transaction {0}: activity {1} {2} with opcode {3}", this, activity, occurred, opcode);
        synchronized (lock) {
            try {
                if (!isFinished()) {
                    finishedChildren.remove(activity);
                    checkFinishTransaction(activity);
                }
            } finally {
                if (!activity.isNotInThreadLocal()) {
                    transactionHolder.remove();
                }
            }
        }
    }

    private void checkFinishTransactionFromToken() {
        checkFinishTransaction(null, null);
    }

    public void checkFinishTransactionFromActivity(TransactionActivity txa) {
      checkFinishTransaction(txa, null);
    }

    private void checkFinishTransaction(TransactionActivity txa) {
        checkFinishTransaction(txa, txa.hashCode());
    }

    // This secondary method exists only because when we link a Transaction we need to change the hashCode of the
    // txa in progress but we also need to remove the old "nub" so we allow the hashCode to be removed as a parameter.
    private void checkFinishTransaction(TransactionActivity txa, Integer runningChildrenKey) {
        if (isStarted()) {
            if (txa != null) {
                transactionTime.markTxaFinishTime(txa.getRootTracer());
                if (runningChildrenKey != null) {
                    runningChildren.remove(runningChildrenKey);
                }
            }
            if (activeCount.decrementAndGet() == 0) {
                finishTransaction();
            }
        }
    }

    private void threadAssertion() {
        if (Agent.LOG.isFinestEnabled() && !Thread.holdsLock(lock)) {
            Agent.LOG.log(Level.FINEST, THREAD_ASSERTION_FAILURE,
                    new Exception(THREAD_ASSERTION_FAILURE).fillInStackTrace());
        }
    }

    private void checkExpireTracedActivities() {
        // this isn't perfectly timed; it's called during the harvest - let's
        // just do a time call once.
        // It's very important to use System.nanoTime() here. See JAVA-2388 for details.
        long currentMillis = TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
        for (TransactionActivity txa : runningChildren.values()) {
            Segment segment = txa.getSegment();
            if (segment == null || segment.isFinished()) {
                continue;
            }

            Tracer tracer = segment.getTracer();
            if (tracer == null) {
                continue;
            }

            long duration = currentMillis - tracer.getStartTimeInMilliseconds();
            if (duration > SEGMENT_TIMEOUT_MILLIS()) {
                reportTracedActivityTimeout(txa);
                segment.setTruncated();
                setTimeoutCause(TimeoutCause.SEGMENT);
                segment.end();
            }

        }
    }

    // We are logging this at INFO at the request from Customer Support. And because it is error handling code,
    // we take extreme care to avoid secondary exceptions. We know the argument is non-null because it passed
    // the instanceof test in the calling code above, however. The timeout is 600 seconds, but only 3 when unit
    // testing. Here's what the log statement below should look like after running a unit test:
    //
    // Feb 10, 2016 14:00:29 -0800 [28587 1] com.newrelic INFO: Traced activity timed out after 3 seconds. The
    // "segment_timeout" configuration parameter can be used to adjust this timeout. The affected
    // transaction name is "BAR/BAZ".
    private void reportTracedActivityTimeout(TransactionActivity txa) {
        final String message = "Segment timed out after %d seconds. "
                + "The \"segment_timeout\" configuration parameter can be used to adjust this timeout. "
                + "The affected transaction name is %s.%n";
        String name = "unknown";
        if (txa.getTransaction() != null && txa.getTransaction().getTransactionName() != null) {
            name = "\"" + txa.getTransaction().getTransactionName() + "\"";
        }
        Agent.LOG.log(Level.INFO, String.format(message, SEGMENT_TIMEOUT_MILLIS() / 1000, name));
        Agent.LOG.log(Level.FINE, "Segment {0}-{1} timed out on tx {2}", txa.getSegment(), txa, txa.getTransaction());
        NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_ASYNC_FINISH_SEGMENT_NOT_CALLED);
    }

    public void activityFinished(TransactionActivity activity, Tracer tracer, int opcode) {
        Agent.LOG.log(Level.FINER, "Transaction {0}: Activity {1} finished with opcode {2}", this, activity, opcode);

        synchronized (lock) {
            try {
                if (!isFinished()) {
                    // we are assuming if you call this, you are part of the transaction
                    if (!tracer.isTransactionSegment()) {
                        /*
                         * If the root tracer is not a transaction segment then there should not be any tracers in the
                         * txa. It also means we do not need to store the txa for the transaction trace. We just need to
                         * port over metrics. The consequence of this is that you will no longer be able to ignore this
                         * txa once it is finished.
                         */
                        if (txStats == null) {
                            txStats = activity.getTransactionStats();
                        } else {
                            TransactionStats toMergeStats = activity.getTransactionStats();
                            txStats.getScopedStats().mergeStats(toMergeStats.getScopedStats());
                            txStats.getUnscopedStats().mergeStats(toMergeStats.getUnscopedStats());
                        }
                    } else {
                        finishedChildren.add(activity);
                    }

                    checkFinishTransaction(activity);
                }
            } finally {
                if (!activity.isNotInThreadLocal()) {
                    transactionHolder.remove();
                }
            }
        }
    }

    public Set<TransactionActivity> getFinishedChildren() {
        synchronized (lock) {
            return new HashSet<>(finishedChildren);
        }
    }

    public float getPriority() {
        Float priority = this.priority.get();
        return priority == null ? 0.0f : priority;
    }

    public void setPriorityIfNotNull(Float priority) {
        if (priority != null) {
            this.priority.set(priority);
        }
    }

    public boolean sampled() {
        Float priority = this.priority.get();
        return priority != null && isSampledPriority(priority);
    }

    private String getTransactionName() {
        // getName may return null
        String fullName = getPriorityTransactionName().getName();
        String category = getPriorityTransactionName().getCategory();
        String prefix = getPriorityTransactionName().getPrefix();
        String txnNamePrefix = prefix + MetricNames.SEGMENT_DELIMITER + category + MetricNames.SEGMENT_DELIMITER;
        if (fullName != null && fullName.startsWith(txnNamePrefix)) {
            return fullName.substring(txnNamePrefix.length());
        }
        return fullName;
    }

    @VisibleForTesting
    int getCountOfRunningAndFinishedTransactionActivities() {
        synchronized (lock) {
            return runningChildren.size() + finishedChildren.size();
        }
    }

    public SecurityMetaData getSecurityMetaData() {
        return securityMetaData;
    }
}
