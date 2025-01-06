/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.attributes.AttributeValidator;
import com.newrelic.agent.bridge.datastore.UnknownDatabaseVendor;
import com.newrelic.agent.bridge.external.ExternalMetrics;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.DatastoreConfig;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.DatastoreMetrics;
import com.newrelic.agent.database.ParsedDatabaseStatement;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionGuidFactory;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.util.ExternalsUtil;
import com.newrelic.agent.util.Strings;
import com.newrelic.api.agent.CloudParameters;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.CloudParameters;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.SlowQueryDatastoreParameters;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Opcodes;

import java.net.URI;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * The default tracer implementation.
 */
public class DefaultTracer extends AbstractTracer {

    // Tracers MUST NOT store references to the Transaction. Why: tracers are stored in the TransactionActivity,
    // and Activities can be reparented from one Transaction to another by the public APIs that support async.

    /**
     * Convenience formatter
     */
    public static final MetricNameFormat NULL_METRIC_NAME_FORMATTER = new SimpleMetricNameFormat(null);
    public static final String BACKTRACE_PARAMETER_NAME = "backtrace";

    /**
     * By default tracers generate scoped metrics and generate transaction tracer segments.
     */
    public static final int DEFAULT_TRACER_FLAGS = TracerFlags.TRANSACTION_TRACER_SEGMENT
            | TracerFlags.GENERATE_SCOPED_METRIC;

    private final long startTime;
    private final long timestamp;
    private long duration;
    private long exclusiveDuration;
    private Tracer parentTracer;
    private String guid;

    private final ClassMethodSignature classMethodSignature;
    private Object invocationTarget;
    private MetricNameFormat metricNameFormat;

    private boolean isParent;
    private boolean addedOutboundRequestHeaders;

    /*
     * If the child collected the stack trace, then we still take a trace if over time, but we should not add to the
     * stack trace count.
     */
    private boolean childHasStackTrace;
    private byte tracerFlags;
    private int childCount = 0;

    // values applied at the finish of the tracer
    private ExternalParameters externalParameters = null;
    private InboundHeaders inboundResponseHeaders = null;

    /**
     * Compatibility constructor for old code. Tracers are always bound to the thread they're created on, while
     * transactions are multithreaded. So in theory, we should completely ignore the transaction argument and grab the
     * activity from the current thread. Unfortunately, there are unit tests that pass mocks here, and it's preferable
     * that these tests not have to set up thread local storage in order to exercise the tracer code. So we actually
     * call getTransactionActivity() on the transaction, because the test code can mock this to return to the mock
     * TransactionActivity. (In a real setting, the call transaction.getTransactionActivity() also digs the Activity out
     * of the thread local; the mapping from transactions to activities is one-to-many, so there's no other way to do
     * it.)
     *
     * @param transaction transaction, must not be null.
     * @param sig
     * @param object
     * @param metricNameFormatter
     * @param tracerFlags
     */
    public DefaultTracer(Transaction transaction, ClassMethodSignature sig, Object object,
            MetricNameFormat metricNameFormatter, int tracerFlags) {
        this(transaction.getTransactionActivity(), sig, object, metricNameFormatter, tracerFlags);
    }

    public DefaultTracer(TransactionActivity txa, ClassMethodSignature sig, Object object,
            MetricNameFormat metricNameFormatter, int tracerFlags) {
        this(txa, sig, object, metricNameFormatter, tracerFlags, System.nanoTime());
    }

    /**
     * Primary constructor for tracers created from weaved code or XML instrumentation.
     *
     * @param txa activity, must not be null.
     * @param sig
     * @param object
     * @param metricNameFormatter
     * @param tracerFlags
     */
    public DefaultTracer(TransactionActivity txa, ClassMethodSignature sig, Object object,
            MetricNameFormat metricNameFormatter, int tracerFlags, long pStartTime) {
        super(txa, new AttributeValidator(ATTRIBUTE_TYPE));
        metricNameFormat = metricNameFormatter;
        classMethodSignature = sig;
        startTime = pStartTime;
        timestamp = System.currentTimeMillis();
        invocationTarget = object;
        parentTracer = txa.getLastTracer();
        if (!txa.canCreateTransactionSegment()) {
            // Over segment limit; prevent segment creation.
            tracerFlags = TracerFlags.clearSegment(tracerFlags);
        }

        this.tracerFlags = (byte) tracerFlags;
    }

