/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.database.ExplainPlanExecutor;

public interface SqlTracerExplainInfo {

    Object getSql();

    /**
     * Verify if this SqlTracer has an explain plan associated with it.
     *
     * @return true if this tracer has explain plan information stored on it, false otherwise
     */
    boolean hasExplainPlan();

    void setExplainPlan(Object... explainPlan);

    /**
     * Return an executor class that can run explain plans for the given (Prepared)Statement.
     *
     * @return explain plan executor for this statement
     */
    ExplainPlanExecutor getExplainPlanExecutor();

}
