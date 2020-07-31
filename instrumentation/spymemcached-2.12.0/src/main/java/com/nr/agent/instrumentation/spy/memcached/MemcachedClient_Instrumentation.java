/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spy.memcached;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.internal.BulkFuture;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.StoreType;
import net.spy.memcached.transcoders.Transcoder;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

@Weave(type = MatchType.ExactClass, originalName = "net.spy.memcached.MemcachedClient")
public class MemcachedClient_Instrumentation {

    // This instruments both add() overloads, both replace() overloads, and both set() overloads (6 API methods total)
    private <T> OperationFuture<Boolean> asyncStore(StoreType st, String key, int exp, T value, Transcoder<T> tc) {
        beforeOperation();
        OperationFuture<Boolean> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync(st.toString(), future); // JAVA-2642
        return future;
    }

    public <T> OperationFuture<Boolean> touch(final String key, final int exp) {
        beforeOperation();
        OperationFuture<Boolean> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("touch", future);
        return future;
    }

    public <T> OperationFuture<Boolean> append(long cas, String key, T val, Transcoder<T> tc) {
        beforeOperation();
        OperationFuture<Boolean> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("append", future);
        return future;
    }

    public <T> OperationFuture<Boolean> append(String key, T val, Transcoder<T> tc) {
        beforeOperation();
        OperationFuture<Boolean> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("append", future);
        return future;
    }

    public <T> OperationFuture<Boolean> prepend(long cas, String key, T val, Transcoder<T> tc) {
        beforeOperation();
        OperationFuture<Boolean> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("prepend", future);
        return future;
    }

    public <T> OperationFuture<Boolean> prepend(String key, T val, Transcoder<T> tc) {
        beforeOperation();
        OperationFuture<Boolean> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("prepend", future);
        return future;
    }

    public <T> OperationFuture<CASResponse> asyncCAS(String key, long casId, int exp, T value, Transcoder<T> tc) {
        beforeOperation();
        OperationFuture<CASResponse> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("compare-and-set", future);
        return future;
    }

    // Don't instrument the replace() methods. JAVA-2642.

    public <T> GetFuture<T> asyncGet(final String key, final Transcoder<T> tc) {
        beforeOperation();
        GetFuture<T> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("get", future);
        return future;
    }

    public <T> OperationFuture<CASValue<T>> asyncGets(final String key,final Transcoder<T> tc) {
        beforeOperation();
        OperationFuture<CASValue<T>> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("get-for-cas", future);
        return future;
    }

    // Note: do not instrument the synchronous gets() method because it wraps the async one. JAVA-2641.

