/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.sql;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "java.sql.Connection", type = MatchType.Interface)
public abstract class Connection_Weaved {

    public CallableStatement prepareCall(String sql) throws SQLException {
        CallableStatement callableStatement = Weaver.callOriginal();
        return callableStatement;
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        CallableStatement callableStatement = Weaver.callOriginal();
        return callableStatement;
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        CallableStatement callableStatement = Weaver.callOriginal();
        return callableStatement;
    }

}
