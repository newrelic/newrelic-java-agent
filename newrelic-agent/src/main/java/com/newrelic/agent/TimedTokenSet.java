/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.CacheRemovalListener;
import com.newrelic.agent.bridge.CleanableMap;
import com.newrelic.agent.model.TimeoutCause;
import com.newrelic.agent.util.TimeConversion;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * This implementation relies on a caffeine cache, which is like a map ( it is built on top of ConcurrentHashMap). There is no set implementation which is why the
 * map stores the token reference as both the key and value.
 *
 * Note, changes to token behavior here should be made consistent with the old async api in AsyncTransactionService.
 */
public class TimedTokenSet implements TimedSet<TokenImpl> {

    private final AtomicInteger timedOutTokens;
    private final CleanableMap<TokenImpl, TokenImpl> activeTokens;

    public TimedTokenSet(int timeOut, TimeUnit unit, final ExpirationService expirationService) {
        timedOutTokens = new AtomicInteger(0);

        // async timeout is given in seconds, but passing in 0 causes strange behavior, especially in tests, because
        // onRemoval happens immediately after put, so we can hit onRemoval logic before getToken() even finishes
        long timeOutMilli = TimeConversion.convertToMilliWithLowerBound(timeOut, unit, 250L);
        activeTokens = AgentBridge.collectionFactory.createCacheWithAccessExpirationAndRemovalListener(
                timeOutMilli, TimeUnit.MILLISECONDS, 8,
                (TokenImpl token, TokenImpl value, CacheRemovalListener.RemovalReason cause) -> {
                    Transaction tx = token.getTransaction().getTransactionIfExists();

                    try {
                        if (cause == CacheRemovalListener.RemovalReason.EXPIRED) { // time out case
                            Agent.LOG.log(Level.FINEST, "Timing out token {0} on transaction {1}", token, tx);
                            timedOutTokens.incrementAndGet();
                            token.setTruncated();

                            if (tx != null) {
                                tx.setTimeoutCause(TimeoutCause.TOKEN);
                            }
                        } else if (cause == CacheRemovalListener.RemovalReason.EXPLICIT) { // remove and removeAll case
                            Agent.LOG.log(Level.FINEST, "Expiring token {0} on transaction {1}", token, tx);
                        } else { // should never happen
                            Agent.LOG.log(Level.FINEST, "Token {0} on transaction {1} removed due to cause {2}", token, tx, cause);
                        }
                    } catch (Exception e) {
                        Agent.LOG.log(Level.FINEST, "Token {0} on transaction {1} threw exception: {2}", token, tx, e);
                    } finally {
                        // The expire all tokens code path doesn't iterate over, and call expire on, all the tokens
                        // because that would make it look like the user explicitly did it. However, the on removal
                        // cause for expiring one is the same as for expiring all (EXPLICIT). So markExpire needs to be
                        // called in either case, since it doesn't hurt to null out the tracer again, and it still needs
                        // to happen in the expire all case.
                        //
                        // In the case of a token being expired we *must* spin off the work on to a
                        // second thread in order to prevent a possible deadlock between the expire code
                        // and other tx usages.
                        expirationService.expireToken(token::markExpired);
                    }
                });
    }

    /**
     * The number of entries in the cache that were removed due to timing out.
     */
    @Override
    public int timedOutCount() {
        return timedOutTokens.get();
    }

    /**
     * Removes one entry from the set, the removal cause should be RemovalCause.EXPLICIT.
     */
    @Override
    public boolean remove(TokenImpl token) {
        return activeTokens.remove(token) != null;
    }

    /**
     * Removes any and all entries from the set, the removal cause for each should be RemovalCause.EXPLICIT.
     */
    @Override
    public void removeAll() {
        activeTokens.clear();
    }

    @Override
    public void put(TokenImpl token) {
        activeTokens.put(token, token);
    }

    @Override
    public void cleanUp() {
        activeTokens.cleanUp();
    }

    @Override
    public void refresh(TokenImpl token) {
        activeTokens.get(token);
    }

}
