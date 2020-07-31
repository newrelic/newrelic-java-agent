/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.datastax.driver.core;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.nr.agent.instrumentation.cassandra.CassandraUtils;

import java.net.InetSocketAddress;
import java.util.logging.Level;

public class NewRelicChainedResultSetFuture extends ChainedResultSetFuture {

    private final String keyspace;
    private final Segment segment;
    private final ResultSetFuture source;
    private final Statement statement;

    public NewRelicChainedResultSetFuture(String keyspace, Segment segment, ResultSetFuture source,
                                          Statement statement) {
        this.keyspace = keyspace;
        this.segment = segment;
        this.source = source;
        this.statement = statement;
    }

    @Override
    protected boolean set(ResultSet value) {
        setDatastoreMetrics(value);
        segment.end();
        return super.set(value);
    }

    @Override
    protected boolean setException(Throwable throwable) {
        setDatastoreMetrics(null);
        segment.end();
        return super.setException(throwable);
    }

    private void setDatastoreMetrics(ResultSet value) {
        InetSocketAddress queriedHost = null;
        if (value != null) {
            ExecutionInfo executionInfo = value.getExecutionInfo();
            if (executionInfo != null) {
                queriedHost = executionInfo.getQueriedHost().getSocketAddress();
            }
        }

        String query = null;
        String keyspace = this.keyspace;
        if (statement.getKeyspace() != null) {
            keyspace = statement.getKeyspace();
        }
        String host = queriedHost != null ? queriedHost.getHostName() : null;
        Integer port = queriedHost != null ? queriedHost.getPort() : null;
        if (RegularStatement.class.isAssignableFrom(statement.getClass())) {
            query = RegularStatement.class.cast(statement).getQueryString();
        } else if (BoundStatement.class.isAssignableFrom(statement.getClass())) {
            query = BoundStatement.class.cast(statement).preparedStatement().getQueryString();
        } else if (BatchStatement.class.isAssignableFrom(statement.getClass())) {
            // Don't bother parsing datastax batch statements
            CassandraUtils.metrics(null, null, "BATCH", host, port, keyspace, AgentBridge.getAgent().getTransaction(),
                    segment);
        } else {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "unsupported statement type: {0} class {1}",
                    statement, statement.getClass());
        }

        if (null != query) {
            CassandraUtils.metrics(query, host, port, keyspace, AgentBridge.getAgent().getTransaction(),
                    segment);
        }
    }
}