    public DefaultTracer(TransactionActivity txa, ClassMethodSignature sig, Object object,
            MetricNameFormat metricNameFormatter, long pStartTime) {
        this(txa, sig, object, metricNameFormatter, DEFAULT_TRACER_FLAGS, pStartTime);
    }

    public DefaultTracer(Transaction transaction, ClassMethodSignature sig, Object object,
            MetricNameFormat metricNameFormatter) {
        this(transaction, sig, object, metricNameFormatter, DEFAULT_TRACER_FLAGS);
    }

    public DefaultTracer(Transaction transaction, ClassMethodSignature sig, Object object) {
        this(transaction, sig, object, NULL_METRIC_NAME_FORMATTER);
    }

    @Override
    public void removeTransactionSegment() {
        this.tracerFlags = (byte) TracerFlags.clearSegment(this.tracerFlags);
    }

    @Override
    public String getGuid() {
        if (this.guid == null) {
            this.guid = TransactionGuidFactory.generate16CharGuid();
        }
        return this.guid;
    }

    @Override
    public void finish(Throwable throwable) {
        Transaction tx = getTransaction();
        if (tx == null) {
            // This is either a serious internal error, or the application
            // used "@Trace(async = true)" and never called startAsyncActivity()
            // to associate with a Transaction. Either way, don't leak the
            // Activity as a stale ThreadLocal.
            TransactionActivity.clear();
            return;
        }
        setThrownException(throwable);
        tx.noticeTracerException(throwable, getGuid());

        if (!tx.getTransactionState().finish(tx, this)) {
            return;
        }
        try {
            getTransactionActivity().lockTracerStart();
            doFinish(throwable);
        } catch (Throwable t) {
            String msg = MessageFormat.format("An error occurred finishing tracer for class {0} : {1}",
                    classMethodSignature.getClassName(), t);
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.WARNING, msg, t);
            } else {
                Agent.LOG.warning(msg);
            }
        } finally {
            getTransactionActivity().unlockTracerStart();
        }

        finish(Opcodes.ATHROW, null);

        if (Agent.isDebugEnabled()) {
            Agent.LOG.log(Level.FINE, "(Debug) Tracer.finish(Throwable)");
        }
    }

    protected void reset() {
        invocationTarget = null;
    }

    @Override
    public void finish(int opcode, Object returnValue) {

        TransactionActivity txa = getTransactionActivity();
        if (txa == null) {
            // Internal error - null txa is permitted for
            // a weird legacy Play instrumentation case
            Agent.LOG.log(Level.FINER, "Transaction activity for {0} was null", this);
            return;
        }

        // Get transaction from this tracer's txa.
        Transaction tx = getTransaction();
        if (tx != null && !tx.getTransactionState().finish(tx, this)) {
            return;
        }

        performFinishWork(finishTime.get() == null ? System.nanoTime() : finishTime.get(), opcode, returnValue);
    }

    // this is public for testing - do not call directly unless testing
    public void performFinishWork(long finishTime, int opcode, Object returnValue) {
        // Believe it or not, it's possible to get a negative value!
        // (At least on some old, broken Linux kernels)
        duration = Math.max(0, finishTime - getStartTime());
        exclusiveDuration += duration;
        if (exclusiveDuration < 0 || exclusiveDuration > duration) {
            if (NewRelic.getAgent().getConfig().getValue(AgentConfigImpl.METRIC_DEBUG, AgentConfigImpl.DEFAULT_METRIC_DEBUG)) {
                Agent.LOG.log(Level.INFO, "Invalid exclusive time {0} for metric {1}", exclusiveDuration,
                        NewRelic.getAgent().getTransaction().getTracedMethod().getMetricName());
            } else {
                Agent.LOG.log(Level.FINE, "Invalid exclusive time {0} for metric {1}", exclusiveDuration,
                        NewRelic.getAgent().getTransaction().getTracedMethod().getMetricName());
            }
            exclusiveDuration = duration;
        }

        getTransactionActivity().lockTracerStart();
        try {
            try {
                if (Opcodes.ATHROW != opcode) {
                    doFinish(opcode, returnValue);
                }
            } catch (Throwable t) {
                String msg = MessageFormat.format("An error occurred finishing tracer for class {0} : {1}",
                        classMethodSignature.getClassName(), t.toString());
                Agent.LOG.severe(msg);
                Agent.LOG.log(Level.FINER, msg, t);
            }

            try {
                attemptToStoreStackTrace();
            } catch (Throwable t) {
                if (Agent.LOG.isFinestEnabled()) {
                    String msg = MessageFormat.format("An error occurred getting stack trace for class {0} : {1}",
                            classMethodSignature.getClassName(), t.toString());
                    Agent.LOG.log(Level.FINEST, msg, t);
                }
            }

            if (impactsParent(parentTracer)) {
                parentTracer.childTracerFinished(this);
            }

            try {
                recordMetrics(getTransactionActivity().getTransactionStats());
            } catch (Throwable t) {
                String msg = MessageFormat.format("An error occurred recording tracer metrics for class {0} : {1}",
                        classMethodSignature.getClassName(), t.toString());
                Agent.LOG.severe(msg);
                Agent.LOG.log(Level.FINER, msg, t);
            }

            try {
                setAgentAttribute(AttributeNames.THREAD_ID, getTransactionActivity().getThreadId());
                if (classMethodSignature != null && getTransaction() != null &&
                        ServiceFactory.getConfigService().getDefaultAgentConfig().getCodeLevelMetricsConfig().isEnabled()) {
                    String className = classMethodSignature.getClassName();
                    String methodName = classMethodSignature.getMethodName();

                    // tracers created by the API will only have the className and should be ignored
                    if (StringUtils.isNotEmpty(className) && StringUtils.isNotEmpty(methodName)) {
                        setAgentAttribute(AttributeNames.CLM_NAMESPACE, className);
                        setAgentAttribute(AttributeNames.CLM_FUNCTION, methodName);
                    }
                }
            } catch (Throwable t) {
                String msg = "An error occurred saving the clm attributes: " + t;
                Agent.LOG.severe(msg);
                Agent.LOG.log(Level.FINER, msg, t);
            }

            try {
                if (!(this instanceof SkipTracer)) {
                    getTransactionActivity().tracerFinished(this, opcode);
                }
            } catch (Throwable t) {
                String msg = MessageFormat.format(
                        "Tracer Debug: An error occurred calling Transaction.tracerFinished() for class {0} : {1} : this Tracer = {2}",
                        classMethodSignature.getClassName(), t.toString(), this);

                Agent.LOG.severe(msg);
                Agent.LOG.log(Level.FINER, msg, t);
            }
            reset();
        } finally {
            getTransactionActivity().unlockTracerStart();
        }
    }

    @Override
    public long getStartTimeInMillis() {
        return timestamp;
    }

    @Override
    public ExternalParameters getExternalParameters() {
        return externalParameters;
    }

    /**
     * Subclasses may override.
     */
    protected void doFinish(Throwable throwable) {
    }

    /**
     * Subclasses may override.
     */
    protected void doFinish(int opcode, Object returnValue) {
    }

    protected boolean shouldStoreStackTrace() {
        return isTransactionSegment();
    }

    private void attemptToStoreStackTrace() {
        if (getTransaction() != null && shouldStoreStackTrace()) {
            TransactionTracerConfig transactionTracerConfig = getTransaction().getTransactionTracerConfig();
            double stackTraceThresholdInNanos = transactionTracerConfig.getStackTraceThresholdInNanos();
            int stackTraceMax = transactionTracerConfig.getMaxStackTraces();
            // you must be over the duration and either child has taken a stack trace or we are under the stack trace
            // count
            if ((getDuration() > stackTraceThresholdInNanos)
                    && (childHasStackTrace || (getTransaction().getTransactionCounts().getStackTraceCount() < stackTraceMax))) {
                storeStackTrace();
                // only increment the stack trace count if there are no children which have taken a stack trace
                if (!childHasStackTrace) {
                    getTransaction().getTransactionCounts().incrementStackTraceCount();
                    // this property is used to tell parents not to increment the stack trace count
                    childHasStackTrace = true;
                }
            }
        }
    }

    /**
     * Stores the stack trace. This is public for testing, but really should be private and so please do not call it
     * unless you are in a test.
     */
    public void storeStackTrace() {
        setAgentAttribute(BACKTRACE_PARAMETER_NAME, Thread.currentThread().getStackTrace());
    }

    @Override
    public long getRunningDurationInNanos() {
        return duration > 0 ? duration : Math.max(0, System.nanoTime() - getStartTime());
    }

    @Override
    public long getDurationInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(getDuration(), TimeUnit.NANOSECONDS);
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public long getExclusiveDuration() {
        return exclusiveDuration;
    }

    @Override
    public long getEndTime() {
        return getStartTime() + duration;
    }

    @Override
    public long getEndTimeInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(getEndTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public long getStartTimeInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(getStartTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    protected final Object getInvocationTarget() {
        return invocationTarget;
    }

    @Override
    public Tracer getParentTracer() {
        return parentTracer;
    }

    @Override
    public void setParentTracer(Tracer tracer) {
        parentTracer = tracer;
    }

    public String getRequestMetricName() {
        return null;
    }

    public void setMetricNameFormat(MetricNameFormat nameFormat) {
        metricNameFormat = nameFormat;
    }

    protected final MetricNameFormat getMetricNameFormat() {
        return metricNameFormat;
    }

    @Override
    public final String getMetricName() {
        return metricNameFormat == null ? null : metricNameFormat.getMetricName();
    }

    @Override
    public final String getTransactionSegmentName() {
        return metricNameFormat == null ? null : metricNameFormat.getTransactionSegmentName();
    }

    @Override
    public final String getTransactionSegmentUri() {
        return metricNameFormat == null ? null : metricNameFormat.getTransactionSegmentUri();
    }

    /**
     * Record response time metrics.
     */
    protected void recordMetrics(TransactionStats transactionStats) {
        try {
            recordExternalMetrics();
        } catch (Throwable t) {
            String msg = MessageFormat.format("An error occurred recording external metrics for class {0} : {1}",
                    classMethodSignature.getClassName(), t.toString());
            Agent.LOG.severe(msg);
            Agent.LOG.log(Level.FINER, msg, t);
        }
        if (getTransaction() == null || getTransaction().isIgnore()) {
            return;
        }
        if (isMetricProducer()) {
            String metricName = getMetricName();
            if (metricName != null) {
                // record the scoped metrics
                ResponseTimeStats stats = transactionStats.getScopedStats().getOrCreateResponseTimeStats(metricName);
                stats.recordResponseTimeInNanos(getDuration(), getExclusiveDuration());

                // there is now an unscoped metric for every scoped metric
                // the unscoped metric is created in the StatsEngineImpl

            }
            if (getRollupMetricNames() != null) {
                for (String name : getRollupMetricNames()) {
                    ResponseTimeStats stats = transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(name);
                    stats.recordResponseTimeInNanos(getDuration(), getExclusiveDuration());
                }
            }
            if (getExclusiveRollupMetricNames() != null) {
                for (String name : getExclusiveRollupMetricNames()) {
                    ResponseTimeStats stats = transactionStats.getUnscopedStats().getOrCreateResponseTimeStats(name);
                    stats.recordResponseTimeInNanos(getExclusiveDuration(), getExclusiveDuration());
                }
            }
            doRecordMetrics(transactionStats);
        }
    }

    /**
     * Generate additional metrics. Subclasses may override.
     */
    protected void doRecordMetrics(TransactionStats transactionStats) {
    }

    @Override
    public final boolean isParent() {
        return isParent;
    }

    @Override
    public void childTracerFinished(Tracer child) {
        if (child.isMetricProducer() && !(child instanceof SkipTracer)) {
            childCount++;
            exclusiveDuration -= child.getDuration();
            if (isTransactionSegment() && child.isTransactionSegment()) {
                isParent = true;
                if (child.isChildHasStackTrace()) {
                    childHasStackTrace = true;
                }
            }
        }
    }

    @Override
    public int getChildCount() {
        return childCount;
    }

    private boolean impactsParent(Tracer parent) {
        return (parent != null && parent.getTransactionActivity() == this.getTransactionActivity());
    }

    public void childTracerFinished(long childDurationInNanos) {
        exclusiveDuration -= childDurationInNanos;
    }

    @Override
    public ClassMethodSignature getClassMethodSignature() {
        return classMethodSignature;
    }

    @Override
    public final boolean isTransactionSegment() {
        return (tracerFlags & TracerFlags.TRANSACTION_TRACER_SEGMENT) == TracerFlags.TRANSACTION_TRACER_SEGMENT;
    }

    @Override
    public boolean isMetricProducer() {
        return (tracerFlags & TracerFlags.GENERATE_SCOPED_METRIC) == TracerFlags.GENERATE_SCOPED_METRIC;
    }

    @Override
    public final boolean isLeaf() {
        return (tracerFlags & TracerFlags.LEAF) == TracerFlags.LEAF;
    }

    @Override
    public final boolean isAsync() {
        return TracerFlags.isAsync(tracerFlags);
    }

    /**
     * Gets the field childHasStackTrace.
     *
     * @return the childHasStackTrace
     */
    @Override
    public boolean isChildHasStackTrace() {
        return childHasStackTrace;
    }

    @Override
    public TransactionSegment getTransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator,
            long startTime, TransactionSegment lastSibling) {
        return new TransactionSegment(ttConfig, sqlObfuscator, startTime, this);
    }

    @Override
    public void setMetricName(String... metricNameParts) {
        String metricName = Strings.join(MetricNames.SEGMENT_DELIMITER, metricNameParts);
        if (metricName != null) {
            setMetricNameFormat(new SimpleMetricNameFormat(metricName));
        }
        MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SEGMENT_SET_METRIC_NAME);
    }

    @Override
    public void setMetricNameFormatInfo(String metricName, String transactionSegmentName, String transactionSegmentUri) {
        MetricNameFormat format = new SimpleMetricNameFormat(metricName, transactionSegmentName, transactionSegmentUri);
        setMetricNameFormat(format);
    }

    @Override
    public void addOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
        Transaction transaction = getTransactionActivity().getTransaction();
        if (transaction == null) {
            Agent.LOG.log(Level.FINE, "Could not set outbound headers: There is no transaction yet.");
            return;
        }

        transaction.getCrossProcessState().processOutboundRequestHeaders(outboundHeaders, this);
        addedOutboundRequestHeaders = true;
    }

    @Override
    public void readInboundResponseHeaders(InboundHeaders inboundResponseHeaders) {
        Agent.LOG.log(Level.FINE, "Setting inboundResponseHeaders to: " + inboundResponseHeaders);
        this.inboundResponseHeaders = inboundResponseHeaders;
    }

    @Override
    public void reportAsExternal(com.newrelic.agent.bridge.external.ExternalParameters externalParameters) {
        reportAsExternal((ExternalParameters) externalParameters);
    }

    @Override
    public void reportAsExternal(ExternalParameters externalParameters) {
        if (Agent.LOG.isFineEnabled()) {
            Agent.LOG.log(Level.FINE, "Setting externalParameters to: " + externalParameters);
        }
        MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_REPORT_AS_EXTERNAL);

        this.externalParameters = externalParameters;
        if (this.externalParameters instanceof HttpParameters) {
            // URI validity check and logging
            HttpParameters httpParameters = (HttpParameters) this.externalParameters;
            URI uri = httpParameters.getUri();
            if (uri == null || uri.getScheme() == null || uri.getHost() == null || uri.getPort() == -1) {
                Agent.LOG.log(Level.FINE, "URI parameter passed to HttpParameters should include a valid scheme, host, and port.");
            }

            InboundHeaders headers = httpParameters.getInboundResponseHeaders();
            if (null != headers) {
                readInboundResponseHeaders(headers);
            }
        } else if (this.externalParameters instanceof MessageProduceParameters) {
            catForMessaging(((MessageProduceParameters) this.externalParameters));
        } else if (this.externalParameters instanceof MessageConsumeParameters) {
            catForMessaging(((MessageConsumeParameters) this.externalParameters));
        }
    }

    /**
     * If {@link DefaultTracer#reportAsExternal(ExternalParameters)} reportAsExternal has been called, add the
     * appropriate rollup metrics.
     */
    private void recordExternalMetrics() {
        if (null != externalParameters) {
            if (externalParameters instanceof DatastoreParameters) {
                recordExternalMetricsDatastore((DatastoreParameters) externalParameters);
                if (externalParameters instanceof SlowQueryDatastoreParameters) {
                    recordSlowQueryData((SlowQueryDatastoreParameters) externalParameters);
                }
            } else if (externalParameters instanceof GenericParameters) {
                recordExternalMetricsGeneric((GenericParameters) externalParameters);
            } else if (externalParameters instanceof HttpParameters) {
                recordExternalMetricsHttp((HttpParameters) externalParameters);
            } else if (externalParameters instanceof MessageProduceParameters) {
                recordMessageBrokerMetrics((MessageProduceParameters) this.externalParameters);
            } else if (externalParameters instanceof MessageConsumeParameters) {
                recordMessageBrokerMetrics((MessageConsumeParameters) this.externalParameters);
            } else if (externalParameters instanceof CloudParameters) {
                recordFaasAttributes((CloudParameters) externalParameters);
            } else {
                Agent.LOG.log(Level.SEVERE, "Unknown externalParameters type. This should not happen. {0} -- {1}",
                        externalParameters, externalParameters.getClass());
            }
        } else if (null == externalParameters && null != inboundResponseHeaders) {
            Agent.LOG.log(Level.FINE,
                    "Warning: readInboundResponseHeaders was called without a call to reportAsExternal. Inbound headers will not take effect.");
        }
    }

    /**
     * {@link ExternalParameters} which are coupled with cat should use this method.
     */
    private void recordInboundResponseHeaders(InboundHeaders inboundResponseHeaders, String host, String uri) {
        Transaction transaction = getTransactionActivity().getTransaction();
        if (addedOutboundRequestHeaders && inboundResponseHeaders != null) {
            transaction.getCrossProcessState().processInboundResponseHeaders(inboundResponseHeaders, this, host, uri, false);
            if (metricNameFormat instanceof CrossProcessNameFormat) {
                addRollupMetricName(
                        ((CrossProcessNameFormat) metricNameFormat).getHostCrossProcessIdRollupMetricName());
            }
        }
    }

    private void recordExternalMetricsGeneric(GenericParameters externalParameters) {
        Transaction transaction = getTransactionActivity().getTransaction();
        URI uri = ExternalsUtil.sanitizeURI(externalParameters.getUri());
        String host = uri == null ? ExternalMetrics.UNKNOWN_HOST : uri.getHost();
        String uriStr = uri == null ? ExternalMetrics.UNKNOWN_HOST : uri.toString();

        String library = externalParameters.getLibrary();
        String procedure = externalParameters.getProcedure();

        ExternalMetrics.makeExternalComponentTrace(transaction.isWebTransaction(), this, host, library, true,
                uriStr, procedure);

        recordInboundResponseHeaders(inboundResponseHeaders, host, uriStr);
    }

    private void recordExternalMetricsHttp(HttpParameters externalParameters) {
        Transaction transaction = getTransactionActivity().getTransaction();
        URI uri = ExternalsUtil.sanitizeURI(externalParameters.getUri());
        String host = uri == null ? ExternalMetrics.UNKNOWN_HOST : uri.getHost();
        String uriStr = uri == null ? ExternalMetrics.UNKNOWN_HOST : uri.toString();

        String library = externalParameters.getLibrary();
        setAgentAttribute(AttributeNames.COMPONENT, library);
        String procedure = externalParameters.getProcedure();
        setAgentAttribute(AttributeNames.HTTP_METHOD, procedure);

        ExternalMetrics.makeExternalComponentTrace(transaction.isWebTransaction(), this, host, library, true,
                uriStr, procedure);

        recordInboundResponseHeaders(inboundResponseHeaders, host, uriStr);
    }

    private void recordExternalMetricsDatastore(DatastoreParameters datastoreParameters) {
        Transaction tx = getTransactionActivity().getTransaction();
        if (tx != null && datastoreParameters != null) {
            final String collection = getCollection(datastoreParameters);
            DatastoreMetrics.collectDatastoreMetrics(datastoreParameters.getProduct(), tx, this,
                    collection, datastoreParameters.getOperation(),
                    datastoreParameters.getHost(), datastoreParameters.getPort(), datastoreParameters.getPathOrId(),
                    datastoreParameters.getDatabaseName());

            DatastoreConfig datastoreConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getDatastoreConfig();
            if (datastoreConfig.isInstanceReportingEnabled()) {
                boolean allUnknown = datastoreParameters.getHost() == null && datastoreParameters.getPort() == null
                        && datastoreParameters.getPathOrId() == null;
                if (!allUnknown) {
                    setAgentAttribute(DatastoreMetrics.DATASTORE_HOST, DatastoreMetrics.replaceLocalhost(datastoreParameters.getHost()));
                    setAgentAttribute(DatastoreMetrics.DATASTORE_PORT_PATH_OR_ID, DatastoreMetrics.getIdentifierOrPort(
                            datastoreParameters.getPort(), datastoreParameters.getPathOrId()));
                }
                if (datastoreParameters.getCloudResourceId() != null) {
                    setAgentAttribute(AttributeNames.CLOUD_RESOURCE_ID, datastoreParameters.getCloudResourceId());
                }
            }

            // Spec says this is a should, only send database name when we actually have one.
            if (datastoreConfig.isDatabaseNameReportingEnabled() && datastoreParameters.getDatabaseName() != null) {
                setAgentAttribute(DatastoreMetrics.DB_INSTANCE, datastoreParameters.getDatabaseName());
            }
        } else {
            Agent.LOG.log(Level.FINE,
                    "Datastore metrics will not be applied because the tracer is not in a transaction.");
        }
    }

    private String getCollection(DatastoreParameters datastoreParameters) {
        final String collection = datastoreParameters.getCollection();
        if (collection == null && datastoreParameters instanceof SlowQueryDatastoreParameters) {
            final Object rawQuery = ((SlowQueryDatastoreParameters)datastoreParameters).getRawQuery();
            if (rawQuery != null) {
                ParsedDatabaseStatement databaseStatement = ServiceFactory.getDatabaseService().
                        getDatabaseStatementParser().getParsedDatabaseStatement(
                        UnknownDatabaseVendor.INSTANCE, rawQuery.toString(), null);
                if (databaseStatement.recordMetric()) {
                    return databaseStatement.getModel();
                }
            }
            return "unknown";
        }
        return collection;
    }

    private void catForMessaging(MessageProduceParameters produceParameters) {
        OutboundHeaders outboundHeaders = produceParameters.getOutboundHeaders();
        if (outboundHeaders == null) {
            return;
        }

        // In amqp we don't know if we're sending a request or a response.
        DestinationType destinationType = produceParameters.getDestinationType();
        if (destinationType == DestinationType.EXCHANGE) {
            addOutboundRequestHeaders(outboundHeaders);
        }
        // Messaging CAT logic ported from instrumentation modules. We should revisit this with PM. Same applies to
        // consume.
        // This assumes that when the application is:
        // 1. Producing a message to a named queue, it is making a request (outbound request).
        // 2. Producing a message to a temporary queue, it is writing a response (outbound response).
        else if (destinationType == DestinationType.NAMED_QUEUE || destinationType == DestinationType.NAMED_TOPIC) {
            addOutboundRequestHeaders(outboundHeaders);
        } else if (destinationType == DestinationType.TEMP_QUEUE || destinationType == DestinationType.TEMP_TOPIC) {
            getTransaction().getCrossProcessState().processOutboundResponseHeaders(outboundHeaders, -1);
        } else {
            Agent.LOG.log(Level.FINE, "Unexpected destination type when recording CAT metrics for message produce.");
        }
    }

    private void catForMessaging(MessageConsumeParameters consumeParameters) {
        InboundHeaders headers = consumeParameters.getInboundHeaders();
        if (headers == null) {
            return;
        }

        // In amqp we don't know if we're sending a request or a response.
        DestinationType destinationType = consumeParameters.getDestinationType();
        if (destinationType == DestinationType.EXCHANGE) {
            getTransaction().provideHeaders(headers);
        }
        // This assumes that when the application is:
        // 1. Consuming a message from a named queue, it is processing a request (inbound request).
        // 2. Consuming a message from a temporary queue, it is processing a response (inbound response).
        else if (destinationType == DestinationType.NAMED_QUEUE || destinationType == DestinationType.NAMED_TOPIC) {
            getTransaction().provideHeaders(headers);
        } else if (destinationType == DestinationType.TEMP_QUEUE || destinationType == DestinationType.TEMP_TOPIC) {
            // Do not replace with recordInboundResponseHeaders.
            // recordInboundResponseHeaders assumes we do CAT request/response in the same tracer.
            Transaction transaction = getTransactionActivity().getTransaction();
            transaction.getCrossProcessState().processInboundResponseHeaders(headers, this, "Unknown", null, true);
        } else {
            Agent.LOG.log(Level.FINE,
                    "Unexpected destination type when reporting external metrics for message consume.");
        }
    }

    private void recordMessageBrokerMetrics(MessageProduceParameters messageProduceParameters) {
        DestinationType destinationType = messageProduceParameters.getDestinationType();

        if (destinationType == DestinationType.EXCHANGE) {
            setMetricName(MessageFormat.format(MetricNames.MESSAGE_BROKER_PRODUCE_NAMED,
                    messageProduceParameters.getLibrary(), messageProduceParameters.getDestinationType().getTypeName(),
                    messageProduceParameters.getDestinationName()));
        } else if (destinationType == DestinationType.NAMED_QUEUE || destinationType == DestinationType.NAMED_TOPIC) {
            setMetricName(MessageFormat.format(MetricNames.MESSAGE_BROKER_PRODUCE_NAMED,
                    messageProduceParameters.getLibrary(), messageProduceParameters.getDestinationType().getTypeName(),
                    messageProduceParameters.getDestinationName()));
        } else if (destinationType == DestinationType.TEMP_QUEUE || destinationType == DestinationType.TEMP_TOPIC) {
            setMetricName(MessageFormat.format(MetricNames.MESSAGE_BROKER_PRODUCE_TEMP,
                    messageProduceParameters.getLibrary(),
                    messageProduceParameters.getDestinationType().getTypeName()));
        }
        if (messageProduceParameters.getHost() != null) {
            setAgentAttribute(AttributeNames.SERVER_ADDRESS, messageProduceParameters.getHost());
        }
        if (messageProduceParameters.getPort() != null) {
            setAgentAttribute(AttributeNames.SERVER_PORT, messageProduceParameters.getPort());
        }
    }

    private void recordMessageBrokerMetrics(MessageConsumeParameters messageConsumeParameters) {
        DestinationType destinationType = messageConsumeParameters.getDestinationType();

        if (destinationType == DestinationType.EXCHANGE) {
            setMetricName(MessageFormat.format(MetricNames.MESSAGE_BROKER_CONSUME_NAMED,
                    messageConsumeParameters.getLibrary(), messageConsumeParameters.getDestinationType().getTypeName(),
                    messageConsumeParameters.getDestinationName()));
        } else if (destinationType == DestinationType.NAMED_QUEUE || destinationType == DestinationType.NAMED_TOPIC) {
            setMetricName(MessageFormat.format(MetricNames.MESSAGE_BROKER_CONSUME_NAMED,
                    messageConsumeParameters.getLibrary(), messageConsumeParameters.getDestinationType().getTypeName(),
                    messageConsumeParameters.getDestinationName()));

        } else if (destinationType == DestinationType.TEMP_QUEUE || destinationType == DestinationType.TEMP_TOPIC) {
            setMetricName(MessageFormat.format(MetricNames.MESSAGE_BROKER_CONSUME_TEMP,
                    messageConsumeParameters.getLibrary(),
                    messageConsumeParameters.getDestinationType().getTypeName()));
        }
        if (messageConsumeParameters.getHost() != null) {
            setAgentAttribute(AttributeNames.SERVER_ADDRESS, messageConsumeParameters.getHost());
        }
        if (messageConsumeParameters.getPort() != null) {
            setAgentAttribute(AttributeNames.SERVER_PORT, messageConsumeParameters.getPort());
        }
    }

    private void recordFaasAttributes(CloudParameters cloudParameters) {
        if (cloudParameters.getPlatform() != null) {
            setAgentAttribute(AttributeNames.CLOUD_PLATFORM, cloudParameters.getPlatform());
        }
        if (cloudParameters.getResourceId() != null) {
            setAgentAttribute(AttributeNames.CLOUD_RESOURCE_ID, cloudParameters.getResourceId());
        }
    }

    private <T> void recordSlowQueryData(SlowQueryDatastoreParameters<T> slowQueryDatastoreParameters) {
        Transaction transaction = getTransactionActivity().getTransaction();
        if (transaction != null && slowQueryDatastoreParameters.getRawQuery() != null
                && slowQueryDatastoreParameters.getQueryConverter() != null) {
            // Attempt to record the slow query if it's above the threshold
            transaction.getSlowQueryListener(true).noticeTracer(this, slowQueryDatastoreParameters);
        }
    }
}
