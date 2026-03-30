package com.nr.instrumentation;

import com.newrelic.api.agent.Segment;
import reactor.netty.Connection;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class ReactorNettyContext {

    public static final String LIBRARY = "NettyReactor";

    private static final Map<Connection, SegmentData> connectionSegments = Collections.synchronizedMap(new WeakHashMap<>());

    public static void put(Connection connection, SegmentData segmentData) {
        if (connection != null && segmentData != null) {
            connectionSegments.put(connection, segmentData);
        }
    }

    public static SegmentData remove(Connection connection) {
        if (connection == null) return null;
        return connectionSegments.remove(connection);
    }

    public static class SegmentData {

        public final Segment segment;
        public volatile URI requestUri;
        public final String httpMethod;

        public SegmentData(Segment segment, URI requestUri, String httpMethod) {
            this.segment = segment;
            this.requestUri = requestUri;
            this.httpMethod = httpMethod;
        }

        public void updateUri(URI uri) {
            if (uri != null) {
                this.requestUri = uri;
            }
        }
    }
}