    @Trace(leaf = true)
    public <T> CASValue<T> getAndTouch(String key, int exp, Transcoder<T> tc) {
        beforeOperation();
        CASValue<T> retValue = Weaver.callOriginal();
        reportDatastoreMetrics("get-and-touch");
        return retValue;
    }

    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keyIter, Iterator<Transcoder<T>> tcIter) {
        beforeOperation();
        BulkFuture<Map<String, T>> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("get-bulk", future);
        return future;
    }

    public <T> OperationFuture<CASValue<T>> asyncGetAndTouch(final String key, final int exp, final Transcoder<T> tc) {
        beforeOperation();
        OperationFuture<CASValue<T>> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("get-and-touch", future);
        return future;
    }

    @Trace(leaf = true)
    public long incr(String key, long by) {
        beforeOperation();
        long retValue = Weaver.callOriginal();
        reportDatastoreMetrics("incr");
        return retValue;
    }

    @Trace(leaf = true)
    public long incr(String key, int by) {
        beforeOperation();
        long retValue = Weaver.callOriginal();
        reportDatastoreMetrics("incr");
        return retValue;
    }

    @Trace(leaf = true)
    public long decr(String key, long by) {
        beforeOperation();
        long retValue = Weaver.callOriginal();
        reportDatastoreMetrics("decr");
        return retValue;
    }

    @Trace(leaf = true)
    public long decr(String key, int by) {
        beforeOperation();
        long retValue = Weaver.callOriginal();
        reportDatastoreMetrics("decr");
        return retValue;
    }

    @Trace(leaf = true)
    public long incr(String key, long by, long def, int exp) {
        beforeOperation();
        long retValue = Weaver.callOriginal();
        reportDatastoreMetrics("incr");
        return retValue;
    }

    @Trace(leaf = true)
    public long incr(String key, int by, long def, int exp) {
        beforeOperation();
        long retValue = Weaver.callOriginal();
        reportDatastoreMetrics("incr");
        return retValue;
    }

    @Trace(leaf = true)
    public long decr(String key, long by, long def, int exp) {
        beforeOperation();
        long retValue = Weaver.callOriginal();
        reportDatastoreMetrics("decr");
        return retValue;
    }

    @Trace(leaf = true)
    public long decr(String key, int by, long def, int exp) {
        beforeOperation();
        long retValue = Weaver.callOriginal();
        reportDatastoreMetrics("decr");
        return retValue;
    }

    public OperationFuture<Long> asyncIncr(String key, long by) {
        beforeOperation();
        OperationFuture<Long> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("incr", future);
        return future;
    }

    public OperationFuture<Long> asyncIncr(String key, int by) {
        beforeOperation();
        OperationFuture<Long> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("incr", future);
        return future;
    }

    public OperationFuture<Long> asyncDecr(String key, long by) {
        beforeOperation();
        OperationFuture<Long> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("decr", future);
        return future;
    }

    public OperationFuture<Long> asyncDecr(String key, int by) {
        beforeOperation();
        OperationFuture<Long> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("decr", future);
        return future;
    }

    public OperationFuture<Long> asyncIncr(String key, long by, long def, int exp) {
        beforeOperation();
        OperationFuture<Long> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("incr", future);
        return future;
    }

    public OperationFuture<Long> asyncIncr(String key, int by, long def, int exp) {
        beforeOperation();
        OperationFuture<Long> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("incr", future);
        return future;
    }

    public OperationFuture<Long> asyncDecr(String key, long by, long def, int exp) {
        beforeOperation();
        OperationFuture<Long> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("decr", future);
        return future;
    }

    public OperationFuture<Long> asyncDecr(String key, int by, long def, int exp) {
        beforeOperation();
        OperationFuture<Long> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("decr", future);
        return future;
    }

    public OperationFuture<Long> asyncIncr(String key, long by, long def) {
        beforeOperation();
        OperationFuture<Long> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("incr", future);
        return future;
    }

    public OperationFuture<Long> asyncIncr(String key, int by, long def) {
        beforeOperation();
        OperationFuture<Long> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("incr", future);
        return future;
    }

    public OperationFuture<Long> asyncDecr(String key, long by, long def) {
        beforeOperation();
        OperationFuture<Long> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("decr", future);
        return future;
    }

    public OperationFuture<Long> asyncDecr(String key, int by, long def) {
        beforeOperation();
        OperationFuture<Long> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("decr", future);
        return future;
    }

    @Trace(leaf = true)
    public long incr(String key, long by, long def) {
        beforeOperation();
        long retValue = Weaver.callOriginal();
        reportDatastoreMetrics("incr");
        return retValue;
    }

    @Trace(leaf = true)
    public long incr(String key, int by, long def) {
        beforeOperation();
        long retValue = Weaver.callOriginal();
        reportDatastoreMetrics("incr");
        return retValue;
    }

    @Trace(leaf = true)
    public long decr(String key, long by, long def) {
        beforeOperation();
        long retValue = Weaver.callOriginal();
        reportDatastoreMetrics("decr");
        return retValue;
    }

    @Trace(leaf = true)
    public long decr(String key, int by, long def) {
        beforeOperation();
        long retValue = Weaver.callOriginal();
        reportDatastoreMetrics("decr");
        return retValue;
    }

    public OperationFuture<Boolean> delete(String key, long cas) {
        beforeOperation();
        OperationFuture<Boolean> future = Weaver.callOriginal();
        reportDatastoreMetricsAsync("delete", future);
        return future;
    }

    private void beforeOperation() {
        MemcachedUtil.OPERATION_NODE.set(null);
    }

    /**
     * Report as external on current traced method.
     *
     * Callers of this helper should have an @Trace
     *
     * @param op Operation to report
     */
    private final void reportDatastoreMetrics(String op) {
        String host = null;
        Integer port = null;

        try {
            MemcachedNode memcachedNode = MemcachedUtil.OPERATION_NODE.get();
            if (memcachedNode != null) {
                SocketAddress address = memcachedNode.getSocketAddress();
                if (address instanceof InetSocketAddress) {
                    InetSocketAddress nodeAddress = (InetSocketAddress) address;
                    host = nodeAddress.getHostName();
                    port = nodeAddress.getPort();
                }
            }
        } catch (Throwable t) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, t, "Unable to capture host/port for memcached operation");
        } finally {
            MemcachedUtil.OPERATION_NODE.set(null);
        }

        if (op.equals("get-bulk")) {
            // Reporting the instance metrics would be incorrect, because the bulk operation can send off a whole bunch
            // of operations to multiple memcached servers, while this logic is only capable of capturing instance
            // information for one of them. Fix in the future. Tracked by JAVA-2640.
            host = null;
            port = null;
        }
        NewRelic.getAgent().getTracedMethod().reportAsExternal(DatastoreParameters
                .product("Memcached")
                .collection("cache")
                .operation(op)
                .instance(host, port)
                .build());
    }

    // Async reporting - overload for OperationsFuture
    private void reportDatastoreMetricsAsync(String op, OperationFuture<?> future) {
        String host = null;
        Integer port = null;

        try {
            MemcachedNode memcachedNode = MemcachedUtil.OPERATION_NODE.get();
            if (memcachedNode != null) {
                SocketAddress address = memcachedNode.getSocketAddress();
                if (address instanceof InetSocketAddress) {
                    InetSocketAddress nodeAddress = (InetSocketAddress) address;
                    host = nodeAddress.getHostName();
                    port = nodeAddress.getPort();
                }
            }
        } catch (Throwable t) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, t, "Unable to capture host/port for memcached operation");
        } finally {
            MemcachedUtil.OPERATION_NODE.set(null);
        }

        final Segment segment = AgentBridge.getAgent().getTransaction().createAndStartTracedActivity();
        if (segment != null) {
            ExternalParameters params = DatastoreParameters
                    .product("Memcached")
                    .collection("cache")
                    .operation(op)
                    .instance(host, port)
                    .build();
            segment.reportAsExternal(params);
            if (future.isDone()) {
                segment.endAsync();
            } else {
                future.addListener(new OperationCompletionListener(segment));
            }
        }
    }

    /**
     *
     *  Async reporting - overload for GetFuture
     *
     *  Create traced activity and call reportAsExternal on the traced activitiy TracedMethod.
     *
     *  Callers of this helper should not have an @Trace.
     *
     * @param op Operation to report
     * @param future Future to add listener to.
     */
    private void reportDatastoreMetricsAsync(String op, GetFuture<?> future) {
        String host = null;
        Integer port = null;

        try {
            MemcachedNode memcachedNode = MemcachedUtil.OPERATION_NODE.get();
            if (memcachedNode != null) {
                SocketAddress address = memcachedNode.getSocketAddress();
                if (address instanceof InetSocketAddress) {
                    InetSocketAddress nodeAddress = (InetSocketAddress) address;
                    host = nodeAddress.getHostName();
                    port = nodeAddress.getPort();
                }
            }
        } catch (Throwable t) {
            AgentBridge.getAgent().getLogger().log(Level.FINER, t, "Unable to capture host/port for memcached operation");
        } finally {
            MemcachedUtil.OPERATION_NODE.set(null);
        }

        if (AgentBridge.getAgent().getTransaction(false) != null) {
            final Segment segment = NewRelic.getAgent().getTransaction().startSegment(op);
            ExternalParameters params = DatastoreParameters
                    .product("Memcached")
                    .collection("cache")
                    .operation(op)
                    .instance(host, port)
                    .build();
            segment.reportAsExternal(params);
            if (future.isDone()) {
                segment.endAsync();
            } else {
                future.addListener(new GetCompletionListener(segment));
            }
        }
    }

    /**
     *  Async reporting - overload for BulkFuture
     *
     *  Create traced activity and call reportAsExternal on the traced activitiy TracedMethod.
     *
     *  Callers of this helper should not have an @Trace.
     *
     * @param op Operation to report
     * @param future Future to add listener to.
     */
    private void reportDatastoreMetricsAsync(String op, BulkFuture<?> future) {
        String host = null;
        Integer port = null;

        // This is where we would normally grab the host and port for the instance metrics from the MemcachedNode
        // instrumentation. But for a bulk operation like this one, this would be incorrect, because the bulk operation
        // can send off a whole bunch of operations to multiple memcached servers, while this logic is only capable of
        // capturing instance information for one of them. Tracked by JAVA-2640.
        // Let's clear the thread local despite the fact that we didn't use it.
        MemcachedUtil.OPERATION_NODE.set(null);

        if (AgentBridge.getAgent().getTransaction(false) != null) {
            final Segment segment = NewRelic.getAgent().getTransaction().startSegment(op);
            ExternalParameters params = DatastoreParameters
                    .product("Memcached")
                    .collection("cache")
                    .operation(op)
                    .instance(host, port)
                    .build();
            segment.reportAsExternal(params);
            if (future.isDone()) {
                segment.endAsync();
            } else {
                future.addListener(new BulkGetCompletionListener(segment));
            }
        }
    }
}
