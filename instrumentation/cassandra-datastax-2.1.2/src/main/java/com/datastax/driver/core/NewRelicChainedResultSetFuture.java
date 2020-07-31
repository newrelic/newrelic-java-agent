/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.datastax.driver.core;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import com.datastax.driver.core.exceptions.DriverException;
import com.datastax.driver.core.exceptions.DriverInternalError;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.Uninterruptibles;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.nr.agent.instrumentation.cassandra.CassandraUtils;

public class NewRelicChainedResultSetFuture extends AbstractFuture<ResultSet> implements ResultSetFuture {
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
        Futures.addCallback(source, new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet result) {
                NewRelicChainedResultSetFuture.this.set(result);
            }

            @Override
            public void onFailure(Throwable t) {
                NewRelicChainedResultSetFuture.this.setException(t);
            }
        });
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return (source == null || source.cancel(mayInterruptIfRunning))
                && super.cancel(mayInterruptIfRunning);
    }

    @Override
    public ResultSet getUninterruptibly() {
        try {
            return Uninterruptibles.getUninterruptibly(this);
        } catch (ExecutionException e) {
            throw extractCauseFromExecutionException(e);
        }
    }

    @Override
    public ResultSet getUninterruptibly(long timeout, TimeUnit unit) throws TimeoutException {
        try {
            return Uninterruptibles.getUninterruptibly(this, timeout, unit);
        } catch (ExecutionException e) {
            throw extractCauseFromExecutionException(e);
        }
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

    /**
     * Taken from original DefaultResutSetFuture
     */
    private static RuntimeException extractCauseFromExecutionException(ExecutionException e) {
        // We could just rethrow e.getCause(). However, the cause of the ExecutionException has likely been
        // created on the I/O thread receiving the response. Which means that the stacktrace associated
        // with said cause will make no mention of the current thread. This is painful for say, finding
        // out which execute() statement actually raised the exception. So instead, we re-create the
        // exception.
        if (e.getCause() instanceof DriverException)
            throw ((DriverException)e.getCause()).copy();
        else
            throw new DriverInternalError("Unexpected exception thrown", e.getCause());
    }
}
