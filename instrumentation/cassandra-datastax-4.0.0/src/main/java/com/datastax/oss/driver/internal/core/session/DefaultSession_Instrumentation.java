package com.datastax.oss.driver.internal.core.session;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.session.Request;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.nr.agent.instrumentation.cassandra.CassandraUtils;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

@Weave(type = MatchType.ExactClass, originalName = "com.datastax.oss.driver.internal.core.session.DefaultSession")
public class DefaultSession_Instrumentation {
    public <RequestT extends Request, ResultT> ResultT execute(RequestT request, GenericType<ResultT> resultType) {
        Segment segment = null;
        // For the async path the completion callback owns ending the segment. This flag records that
        // the segment was successfully handed off so the finally block does not end it prematurely.
        boolean asyncSegmentHandedOff = false;

        if (request instanceof Statement && (resultType.equals(Statement.SYNC) || resultType.equals(Statement.ASYNC)) ) {
            segment = NewRelic.getAgent().getTransaction().startSegment("execute");
        }

        try {
            Object result = Weaver.callOriginal();
            if (request instanceof Statement && (resultType.equals(Statement.SYNC))) {
                return (ResultT) CassandraUtils.wrapSyncRequest((Statement) request, (ResultSet) result, getKeyspace().orElse(null), segment);
            } else if (request instanceof Statement && (resultType.equals(Statement.ASYNC))) {
                ResultT wrapped = (ResultT) CassandraUtils.wrapAsyncRequest((Statement) request, (CompletionStage<AsyncResultSet>) result, getKeyspace().orElse(null), segment);
                asyncSegmentHandedOff = true;
                return wrapped;
            } else {
                return (ResultT) result;
            }
        } catch (Exception e) {
            AgentBridge.privateApi.reportException(e);
            throw e;
        } finally {
            // End the segment for the synchronous path, and for any path where a segment was started
            // but not handed to the async completion callback (e.g. the original call threw). This
            // avoids leaking the segment until the segment timeout, which would report an inflated
            // duration.
            if (segment != null && !asyncSegmentHandedOff) {
                segment.end();
            }
        }
    }

    public Optional<CqlIdentifier> getKeyspace() {
        return Weaver.callOriginal();
    }
}
