/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.sql;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

/**
 * This interface match is here to help us catch any JDBC drivers that we don't have instrumentation for. This
 * class should get weaved for every single JDBC driver since all are required to implement the Driver interface.
 * 
 * Additionally, when the class is weaved we automatically send up a Supportability metric which we can use to do
 * a prefix search for any potential drivers that we missed:
 * 
 * ^Supportability/WeaveInstrumentation/WeaveClass/com.newrelic.instrumentation.jdbc-driver/*
 * 
 * Anything after the "*" will be the Driver class that we matched and we can compare this to a list of known
 * drivers to keep ahead of any missing instrumentation.
 */
@Weave(type = MatchType.Interface)
public class Driver {

}
