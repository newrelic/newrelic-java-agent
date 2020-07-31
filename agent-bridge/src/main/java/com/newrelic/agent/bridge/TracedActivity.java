/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.api.agent.Segment;

/**
 * @deprecated Do not use. Use {@link Segment} instead.
 * Note: TracedActivity has been exposed on the public api as {@link Segment}. Consider using
 * {@link Segment} instead.
 *
 * A timed activity for a transaction. This activity will appear as a transaction trace segment, but will not have any
 * children and may be reported as asynchronous. The exclusive time of this activity is the time between the calls to
 * {@link Transaction#createAndStartTracedActivity()} and ({@link #finish()} or {@link #finish(Throwable t)}).
 */
@Deprecated
public interface TracedActivity extends Segment {
    /**
     * Returns the underlying {@link TracedMethod} of this activity. Normal api calls (e.g. Datastore, external) can be
     * applied to the returned traced method as long as the activity is not finished.
     */
    TracedMethod getTracedMethod();

    /**
     * This method has been deprecated as it does not do what it was originally intended to do. Functionally
     * it is a NoOp method.
     */
    @Deprecated
    void setAsyncThreadName(String threadName);

    /**
     * Do not report this activity in its parent transaction. Has no effect if the activity is finished.
     */
    void ignoreIfUnfinished();

    /**
     * Stop timing the activity. Only the first call to this method will have an effect.
     */
    void finish();

    /**
     * Stop timing the activity. Only the first call to this method will have an effect.
     *
     * @param t an error to pass to the agent. Note that this error is not automatically reported to APM.
     */
    void finish(Throwable t);
}
