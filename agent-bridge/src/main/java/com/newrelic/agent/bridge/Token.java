/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

/**
 * @Deprecated Do not use. Use {@link com.newrelic.api.agent.Token} instead.
 *
 * Used to join two or more pieces of work together into one transaction.
 */
@Deprecated
public interface Token extends com.newrelic.api.agent.Token {

    /**
     * Once a token is taken from a transaction, the transaction will remain open until the token is expired. This means
     * a token can be used to join multiple units of work together.
     * 
     * @return True if the token was found and expired.
     */
    boolean expire();

    /**
     * Links the transaction associated with the token to the current thread. Linking does not start tracing work on the
     * thread. @Trace must be called to begin tracing work to be put into the transaction.
     * 
     * @return Returns true if the current thread has been linked to the token's transaction.
     */
    boolean link();

    /**
     * Links the transaction associated with the token to the current thread and expires the token. If linking fails for
     * any reason the token will still be expired. Linking does not start tracing work on the thread. @Trace must be
     * called to begin tracing work to be put into the transaction.
     *
     * @return Returns true if the current thread has been linked to the token's transaction and the token has been
     * expired, false otherwise. Note: The token will be expired (if found) even if the result of this call is "false".
     */
    boolean linkAndExpire();

    /**
     * Returns true if the token can be used to link work. Returns false if the token has already been expired meaning
     * it can no longer be used to link work.
     * 
     * @return True if the token is active and can be used, false if the token has been expired.
     */
    boolean isActive();

    /**
     * Returns the transaction associated with the token.
     *
     * @return Returns the transaction associated with the token or null if there is not one.
     */
    com.newrelic.api.agent.Transaction getTransaction();

}
