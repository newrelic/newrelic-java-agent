/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * Tokens are passed between threads to link asynchronous units of work to the originating {@link Transaction}
 * associated with the token. This results in two or more pieces of work being tied together into a single
 * {@link Transaction}. A token is created by a call to {@link Transaction#getToken()}.
 */
public interface Token {

    /**
     * Links the work being done on the current thread back to the originating {@link Transaction} associated with the
     * token.
     *
     * Linking alone does not start tracing work on the thread. To begin tracing work on methods that should be
     * included in the transaction, you should use the @Trace(async = true) annotation.
     *
     * @return True if the current thread has been linked to the token's transaction. False if the token has been
     * expired or if you are trying to link within the @Trace(dispatcher = true) transaction where the token was created.
     * @since 3.37.0
     */
    boolean link();

    /**
     * Invalidates the token, making it unable to link work together in a {@link Transaction}. When a token is expired
     * it can no longer be used.
     *
     * If a {@link Token} is not explicitly expired it will be timed out according to the <code>token_timeout</code>
     * value which is user configurable in the yaml file or by a Java system property.
     *
     * When all tokens are expired the transaction will be allowed to finish.
     *
     * @return True if the token was found and expired.
     * @since 3.37.0
     */
    boolean expire();

    /**
     * Links the work being done on the current thread back to the originating {@link Transaction} associated with the
     * token and expires the token. If linking fails for any reason the token will still be expired. This provides an
     * alternative to separately calling {@link #link()} and {@link #expire()}.
     *
     * @return True if the current thread has been linked to the token's transaction and the token has been expired,
     * false otherwise. Note: The token will be expired (if found) even if the result of this call is "false".
     * @since 3.37.0
     */
    boolean linkAndExpire();

    /**
     * Checks if a token is valid and can be used to link work to the originating {@link Transaction} associated with
     * the token.
     *
     * @return True if the token is active and can be used, false if the token has been expired.
     * @since 3.37.0
     */
    boolean isActive();

}
