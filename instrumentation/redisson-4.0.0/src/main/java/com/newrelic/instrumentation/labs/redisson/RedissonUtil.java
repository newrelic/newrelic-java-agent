package com.newrelic.instrumentation.labs.redisson;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;

public class RedissonUtil {

    public static Segment createSegment(String collection, String operationName) {
        return NewRelic.getAgent().getTransaction().startSegment(collection + "-" + operationName);
    }

    public static DatastoreParameters createDatastoreParameters(String collection, String operationName) {
        return DatastoreParameters.product("Redisson")
                .collection(collection)
                .operation(operationName)
                .build();
    }

    @Trace(excludeFromTransactionTrace = true)
    public static SegmentEntry createTracedSegmentEntry(String collection, String operationName) {
        Segment segment = createSegment(collection, operationName);
        DatastoreParameters params = createDatastoreParameters(collection, operationName);
        return new SegmentEntry(segment, params);
    }
}
