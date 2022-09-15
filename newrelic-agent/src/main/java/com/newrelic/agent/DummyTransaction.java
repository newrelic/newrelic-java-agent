/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.application.PriorityApplicationName;
import com.newrelic.agent.bridge.CrossProcessState;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.WebResponse;
import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.logging.LogAttributeKey;
import com.newrelic.agent.browser.BrowserTransactionState;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.CrossProcessConfig;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.DatabaseStatementParser;
import com.newrelic.agent.database.ParsedDatabaseStatement;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.normalization.Normalizer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.sql.NopSlowQueryListener;
import com.newrelic.agent.sql.SlowQueryListener;
import com.newrelic.agent.stats.AbstractMetricAggregator;
import com.newrelic.agent.stats.ApdexStats;
import com.newrelic.agent.stats.ApdexStatsImpl;
import com.newrelic.agent.stats.DataUsageStats;
import com.newrelic.agent.stats.DataUsageStatsImpl;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.ResponseTimeStatsImpl;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.Stats;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.stats.StatsImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionGuidFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.MetricNameFormatWithHost;
import com.newrelic.agent.tracers.NoOpTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.transaction.TransactionCache;
import com.newrelic.agent.transaction.TransactionCounts;
import com.newrelic.agent.transaction.TransactionNamingPolicy;
import com.newrelic.agent.transaction.TransactionThrowable;
import com.newrelic.agent.transaction.TransactionTimer;
import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.Logs;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.TransactionNamePriority;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.sql.ResultSetMetaData;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * DummyTransaction is a lightweight Transaction that gets returned when the agent's circuit
 * breaker has been tripped in order to minimize the agent's effects on a JVM running near its limits.
 */
public class DummyTransaction extends Transaction {

    private final String guid;
    private final Map<String, Object> dummyMap = new DummyMap<>();
    private final Map<String, Object> dummyObjectMap = new DummyMap<>();
    private final Map<String, Map<String, String>> dummyStringMap = new DummyMap<>();

    private final Object lock = new Object();
    private final Insights insights = new DummyInsights();
    private final Logs logs = new DummyLogs();
    private final AgentConfig defaultConfig;
    private final TracerList tracerList = new TracerList(null, new DummySet<>());
    private final TransactionTimer timer = new TransactionTimer(0);
    private final InboundHeaderState inboundHeaderState = new InboundHeaderState(null, null);
    private final SlowQueryListener slowQueryListener = new NopSlowQueryListener();
    private final boolean autoAppNamingEnabled;

    private final TransactionCounts txnCounts;
    private final Set<TransactionActivity> finishedChildren = new DummySet<>();

    private static final MetricAggregator metricAggregator = new AbstractMetricAggregator() {
        @Override
        protected void doRecordResponseTimeMetric(String name, long totalTime, long exclusiveTime, TimeUnit timeUnit) {
        }

        @Override
        protected void doRecordMetric(String name, float value) {
        }

        @Override
        protected void doIncrementCounter(String name, int count) {
        }
    };

    protected DummyTransaction() {
        guid = TransactionGuidFactory.generate16CharGuid();
        defaultConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        autoAppNamingEnabled = defaultConfig.isAutoAppNamingEnabled();
        txnCounts = new TransactionCounts(defaultConfig);
    }

    @Override
    public MetricAggregator getMetricAggregator() {
        return metricAggregator;
    }

    @Override
    public Object getLock() {
        return lock;
    }

    @Override
    public String getGuid() {
        return guid;
    }

    @Override
    public AgentConfig getAgentConfig() {
        return defaultConfig;
    }

    @Override
    public long getWallClockStartTimeMs() {
        return System.currentTimeMillis();
    }

    @Override
    public Map<String, Object> getInternalParameters() {
        return dummyObjectMap;
    }

    @Override
    public Map<String, Map<String, String>> getPrefixedAgentAttributes() {
        return dummyStringMap;
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        return dummyObjectMap;
    }

    @Override
    public Map<String, Object> getAgentAttributes() {
        return dummyObjectMap;
    }

    @Override
    public Map<String, Object> getIntrinsicAttributes() {
        return dummyObjectMap;
    }

    @Override
    public Map<String, Object> getErrorAttributes() {
        return dummyMap;
    }

    @Override
    public Insights getInsightsData() {
        return insights;
    }

    @Override
    public Logs getLogEventData() {
        return logs;
    }

    @Override
    public TransactionTracerConfig getTransactionTracerConfig() {
        return getAgentConfig().getTransactionTracerConfig();
    }

