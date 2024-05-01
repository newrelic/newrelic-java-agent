package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.datastore.SqlQueryConverter;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.NewRelic;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class SpanToTracerProcessor implements SpanProcessor {
    private static final AttributeKey<String> OTEL_LIBRARY_NAME = AttributeKey.stringKey("otel.library.name");
    private static final AttributeKey<String> DB_SYSTEM = AttributeKey.stringKey("db.system");
    private static final AttributeKey<String> DB_STATEMENT = AttributeKey.stringKey("db.statement");
    private static final AttributeKey<String> DB_OPERATION = AttributeKey.stringKey("db.operation");
    private static final AttributeKey<String> DB_SQL_TABLE = AttributeKey.stringKey("db.sql.table");
    private static final AttributeKey<String> DB_NAME = AttributeKey.stringKey("db.name");

    private static final AttributeKey<String> SERVER_ADDRESS = AttributeKey.stringKey("server.address");
    private static final AttributeKey<Long> SERVER_PORT = AttributeKey.longKey("server.port");
    private static final AttributeKey<String> URL_FULL = AttributeKey.stringKey("url.full");
    private static final AttributeKey<String> URL_SCHEME = AttributeKey.stringKey("url.scheme");
    private static final AttributeKey<String> RPC_METHOD = AttributeKey.stringKey("rpc.method");
    private static final AttributeKey<String> CODE_FUNCTION = AttributeKey.stringKey("code.function");
    private static final AttributeKey<String> HTTP_REQUEST_METHOD = AttributeKey.stringKey("http.request.method");

    private static final List<AttributeKey<String>> PROCEDURE_KEYS =
            Arrays.asList(CODE_FUNCTION, RPC_METHOD, HTTP_REQUEST_METHOD);

    private final Map<String, ExitTracer> tracersBySpanId = new ConcurrentHashMap<>();

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        final ExitTracer tracer = AgentBridge.instrumentation.createTracer("Span/" + span.getName(),
                TracerFlags.GENERATE_SCOPED_METRIC
                        | TracerFlags.TRANSACTION_TRACER_SEGMENT
                        | TracerFlags.CUSTOM);
        if (tracer != null) {
            final SpanKind spanKind = span.getKind();
            tracer.addCustomAttribute("span.kind", spanKind.name());
            tracersBySpanId.put(span.getSpanContext().getSpanId(), tracer);

            if (SpanKind.CLIENT == spanKind) {
                reportClientSpan(span, tracer);
            }

            NewRelic.getAgent().getMetricAggregator().recordMetric("Supportability/SpanToTracerProcessor/tracersBySpanId/size", tracersBySpanId.size());
        } else {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "No tracer, skipping span {0}", span.getName());
        }
    }

    static void reportClientSpan(ReadableSpan span, ExitTracer tracer) {
        final String dbSystem = span.getAttribute(DB_SYSTEM);
        if (dbSystem != null) {
            String operation = span.getAttribute(DB_OPERATION);
            DatastoreParameters.InstanceParameter builder = DatastoreParameters
                    .product(dbSystem)
                    .collection(span.getAttribute(DB_SQL_TABLE))
                    .operation(operation == null ? "unknown" : operation);
            String serverAddress = span.getAttribute(SERVER_ADDRESS);
            Long serverPort = span.getAttribute(SERVER_PORT);

            DatastoreParameters.DatabaseParameter instance = serverAddress == null ? builder.noInstance() :
                builder.instance(serverAddress, (serverPort == null ? Long.valueOf(0L) : serverPort).intValue());

            String dbName = span.getAttribute(DB_NAME);
            DatastoreParameters.SlowQueryParameter slowQueryParameter =
                    dbName == null ? instance.noDatabaseName() : instance.databaseName(dbName);
            final String dbStatement = span.getAttribute(DB_STATEMENT);
            final DatastoreParameters datastoreParameters;
            if (dbStatement == null) {
                datastoreParameters = slowQueryParameter.build();
            } else {
                datastoreParameters = slowQueryParameter.slowQuery(dbStatement, SqlQueryConverter.INSTANCE).build();
            }

            tracer.reportAsExternal(datastoreParameters);
        }
        // Only support the current otel spec.  Ignore client spans with old attribute names
        else {
            try {
                final URI uri = getUri(span);
                if (uri != null) {
                    final String libraryName = span.getAttribute(OTEL_LIBRARY_NAME);
                    GenericParameters genericParameters = GenericParameters.library(libraryName).uri(uri)
                            .procedure(getProcedure(span)).build();
                    tracer.reportAsExternal(genericParameters);
                }
            } catch (URISyntaxException e) {
                NewRelic.getAgent().getLogger().log(Level.FINER, "Error parsing client span uri", e);
            }
        }
    }

    static String getProcedure(ReadableSpan span) {
        for (AttributeKey<String> key : PROCEDURE_KEYS) {
            String value = span.getAttribute(key);
            if (value != null) {
                return value;
            }
        }
        return "unknown";
    }

    static URI getUri(ReadableSpan span) throws URISyntaxException {
        final String urlFull = span.getAttribute(URL_FULL);
        if (urlFull != null) {
            return URI.create(urlFull);
        } else {
            final String serverAddress = span.getAttribute(SERVER_ADDRESS);
            if (serverAddress != null) {
                final String scheme = span.getAttribute(URL_SCHEME);
                final Long serverPort = span.getAttribute(SERVER_PORT);
                return new URI(scheme == null ? "http" : scheme, null, serverAddress,
                        serverPort == null ? 0 : serverPort.intValue(), null, null, null);
            }
        }
        return null;
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        final ExitTracer tracer = tracersBySpanId.remove(span.getSpanContext().getSpanId());
        if (tracer != null) {
            tracer.addCustomAttribute("span.name", span.getName());
            tracer.finish();
        }
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }
}
