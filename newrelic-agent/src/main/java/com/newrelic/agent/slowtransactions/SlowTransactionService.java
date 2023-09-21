package com.newrelic.agent.slowtransactions;

import com.newrelic.agent.ExtendedTransactionListener;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.StackTraces;
import com.newrelic.api.agent.NewRelic;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SlowTransactionService extends AbstractService implements ExtendedTransactionListener {

    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, Transaction> openTransactions = new ConcurrentHashMap<>();
    private final Set<String> previouslyReportedTransactions = new HashSet<>();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    public SlowTransactionService() {
        super(SlowTransactionService.class.getSimpleName());
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("Slow Transactions Thread", true));
    }

    @Override
    public void dispatcherTransactionStarted(Transaction transaction) {
        getLogger().info("Transaction started with id " + transaction.getGuid());
        openTransactions.put(transaction.getGuid(), transaction);
    }

    @Override
    public void dispatcherTransactionCancelled(Transaction transaction) {
        getLogger().info("Transaction cancelled with guid " + transaction.getGuid());
        openTransactions.remove(transaction.getGuid());
    }

    @Override
    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        getLogger().info("Transaction finished with guid " + transactionData.getGuid());
        openTransactions.remove(transactionData.getGuid());
    }

    @Override
    protected void doStart() throws Exception {
        // NewRelic.getAgent().getInsights().recordCustomEvent();
        ServiceFactory.getTransactionService().addTransactionListener(this);
        scheduler.scheduleAtFixedRate(this::run, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    protected void doStop() throws Exception {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private void run() {
        getLogger().info("Identifying slow threads. Open transactions: " + openTransactions.size());
        long slowThresholdMs = 1000; // Only consider transcations open longer than this
        Transaction slowestOpen = null;
        long slowestOpenMs = 0;

        // Identify the slowest open transaction we haven't yet reported
        for (Transaction transaction : openTransactions.values()) {
            // Ignore previously reported transactions
            if (previouslyReportedTransactions.contains(transaction.getGuid())) {
                continue;
            }

            long openMs = System.currentTimeMillis() - transaction.getWallClockStartTimeMs();
            if (openMs > slowThresholdMs && openMs > slowestOpenMs) {
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
        // General
        attributes.put("transaction.guid", guid);
        attributes.put("transaction.open_ms", slowestOpenMs);

        // Transaction attributes
        slowestOpen.getIntrinsicAttributes().forEach((key, value) -> attributes.put("intrinsic." + key, value));
        slowestOpen.getAgentAttributes().forEach((key, value) -> attributes.put("agent." + key, value));
        slowestOpen.getUserAttributes().forEach((key, value) -> attributes.put("user." + key, value));
        slowestOpen.getErrorAttributes().forEach((key, value) -> attributes.put("error." + key, value));

        // Initiating thread info
        long initiatingThreadId = slowestOpen.getInitiatingThreadId();
        attributes.put("initiating_thread.id", initiatingThreadId);
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(initiatingThreadId, 20);
        if (threadInfo != null) {
            attributes.put("initiating_thread.name", threadInfo.getThreadName());
            attributes.put("initiating_thread.stacktrace", StackTraces.stackTracesToStrings(threadInfo.getStackTrace()));
        }

        getLogger().info("Transaction with guid " + guid + " has exceeded slow transaction threshold of " + slowThresholdMs + ", attributes: " + attributes);
        NewRelic.getAgent().getInsights().recordCustomEvent("SlowTransaction", attributes);
        // TODO: remove old entries after a while to avoid unbounded memory usage
        previouslyReportedTransactions.add(guid);
    }
}
