/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.async;

import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.CacheRemovalListener;
import com.newrelic.agent.bridge.CleanableMap;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.util.TimeConversion;
import com.newrelic.api.agent.Token;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Stores pending async transactions. Responsible for timing out old async keys.
 *
 * Most of this logic used to be in the Transaction. However, it was extracted to take advantage of the harvest thread
 * to timeout async transaction keys. Previously the timeout was occurring during extractIfPresent and putIfAbsent. This
 * caused a deadlock between the transaction lock and the pendingActivities lock.
 *
 * This class supports the legacy register and start API. The register and start should no longer be used. Instead
 * please use getToken and link.
 *
 * This class checked old tokens to see if they should be timed out.
 */
public class AsyncTransactionService extends AbstractService implements HarvestListener {

    public AsyncTransactionService() {
        super(AsyncTransactionService.class.getSimpleName());
        PENDING_ACTIVITIES.clear(); // Clean up pending activities for tests
    }

    /*
     * Async registrations JVM-wide are recorded here. References to this object must be locked by locking the object
     * itself. This lock is "inner" to the instance lock, i.e. caller should always hold the instance lock before
     * locking this collection.
     */
    private static final CleanableMap<Object, Token> PENDING_ACTIVITIES = makeCache();

    // Return a CleanableMap that maps asynchronous context keys to their transactions.
    // The implementation class is threadsafe.
    private static CleanableMap<Object, Token> makeCache() {
        // default set to 3 minutes (180), must match the behavior in TimedTokenSet: expireAfterWrite with same timeout
        long timeoutSec = ServiceFactory.getConfigService().getDefaultAgentConfig().getTokenTimeoutInSec();
        long timeOutMilli = TimeConversion.convertToMilliWithLowerBound(timeoutSec, TimeUnit.SECONDS, 250L);

        return AgentBridge.collectionFactory.createCacheWithWriteExpirationAndRemovalListener(
                timeOutMilli, TimeUnit.MILLISECONDS, 8,
                (Object key, Token transaction, CacheRemovalListener.RemovalReason cause) -> {
                    if (cause == CacheRemovalListener.RemovalReason.EXPLICIT) {
                        Agent.LOG.log(Level.FINEST, "{2}: Key {0} with transaction {1} removed from cache.",
                                key, transaction, cause);
                    } else {
                        // timeout, size
                        Agent.LOG.log(Level.FINE,
                                "{2}: The registered async activity with async context {0} has timed out for transaction {1} and been removed from the cache.",
                                key, transaction, cause);
                    }
                });
    }

    protected void cleanUpPendingTransactions() {
        PENDING_ACTIVITIES.cleanUp();
        Agent.LOG.log(Level.FINER, "Cleaning up the pending activities cache.");
    }

    /*
     * Lock the system-wide pending activities collection and add the Transaction argument by the key. Return true if
     * the key is not present and the Transaction is added, false if the key is already present so the Transaction
     * cannot be added. Note: for deadlock avoidance, the lock on this Transaction object is "outer" to the
     * allPendingActivities lock. In other words, the caller should hold the instance lock when calling here.
     * Additionally, the transaction lock can not be taken within this method.
     */

    /**
     * This method should only be called from within a transaction object.
     *
     * @param key The key to register
     * @param tx The transaction associated with the key.
     */
    public boolean putIfAbsent(Object key, Token tx) {
        return PENDING_ACTIVITIES.putIfAbsent(key, tx) == null;
    }

    /*
     * Lock the systemwide pending activities collection and extract the Transaction referenced by the key. Return null
     * if the key is not present. Note: for deadlock avoidance, the lock on this Transaction object is "outer" to the
     * allPendingActivities lock. In other words, the caller should hold the instance lock when calling here.
     * Additionally, the transaction lock can not be taken within this method.
     */

    /**
     * This method should only be called from within a transaction object.
     *
     * @param key The key representing some async work.
     * @return The transaction associated with the key.
     */
    public Token extractIfPresent(Object key) {
        return PENDING_ACTIVITIES.remove(key);
    }

    /*
     * We need a thread timeout the async keys. The expired async keys can not be timed out in the putIfAbsent or
     * extractIfPresent methods because then a deadlock can potentially occur between the transaction lock and the
     * pending activities lock.
     *
     * This does result in a slight delay to the expiration of keys. However, we are okay with this for the time being.
     *
     * @see com.newrelic.agent.HarvestListener#beforeHarvest(java.lang.String, com.newrelic.agent.stats.StatsEngine)
     */
    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        cleanUpPendingTransactions();
    }

    /*
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.Token} instead.
     */
    @Deprecated
    public boolean registerAsyncActivity(Object context) {
        Transaction tx = Transaction.getTransaction(false);
        if (tx != null && context != null) {
            Token token = tx.getToken();
            if (token != null) {
                if (!putIfAbsent(context, token)) {
                    token.expire();
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.Token} instead.
     */
    @Deprecated
    public boolean startAsyncActivity(Object context) {
        Token token = extractIfPresent(context);
        if (token != null) {
            // to keep the same behavior, only link if there is already a transaction present
            if (TransactionActivity.get() != null && TransactionActivity.get().isStarted()) {
                token.link();
            }
            token.expire();
            return true;
        }
        return false;
    }

    public boolean ignoreIfUnstartedAsyncContext(Object asyncContext) {
        Token token = extractIfPresent(asyncContext);
        if (token == null) {
            return false;
        } else {
            return token.expire();
        }
    }

    // only call in tests - size of the PENDING_ACTIVITIES cache
    protected int cacheSizeForTesting() {
        return PENDING_ACTIVITIES.size();
    }

    @Override
    public void afterHarvest(String appName) {
        // do nothing
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected void doStart() throws Exception {
        ServiceFactory.getHarvestService().addHarvestListener(this);
    }

    @Override
    protected void doStop() throws Exception {
        ServiceFactory.getHarvestService().removeHarvestListener(this);
    }

}