    @Override
    public CrossProcessConfig getCrossProcessConfig() {
        return getAgentConfig().getCrossProcessConfig();
    }

    @Override
    public boolean setTransactionName(TransactionNamePriority namePriority, boolean override, String category, String... parts) {
        return false;
    }

    @Override
    public boolean setTransactionName(com.newrelic.agent.bridge.TransactionNamePriority namePriority, boolean override, String category, String... parts) {
        return false;
    }

    @Override
    public boolean isTransactionNameSet() {
        return false;
    }

    @Override
    public PriorityTransactionName getPriorityTransactionName() {
        return PriorityTransactionName.NONE;
    }

    @Override
    public void freezeTransactionName() {
    }

    @Override
    public boolean conditionalSetPriorityTransactionName(TransactionNamingPolicy policy, String name, String category,
            com.newrelic.agent.bridge.TransactionNamePriority priority) {
        return false;
    }

    @Override
    public boolean setPriorityTransactionName(PriorityTransactionName ptn) {
        return false;
    }

    @Override
    public SlowQueryListener getSlowQueryListener(boolean createIfNotExists) {
        return slowQueryListener;
    }

    @Override
    public TransactionCache getTransactionCache() {
        return DummyTransactionCache.INSTANCE;
    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public boolean isInProgress() {
        return false;
    }

    @Override
    public Dispatcher getDispatcher() {
        return null;
    }

    @Override
    public long getExternalTime() {
        return 0L;
    }

    @Override
    public Tracer getRootTracer() {
        return null;
    }

    @Override
    public List<Tracer> getTracers() {
        return tracerList;
    }

    @Override
    public TransactionActivity getTransactionActivity() {
        return DummyTransactionActivity.INSTANCE;
    }

    @Override
    void activityStarted(TransactionActivity activity) {
    }

    @Override
    public void startTransactionIfBeginning(Tracer tracer) {
    }

    @Override
    public void setDispatcher(Dispatcher dispatcher) {
    }

    @Override
    public TransactionTimer getTransactionTimer() {
        return timer;
    }

    @Override
    public void addTotalCpuTimeForLegacy(long time) {
    }

    @Override
    public void recordFinalGCTime(TransactionStats stats) {
    }

    @Override
    public boolean isTransactionTraceEnabled() {
        return false;
    }

    @Override
    public boolean isAutoAppNamingEnabled() {
        return autoAppNamingEnabled;
    }

    @Override
    public boolean isTransactionNamingEnabled() {
        return false;
    }

    @Override
    public boolean isWebTransaction() {
        return false;
    }

    @Override
    public boolean isSynthetic() {
        return false;
    }

    @Override
    public void provideHeaders(InboundHeaders headers) {
    }

    @Override
    public void provideRawHeaders(InboundHeaders headers) {
    }

    @Override
    public InboundHeaderState getInboundHeaderState() {
        return inboundHeaderState;
    }

    @Override
    public IRPMService getRPMService() {
        return ServiceFactory.getRPMServiceManager().getOrCreateRPMService(getPriorityApplicationName());
    }

    @Override
    public void setNormalizedUri(String normalizedUri) {
    }

    @Override
    public String getNormalizedUri() {
        return null;
    }

    @Override
    public TransactionThrowable getThrowable() {
        return null;
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

    @Override
    public void setThrowable(Throwable throwable, TransactionErrorPriority priority, boolean expected) {
    }

    @Override
    public boolean isIgnore() {
        return true;
    }

    @Override
    public void ignore() {
    }

    @Override
    public void setIgnore(boolean ignore) {
    }

    @Override
    public void ignoreApdex() {
    }

    @Override
    public TransactionCounts getTransactionCounts() {
        return txnCounts;
    }

    @Override
    public boolean shouldGenerateTransactionSegment() {
        return false;
    }

    @Override
    public DatabaseStatementParser getDatabaseStatementParser() {
        return DummyDatabaseStatementParser.INSTANCE;
    }

    @Override
    public BrowserTransactionState getBrowserTransactionState() {
        return null;
    }

    @Override
    public CrossProcessState getCrossProcessState() {
        return DummyCrossProcessState.INSTANCE;
    }

    @Override
    public CrossProcessTransactionState getCrossProcessTransactionState() {
        return DummyCrossProcessState.INSTANCE;
    }

    @Override
    public TransactionState getTransactionState() {
        return DummyTransactionState.INSTANCE;
    }

    @Override
    public void setTransactionState(TransactionState transactionState) {
    }

    @Override
    public void addOutboundResponseHeaders() {
    }

    @Override
    public WebResponse getWebResponse() {
        return DEFAULT_RESPONSE;
    }

    @Override
    public void convertToWebTransaction() {
    }

    @Override
    public void requestInitialized(Request request, Response response) {
    }

    @Override
    public void requestDestroyed() {
    }

    @Override
    public boolean isWebRequestSet() {
        return false;
    }

    @Override
    public boolean isWebResponseSet() {
        return false;
    }

    @Override
    public void setWebRequest(Request request) {
    }

    @Override
    public void setWebResponse(Response response) {
    }

    @Override
    public String getApplicationName() {
        return getPriorityApplicationName().getName();
    }

    @Override
    public PriorityApplicationName getPriorityApplicationName() {
        return PriorityApplicationName.NONE;
    }

    @Override
    public void setApplicationName(ApplicationNamePriority priority, String appName) {
    }

    @Override
    public long getRunningDurationInNanos() {
        return 0L;
    }

    @Override
    public void saveMessageParameters(Map<String, String> parameters) {
    }

    @Override
    public Set<TransactionActivity> getFinishedChildren() {
        return finishedChildren;
    }

    @Override
    public void activityFinished(TransactionActivity activity, Tracer tracer, int opcode) {
    }

    @Override
    public void activityFailedOrIgnored(TransactionActivity activity, int opcode) {
    }

    @Override
    public void noticeTracerException(Throwable throwable, String spanId) {
    }

    @Override
    public String toString() {
        return "DummyTransaction";
    }

    static final class DummyMap<K, V> implements Map<K, V> {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public V get(Object key) {
            return null;
        }

        @Override
        public V put(K key, V value) {
            return null;
        }

        @Override
        public V remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Set<K> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<V> values() {
            return Collections.emptySet();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }
    }

    static final class DummySet<E> implements Set<E> {
        // This object is used to return an iterator and empty arrays. We do not want to put anything in this set.
        private final Set<E> object = new HashSet<>();

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Iterator<E> iterator() {
            return object.iterator();
        }

        @Override
        public Object[] toArray() {
            return object.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return object.toArray(a);
        }

        @Override
        public boolean add(E e) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

    static final class DummyDatabaseStatementParser implements DatabaseStatementParser {
        static final DatabaseStatementParser INSTANCE = new DummyDatabaseStatementParser();

        private static final ParsedDatabaseStatement parsedDatabaseStatement = new ParsedDatabaseStatement(null, null, false);

        @Override
        public ParsedDatabaseStatement getParsedDatabaseStatement(DatabaseVendor databaseVendor, String statement, ResultSetMetaData resultSetMetaData) {
            return parsedDatabaseStatement;
        }
    }

    static final class DummyInsights implements Insights {
        @Override
        public void recordCustomEvent(String eventType, Map<String, ?> attributes) {
        }
    }

    static final class DummyLogs implements Logs {
        @Override
        public void recordLogEvent(Map<LogAttributeKey, ?> attributes) {
        }
    }

    static final class DummyCrossProcessState implements CrossProcessTransactionState {
        public static final CrossProcessTransactionState INSTANCE = new DummyCrossProcessState();

        @Override
        public void processOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
        }

        @Override
        public void processOutboundRequestHeaders(OutboundHeaders outboundHeaders,
                com.newrelic.api.agent.TracedMethod tracedMethod) {
        }

        @Override
        public void processOutboundResponseHeaders(OutboundHeaders outboundHeaders, long contentLength) {
        }

        @Override
        public void processInboundResponseHeaders(InboundHeaders inboundHeaders, TracedMethod tracer, String host,
                String uri, boolean addRollupMetric) {
        }

        @Override
        public String getRequestMetadata() {
            return null;
        }

        @Override
        public void processRequestMetadata(String requestMetadata) {
        }

        @Override
        public String getResponseMetadata() {
            return null;
        }

        @Override
        public void processResponseMetadata(String responseMetadata, URI uri) {
        }

        @Override
        public void writeResponseHeaders() {
        }

        @Override
        public String getTripId() {
            return "";
        }

        @Override
        public int generatePathHash() {
            return 0;
        }

        @Override
        public String getAlternatePathHashes() {
            return "";
        }
    }

    static final class DummyTransactionActivity extends TransactionActivity {
        public static final TransactionActivity INSTANCE = new DummyTransactionActivity();
        public static final Tracer DUMMY_TRACER = new NoOpTracer();

        @Override
        public TransactionStats getTransactionStats() {
            return DummyTransactionStats.INSTANCE;
        }

        @Override
        public List<Tracer> getTracers() {
            return Collections.emptyList();
        }

        @Override
        public long getTotalCpuTime() {
            return 0L;
        }

        @Override
        public void setToIgnore() {
        }

        @Override
        void setOwningTransactionIsIgnored(boolean newState) {
        }

        @Override
        public Tracer tracerStarted(Tracer tracer) {
            return tracer;
        }

        @Override
        public void tracerFinished(Tracer tracer, int opcode) {
        }

        @Override
        public boolean isStarted() {
            return true;
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public void recordCpu() {
        }

        @Override
        public void addTracer(Tracer tracer) {
        }

        @Override
        public boolean checkTracerStart() {
            return false;
        }

        @Override
        public Tracer getLastTracer() {
            return DUMMY_TRACER;
        }

        @Override
        public TracedMethod startFlyweightTracer() {
            return DUMMY_TRACER;
        }

        @Override
        public void finishFlyweightTracer(TracedMethod parent, long startInNanos, long finishInNanos, String className,
                String methodName, String methodDesc, String metricName, String[] rollupMetricNames) {
        }

        @Override
        public void startAsyncActivity(Transaction transaction, int activityId, Tracer parentTracer) {
        }

        @Override
        public Tracer getRootTracer() {
            return DUMMY_TRACER;
        }

        @Override
        public TransactionCache getTransactionCache() {
            return Transaction.getOrCreateDummyTransaction().getTransactionCache();
        }

        @Override
        public Transaction getTransaction() {
            return Transaction.getOrCreateDummyTransaction();
        }

        @Override
        public int hashCode() {
            return 0;
        }

    }

    static final class DummyTransactionStats extends TransactionStats {
        public static final TransactionStats INSTANCE = new DummyTransactionStats();
        static final SimpleStatsEngine stats = new DummySimpleStatsEngine();

        @Override
        public SimpleStatsEngine getUnscopedStats() {
            return stats;
        }

        @Override
        public SimpleStatsEngine getScopedStats() {
            return stats;
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public String toString() {
            return "";
        }
    }

    static final class DummySimpleStatsEngine extends SimpleStatsEngine {
        static final Map<String, StatsBase> statsMap = new DummyMap<>();
        static final DummyStats stat = new DummyStats();
        static final DummyResponseTimeStats responseTimeStat = new DummyResponseTimeStats();
        static final DummyApdexStat apdexStat = new DummyApdexStat();
        static final DummyDataUsageStats dataUsageStats = new DummyDataUsageStats();

        @Override
        public Map<String, StatsBase> getStatsMap() {
            return statsMap;
        }

        @Override
        public Stats getStats(String metricName) {
            return stat;
        }

        @Override
        public ResponseTimeStats getOrCreateResponseTimeStats(String metric) {
            return responseTimeStat;
        }

        @Override
        public void recordEmptyStats(String metricName) {
        }

        @Override
        public ApdexStats getApdexStats(String metricName) {
            return apdexStat;
        }

        @Override
        public DataUsageStats getDataUsageStats(String metricName) {
            return dataUsageStats;
        }

        @Override
        public void mergeStats(SimpleStatsEngine other) {
        }

        @Override
        public void clear() {
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public List<MetricData> getMetricData(Normalizer metricNormalizer, String scope) {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return "";
        }
    }

    static final class DummyStats extends StatsImpl {
        public DummyStats() {
        }

        public DummyStats(int count, float total, float minValue, float maxValue, double sumOfSquares) {
            super();
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return this;
        }

        @Override
        public String toString() {
            return "";
        }

        @Override
        public void recordDataPoint(float value) {
        }

        @Override
        public boolean hasData() {
            return false;
        }

        @Override
        public void reset() {
        }

        @Override
        public float getTotal() {
            return 0;
        }

        @Override
        public float getTotalExclusiveTime() {
            return 0;
        }

        @Override
        public float getMinCallTime() {
            return 0;
        }

        @Override
        public float getMaxCallTime() {
            return 0;
        }

        @Override
        public double getSumOfSquares() {
            return 0;
        }

        @Override
        public void merge(StatsBase statsObj) {
        }

        @Override
        public void incrementCallCount(int value) {
        }

        @Override
        public void incrementCallCount() {
        }

        @Override
        public int getCallCount() {
            return 0;
        }

        @Override
        public void setCallCount(int count) {
        }
    }

    static final class DummyResponseTimeStats extends ResponseTimeStatsImpl {
        DummyResponseTimeStats() {
            super();
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return this;
        }

        @Override
        public void recordResponseTime(long responseTime, TimeUnit timeUnit) {
        }

        @Override
        public void recordResponseTime(long responseTime, long exclusiveTime, TimeUnit timeUnit) {
        }

        @Override
        public void recordResponseTime(int count, long totalTime, long minTime, long maxTime, TimeUnit unit) {
        }

        @Override
        public void recordResponseTimeInNanos(long responseTime) {
        }

        @Override
        public void recordResponseTimeInNanos(long responseTime, long exclusiveTime) {
        }

        @Override
        public boolean hasData() {
            return false;
        }

        @Override
        public void reset() {
        }

        @Override
        public float getTotal() {
            return 0;
        }

        @Override
        public float getTotalExclusiveTime() {
            return 0;
        }

        @Override
        public float getMaxCallTime() {
            return 0;
        }

        @Override
        public float getMinCallTime() {
            return 0;
        }

        @Override
        public double getSumOfSquares() {
            return 0;
        }

        @Override
        public String toString() {
            return "";
        }

        @Override
        public void incrementCallCount(int value) {
        }

        @Override
        public void incrementCallCount() {
        }

        @Override
        public int getCallCount() {
            return 0;
        }

        @Override
        public void setCallCount(int count) {
        }
    }

    static final class DummyApdexStat extends ApdexStatsImpl {
        DummyApdexStat() {
            super();
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return this;
        }

        @Override
        public String toString() {
            return "";
        }

        @Override
        public void recordApdexFrustrated() {
        }

        @Override
        public int getApdexSatisfying() {
            return 0;
        }

        @Override
        public int getApdexTolerating() {
            return 0;
        }

        @Override
        public int getApdexFrustrating() {
            return 0;
        }

        @Override
        public void recordApdexResponseTime(long responseTimeMillis, long apdexTInMillis) {
        }

        @Override
        public boolean hasData() {
            return false;
        }

        @Override
        public void reset() {
        }

        @Override
        public void writeJSONString(Writer writer) throws IOException {
        }

        @Override
        public void merge(StatsBase statsObj) {
        }
    }

    static final class DummyDataUsageStats extends DataUsageStatsImpl {
        DummyDataUsageStats() {
            super();
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return this;
        }

        @Override
        public String toString() {
            return "";
        }

        @Override
        public void recordDataUsage(long bytesSent, long bytesReceived) {
        }

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public long getBytesSent() {
            return 0;
        }

        @Override
        public long getBytesReceived() {
            return 0;
        }

        @Override
        public boolean hasData() {
            return false;
        }

        @Override
        public void reset() {
        }

        @Override
        public void writeJSONString(Writer writer) throws IOException {
        }

        @Override
        public void merge(StatsBase statsObj) {
        }
    }

    static final class DummyTransactionCache extends TransactionCache {
        public static final TransactionCache INSTANCE = new DummyTransactionCache();

        @Override
        public MetricNameFormatWithHost getMetricNameFormatWithHost(Object key) {
            return null;
        }

        @Override
        public void putMetricNameFormatWithHost(Object key, MetricNameFormatWithHost val) {
        }

        @Override
        public URL getURL(Object key) {
            return null;
        }

        @Override
        public void putURL(Object key, URL val) {
        }
    }

    static final class DummyTransactionState implements TransactionState {
        public static final TransactionState INSTANCE = new DummyTransactionState();

        @Override
        public Tracer getTracer(Transaction tx, TracerFactory tracerFactory, ClassMethodSignature sig, Object obj, Object... args) {
            return null;
        }

        @Override
        public Tracer getTracer(Transaction tx, String tracerFactoryName, ClassMethodSignature sig, Object obj, Object... args) {
            return null;
        }

        @Override
        public Tracer getTracer(Transaction tx, Object invocationTarget, ClassMethodSignature sig, String metricName, int flags) {
            return null;
        }

        @Override
        public Tracer getSqlTracer(Transaction tx, Object invocationTarget, ClassMethodSignature sig, String metricName, int flags) {
            return null;
        }

        @Override
        public boolean finish(Transaction tx, Tracer tracer) {
            return false;
        }

        @Override
        public void resume() {
        }

        @Override
        public void suspend() {
        }

        @Override
        public void suspendRootTracer() {
        }

        @Override
        public void complete() {
        }

        @Override
        public Tracer getRootTracer() {
            return null;
        }
    }

}
