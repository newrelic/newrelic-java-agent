package com.newrelic.agent.service.slowtransactions;

import com.newrelic.agent.ExtendedTransactionListener;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.SlowTransactionsConfig;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.InsightsService;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.util.StackTraces;
import com.newrelic.api.agent.NewRelic;

import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SlowTransactionService extends AbstractService implements ExtendedTransactionListener, HarvestListener {

    private final ConcurrentHashMap<String, Transaction> openTransactions = new ConcurrentHashMap<>();
    private final ThreadMXBean threadMXBean;

    private final boolean isEnabled;
    private final long thresholdMillis;
    private final int maxStackTraceLines;
    private final boolean evalCompletedTransactions;

    @Nullable
    private InsightsService insightsService;

    public SlowTransactionService(AgentConfig agentConfig) {
        this(agentConfig, ManagementFactory.getThreadMXBean());
    }

    // Visible for testing
    SlowTransactionService(AgentConfig agentConfig, ThreadMXBean threadMXBean) {
        super(SlowTransactionService.class.getSimpleName());
        SlowTransactionsConfig slowTransactionsConfig = agentConfig.getSlowTransactionsConfig();
        this.isEnabled = slowTransactionsConfig.isEnabled();
        this.thresholdMillis = slowTransactionsConfig.getThresholdMillis();
        this.maxStackTraceLines = agentConfig.getMaxStackTraceLines();
        this.threadMXBean = threadMXBean;
        this.evalCompletedTransactions = slowTransactionsConfig.evaluateCompletedTransactions();

        NewRelic.getAgent().getMetricAggregator().incrementCounter(
                agentConfig.getSlowTransactionsConfig().isEnabled() ?
                        MetricNames.SUPPORTABILITY_SLOW_TXN_DETECTION_ENABLED : MetricNames.SUPPORTABILITY_SLOW_TXN_DETECTION_DISABLED);
    }

    @Override
    protected void doStart() throws Exception {
        // Short circuit if disabled
        if (!isEnabled) {
            return;
        }
        ServiceFactory.getTransactionService().addTransactionListener(this);
        ServiceFactory.getHarvestService().addHarvestListener(this);
        insightsService = ServiceFactory.getServiceManager().getInsights();
    }

    @Override
    protected void doStop() throws Exception {
        // Short circuit if disabled
        if (!isEnabled) {
            return;
        }
        ServiceFactory.getTransactionService().removeTransactionListener(this);
        ServiceFactory.getHarvestService().removeHarvestListener(this);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void dispatcherTransactionStarted(Transaction transaction) {
        if (getLogger().isLoggable(Level.FINEST)) {
            getLogger().finest("Transaction started with id " + transaction.getGuid());
        }
        openTransactions.put(transaction.getGuid(), transaction);
    }

    @Override
    public void dispatcherTransactionCancelled(Transaction transaction) {
        if (getLogger().isLoggable(Level.FINEST)) {
            getLogger().finest("Transaction cancelled with guid " + transaction.getGuid());
        }
        openTransactions.remove(transaction.getGuid());
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        if (getLogger().isLoggable(Level.FINEST)) {
            getLogger().finest("Transaction finished with guid " + transactionData.getGuid());
        }
        Transaction txn = openTransactions.remove(transactionData.getGuid());

        // txn will be null if it's been reported as part of the harvest cycle.
        if (txn != null && evalCompletedTransactions) {
            long txnExecutionTimeInMs = System.currentTimeMillis() - txn.getWallClockStartTimeMs();
            if (txnExecutionTimeInMs > this.thresholdMillis) {
                reportSlowTransaction(txn, txnExecutionTimeInMs, true);
            }
        }
    }

    // Visible for testing
    Map<String, Transaction> getOpenTransactions() {
        return Collections.unmodifiableMap(openTransactions);
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        run();
    }

    @Override
    public void afterHarvest(String appName) {
    }

    // Visible for testing
    void run() {
        if (getLogger().isLoggable(Level.FINE)) {
            getLogger().fine("Identifying slow threads. Open transactions: " + openTransactions.size());
        }
        Transaction slowestOpen = null;
        long slowestOpenMillis = thresholdMillis;

        // Identify the slowest open transaction we haven't yet reported
        for (Transaction transaction : openTransactions.values()) {
            long openMs = System.currentTimeMillis() - transaction.getWallClockStartTimeMs();
            if (openMs > slowestOpenMillis) {
                slowestOpen = transaction;
                slowestOpenMillis = openMs;
            }
        }

        if (slowestOpen == null) {
            getLogger().fine("No new slow transactions identified.");
            return;
        }

        // Construct and record SlowTransaction event
        reportSlowTransaction(slowestOpen, slowestOpenMillis, false);

        // Remove from openTransactions to ensure we don't report the same Transaction
        // multiple times
        openTransactions.remove(slowestOpen.getGuid());
    }

    // Visible for testing
    Map<String, Object> extractMetadata(Transaction transaction, long openMillis) {
        Map<String, Object> attributes = new HashMap<>();

        // Attributes
        // Write attributes first so hardcoded attributes are prioritized
        attributes.putAll(transaction.getUserAttributes());
        attributes.putAll(transaction.getErrorAttributes());
        attributes.putAll(transaction.getAgentAttributes());
        attributes.putAll(transaction.getIntrinsicAttributes());

        // General
        attributes.put("guid", transaction.getGuid());
        attributes.put("name", transaction.getPriorityTransactionName().getName());
        attributes.put("transactionType", transaction.getPriorityTransactionName().getCategory());
        attributes.put("timestamp", transaction.getWallClockStartTimeMs());
        attributes.put("elapsed_ms", openMillis);

        // Initiating thread info
        long initiatingThreadId = transaction.getInitiatingThreadId();
        attributes.put("thread.id", initiatingThreadId);
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(initiatingThreadId, maxStackTraceLines);
        if (threadInfo != null) {
            attributes.put("thread.name", threadInfo.getThreadName());
            attributes.put("thread.state", threadInfo.getThreadState().name());
            List<StackTraceElement> scrubbedStackTraceElements = StackTraces.scrubAndTruncate(threadInfo.getStackTrace());
            attributes.put("code.stacktrace", stackTraceString(scrubbedStackTraceElements));
        }

        return attributes;
    }

    private void reportSlowTransaction(Transaction slowTxn, long txnExecutionTimeInMs, boolean isCompleted) {
        Map<String, Object> attributes = extractMetadata(slowTxn, txnExecutionTimeInMs);
        if (getLogger().isLoggable(Level.FINE)) {
            getLogger().fine("Reporting " + (isCompleted ? "completed" : "in progress") + " slow transaction with guid "
                    + slowTxn.getGuid() + " with execution time of " + txnExecutionTimeInMs + "ms, attributes: " + attributes);
        }
        if (insightsService != null) {
            insightsService.storeEvent(
                    ServiceFactory.getRPMService().getApplicationName(),
                    new CustomInsightsEvent(
                            "SlowTransaction",
                            System.currentTimeMillis(),
                            attributes,
                            DistributedTraceServiceImpl.nextTruncatedFloat()));
        }
    }

    private static String stackTraceString(List<StackTraceElement> stackTrace) {
        if (stackTrace.isEmpty()) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(stackTrace.get(0)).append("\n");
        for (int i = 1; i < stackTrace.size(); i++) {
            stringBuilder.append("\tat ").append(stackTrace.get(i)).append("\n");
        }
        return stringBuilder.toString();
    }
}
