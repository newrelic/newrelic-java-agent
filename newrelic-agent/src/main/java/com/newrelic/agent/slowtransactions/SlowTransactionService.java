package com.newrelic.agent.slowtransactions;

import com.newrelic.agent.ExtendedTransactionListener;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.NewRelic;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SlowTransactionService extends AbstractService implements ExtendedTransactionListener, HarvestListener {

    private final ConcurrentHashMap<String, Transaction> openTransactions = new ConcurrentHashMap<>();
    private final Set<String> previouslyReportedTransactions = new HashSet<>();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    public SlowTransactionService() {
        super(SlowTransactionService.class.getSimpleName());
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
        previouslyReportedTransactions.remove(transaction.getGuid());
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        if (getLogger().isLoggable(Level.FINEST)) {
            getLogger().finest("Transaction finished with guid " + transactionData.getGuid());
        }
        openTransactions.remove(transactionData.getGuid());
        previouslyReportedTransactions.remove(transactionData.getGuid());
    }

    @Override
    protected void doStart() throws Exception {
        ServiceFactory.getTransactionService().addTransactionListener(this);
        ServiceFactory.getHarvestService().addHarvestListener(this);
    }

    @Override
    protected void doStop() throws Exception {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
        ServiceFactory.getHarvestService().removeHarvestListener(this);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        run();
    }

    @Override
    public void afterHarvest(String appName) {
    }

    private void run() {
        if (getLogger().isLoggable(Level.FINE)) {
            getLogger().fine("Identifying slow threads. Open transactions: " + openTransactions.size());
        }
        Transaction slowestOpen = null;
        long slowestOpenMs = 0;

        // Identify the slowest open transaction we haven't yet reported
        for (Transaction transaction : openTransactions.values()) {
            // Ignore previously reported transactions
            if (previouslyReportedTransactions.contains(transaction.getGuid())) {
                continue;
            }

            long openMs = System.currentTimeMillis() - transaction.getWallClockStartTimeMs();
            if (openMs > slowestOpenMs) {
                slowestOpen = transaction;
                slowestOpenMs = openMs;
            }
        }

        if (slowestOpen == null) {
            getLogger().info("No new slow transactions identified.");
            return;
        }

        // We've identified the slowest open transaction over the threshold.
        // Extract some data about its state and emit as event.
        String guid = slowestOpen.getGuid();
        Map<String, Object> attributes = new HashMap<>();

        // Attributes
        // Write attributes first so hardcoded attributes are prioritized
        attributes.putAll(slowestOpen.getUserAttributes());
        attributes.putAll(slowestOpen.getErrorAttributes());
        attributes.putAll(slowestOpen.getAgentAttributes());
        attributes.putAll(slowestOpen.getIntrinsicAttributes());

        // General
        attributes.put("guid", guid);
        attributes.put("name", slowestOpen.getPriorityTransactionName().getName());
        attributes.put("transactionType", slowestOpen.getPriorityTransactionName().getCategory());
        attributes.put("timestamp", slowestOpen.getWallClockStartTimeMs());
        attributes.put("elapsed_ms", slowestOpenMs);

        // Initiating thread info
        long initiatingThreadId = slowestOpen.getInitiatingThreadId();
        attributes.put("thread.id", initiatingThreadId);
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(initiatingThreadId, 20);
        if (threadInfo != null) {
            attributes.put("thread.name", threadInfo.getThreadName());
            attributes.put("thread.state", threadInfo.getThreadState().name());
            attributes.put("code.stacktrace", stackTraceString(threadInfo.getStackTrace()));
        }

        if (getLogger().isLoggable(Level.FINE)) {
            getLogger().fine("Slowest open transaction has guid " + guid + " has been open for " + slowestOpenMs + "ms, attributes: " + attributes);
        }
        // TODO: emit event such that stack trace isn't truncated so early
        NewRelic.getAgent().getInsights().recordCustomEvent("SlowTransaction", attributes);
        previouslyReportedTransactions.add(guid);
    }

    private static String stackTraceString(StackTraceElement[] stackTrace) {
        if (stackTrace.length == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(stackTrace[0]);
        for (int i = 1; i < stackTrace.length; i++) {
            stringBuilder.append("\tat ").append(stackTrace[i]);
        }
        return stringBuilder.toString();
    }
}
