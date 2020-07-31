/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.newrelic.agent.model.TimeoutCause;
import com.newrelic.agent.util.TimeConversion;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * This implementation relies on a guava cache, which is like a map. There is no set implementation which is why the
 * map stores the token reference as both the key and value.
 *
 * Note, changes to token behavior here should be made consistent with the old async api in AsyncTransactionService.
 */
public class TimedTokenSet implements TimedSet<TokenImpl> {

    private final AtomicInteger timedOutTokens;
    private final Cache<TokenImpl, TokenImpl> activeTokens;

    public TimedTokenSet(int timeOut, TimeUnit unit, final ExpirationService expirationService) {
        timedOutTokens = new AtomicInteger(0);

        // async timeout is given in seconds, but passing in 0 causes strange behavior, especially in tests, because
        // onRemoval happens immediately after put, so we can hit onRemoval logic before getToken() even finishes
        long timeOutMilli = TimeConversion.convertToMilliWithLowerBound(timeOut, unit, 250L);

        activeTokens = CacheBuilder.newBuilder()
                .concurrencyLevel(8)
                .expireAfterAccess(timeOutMilli, TimeUnit.MILLISECONDS)
                .removalListener(new RemovalListener<TokenImpl, TokenImpl>() {
                    @Override
                    public void onRemoval(RemovalNotification<TokenImpl, TokenImpl> notification) {
                        RemovalCause cause = notification.getCause();
                        final TokenImpl token = notification.getKey();
                        Transaction tx = token.getTransaction().getTransactionIfExists();

                        try {
                            if (cause == RemovalCause.EXPIRED) { // time out case
                                Agent.LOG.log(Level.FINEST, "Timing out token {0} on transaction {1}", token, tx);
                                timedOutTokens.incrementAndGet();
                                token.setTruncated();

                                if (tx != null) {
                                    tx.setTimeoutCause(TimeoutCause.TOKEN);
                                }
                            } else if (cause == RemovalCause.EXPLICIT) { // remove and removeAll case
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
                            expirationService.expireToken(new Runnable() {
                                @Override
                                public void run() {
                                    // In the case of a token being expired we *must* spin off the work on to a
                                    // second thread in order to prevent a possible deadlock between the expire code
                                    // and other tx usages.
                                    token.markExpired();
                                }
                            });
                        }
                    }
                }).build();
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
        return activeTokens.asMap().remove(token) != null;
    }

    /**
     * Removes any and all entries from the set, the removal cause for each should be RemovalCause.EXPLICIT.
     */
    @Override
    public void removeAll() {
        activeTokens.invalidateAll();
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
        activeTokens.getIfPresent(token);
    }

}
