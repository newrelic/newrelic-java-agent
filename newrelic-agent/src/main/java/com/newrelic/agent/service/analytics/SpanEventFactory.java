/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.google.common.base.Joiner;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.attributes.AttributeValidator;
import com.newrelic.agent.bridge.datastore.SqlQueryConverter;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AttributesConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.model.AttributeFilter;
import com.newrelic.agent.model.SpanCategory;
import com.newrelic.agent.model.SpanError;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.util.ExternalsUtil;
import com.newrelic.agent.util.StackTraces;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.QueryConverter;
import com.newrelic.api.agent.SlowQueryDatastoreParameters;

import java.net.URI;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.newrelic.agent.json.AttributeFilters.SPAN_EVENTS_ATTRIBUTE_FILTER;
import static com.newrelic.agent.model.SpanEvent.SPAN;

/**
 * This wraps up some of the rather complex logic involved in creating an instance of a SpanEvent.
 */
public class SpanEventFactory {

    private static final Joiner TRACE_STATE_VENDOR_JOINER = Joiner.on(",");
    // Truncate `db.statement` at 2000 characters
    private static final int DB_STATEMENT_TRUNCATE_LENGTH = 4095;
    private static final int MAX_EVENT_ATTRIBUTE_STRING_LENGTH = 4095;

    public static final Supplier<Long> DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER = System::currentTimeMillis;

    private final SpanEvent.Builder builder = SpanEvent.builder();
    private final String appName;
    private final AttributeFilter filter;
    private final Supplier<Long> timestampSupplier;

    public SpanEventFactory(String appName, AttributeFilter filter, Supplier<Long> timestampSupplier) {
        this.filter = filter;
        builder.putIntrinsic("type", SPAN);
        builder.putIntrinsic("category", SpanCategory.generic.name());
        this.appName = appName;
        this.timestampSupplier = timestampSupplier;
        builder.appName(appName);
    }

    public SpanEventFactory(String appName) {
        this(appName, SPAN_EVENTS_ATTRIBUTE_FILTER, DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER);
    }

    public SpanEventFactory setPriority(float priority) {
        builder.priority(priority);
        builder.putIntrinsic("priority", priority);
        return this;
    }

    public SpanEventFactory setParentType(String parentType) {
        builder.putIntrinsic("parent.type", parentType);
        return this;
    }

    public SpanEventFactory setParentId(String parentId) {
        builder.putIntrinsic("parentId", parentId);
        return this;
    }

    public SpanEventFactory setGuid(String guid) {
        builder.putIntrinsic("guid", guid);
        return this;
    }

    public SpanEventFactory setTraceId(String traceId) {
        builder.putIntrinsic("traceId", traceId);
        return this;
    }

    public SpanEventFactory setSampled(boolean sampled) {
        builder.putIntrinsic("sampled", sampled);
        return this;
    }

    public SpanEventFactory setDurationInSeconds(float duration) {
        builder.putIntrinsic("duration", duration);
        return this;
    }

    public SpanEventFactory setName(String name) {
        builder.putIntrinsic("name", name);
        return this;
    }

    public SpanEventFactory setUserAttributes(Map<String, ?> userAttributes) {
        builder.putAllUserAttributes(filter.filterUserAttributes(appName, userAttributes));
        return this;
    }

    public SpanEventFactory putAllAgentAttributes(Map<String, ?> agentAttributes) {
        if (agentAttributes == null || agentAttributes.isEmpty()) {
            return this;
        }
        builder.putAllAgentAttributes(filter.filterAgentAttributes(appName, agentAttributes));
        return this;
    }

    /**
     * This should be called after the span kind is set.
     */
    public SpanEventFactory setStackTraceAttributes(Map<String, Object> agentAttributes) {
        if (builder.isClientSpan()) {
            final List<StackTraceElement> stackTraceList = (List<StackTraceElement>) agentAttributes.get(DefaultTracer.BACKTRACE_PARAMETER_NAME);
            if (stackTraceList != null) {
                final List<StackTraceElement> preStackTraces = StackTraces.scrubAndTruncate(stackTraceList);
                final List<String> postParentRemovalTrace = StackTraces.toStringList(preStackTraces);

                putAgentAttribute(AttributeNames.CODE_STACKTRACE, truncateWithEllipsis(
                        Joiner.on(',').join(postParentRemovalTrace), MAX_EVENT_ATTRIBUTE_STRING_LENGTH));
            }
        }
        return this;
    }

    public SpanEventFactory setClmAttributes(Map<String, Object> agentAttributes) {
        if (agentAttributes == null || agentAttributes.isEmpty()) {
            return this;
        }
        final Object threadId = agentAttributes.get(AttributeNames.THREAD_ID);
        if (threadId != null) {
            builder.putIntrinsic(AttributeNames.THREAD_ID, threadId);
        }
        if (agentAttributes.containsKey(AttributeNames.CLM_NAMESPACE) && agentAttributes.containsKey(AttributeNames.CLM_FUNCTION)) {
            builder.putAgentAttribute(AttributeNames.CLM_NAMESPACE, agentAttributes.get(AttributeNames.CLM_NAMESPACE));
            builder.putAgentAttribute(AttributeNames.CLM_FUNCTION, agentAttributes.get(AttributeNames.CLM_FUNCTION));
        }
        return this;
    }

    public SpanEventFactory putAllUserAttributes(Map<String, ?> userAttributes) {
        userAttributes = filter.filterUserAttributes(appName, userAttributes);
        builder.putAllUserAttributes(userAttributes);
        return this;
    }


    public SpanEventFactory putAllUserAttributesIfAbsent(Map<String, ?> userAttributes) {
        builder.putAllUserAttributesIfAbsent(filter.filterUserAttributes(appName, userAttributes));
        return this;
    }

    public SpanEventFactory putAgentAttribute(String key, Object value) {
        builder.putAgentAttribute(key, value);
        return this;
    }

    public SpanEventFactory putIntrinsicAttribute(String key, Object value) {
        builder.putIntrinsic(key, value);
        return this;
    }

    public SpanEventFactory setTransactionId(String rootId) {
        builder.putIntrinsic("transactionId", rootId);
        return this;
    }

    public SpanEventFactory setTimestamp(long startTime) {
        builder.putIntrinsic("timestamp", startTime);
        return this;
    }

    public SpanEventFactory setCategory(SpanCategory category) {
        if (category != null) {
            builder.putIntrinsic("category", category.name());
        }
        return this;
    }

    public SpanEventFactory setKindFromUserAttributes() {
        builder.spanKind(builder.getSpanKindFromUserAttributes());
        return this;
    }

    // http parameter
    public SpanEventFactory setUri(URI uri) {
        if (uri == null) {
            return this;
        }

        if (filter.shouldIncludeAgentAttribute(appName, "http.url")) {
            final URI sanitizedURI = ExternalsUtil.sanitizeURI(uri);
            if (sanitizedURI != null) {
                builder.putAgentAttribute("http.url", sanitizedURI.toString());
                setServerAddress(sanitizedURI.getHost());
                if (sanitizedURI.getPort() > 0) {
                    setServerPort(sanitizedURI.getPort());
                }
            }
        }
        return this;
    }

    public SpanEventFactory setHttpMethod(String method) {
        if (filter.shouldIncludeAgentAttribute(appName, "http.method")) {
            builder.putAgentAttribute("http.method", method);
        }
        return this;
    }

    // http parameter
    public SpanEventFactory setHttpComponent(String component) {
        builder.putIntrinsic("component", component);
        return this;
    }

    public SpanEventFactory setHttpStatusCode(Integer statusCode) {
        AttributesConfig attributesConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getAttributesConfig();
        if (attributesConfig.isStandardHttpAttr() &&
                filter.shouldIncludeAgentAttribute(appName, AttributeNames.HTTP_STATUS_CODE)) {
            builder.putAgentAttribute(AttributeNames.HTTP_STATUS_CODE, statusCode);
        }
        if (attributesConfig.isLegacyHttpAttr() &&
                filter.shouldIncludeAgentAttribute(appName, AttributeNames.HTTP_STATUS)) {
            builder.putAgentAttribute(AttributeNames.HTTP_STATUS, statusCode);
        }
        return this;
    }

    public SpanEventFactory setHttpStatusText(String statusText) {
        AttributesConfig attributesConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getAttributesConfig();
        if (attributesConfig.isStandardHttpAttr() &&
                filter.shouldIncludeAgentAttribute(appName, AttributeNames.HTTP_STATUS_TEXT)) {
            builder.putAgentAttribute(AttributeNames.HTTP_STATUS_TEXT, statusText);
        }
        if (attributesConfig.isLegacyHttpAttr() &&
                filter.shouldIncludeAgentAttribute(appName, AttributeNames.HTTP_STATUS_MESSAGE)) {
            builder.putAgentAttribute(AttributeNames.HTTP_STATUS_MESSAGE, statusText);
        }
        return this;
    }

    // datastore parameter
    public SpanEventFactory setDatabaseName(String databaseName) {
        builder.putAgentAttribute("db.instance", databaseName);
        return this;
    }

    // datastore parameter
    public SpanEventFactory setDatastoreComponent(String component) {
        builder.putAgentAttribute("db.system", component);
        return this;
    }

    // datastore parameter
    public SpanEventFactory setAddress(String hostName, String portPathOrId) {
        if (portPathOrId != null && hostName != null) {
            String address = MessageFormat.format("{0}:{1}", hostName, portPathOrId);
            builder.putAgentAttribute("peer.address", address);
        }
        return this;
    }

    public SpanEventFactory setServerAddress(String host) {
        builder.putAgentAttribute("server.address", host);
        builder.putAgentAttribute("peer.hostname", host);
        return this;
    }

    public SpanEventFactory setServerPort(int port) {
        builder.putAgentAttribute("server.port", port);
        return this;
    }

    // datastore parameter
    public SpanEventFactory setDatabaseStatement(String query) {
        if (query != null) {
            builder.putAgentAttribute("db.statement", truncateWithEllipsis(query, DB_STATEMENT_TRUNCATE_LENGTH));
        }
        return this;
    }

    // datastore parameter
    private SpanEventFactory setDatabaseCollection(String collection) {
        builder.putAgentAttribute("db.collection", collection);
        return this;
    }

    // datastore parameter
    private SpanEventFactory setDatabaseOperation(String operation) {
        builder.putAgentAttribute("db.operation", operation);
        return this;
    }

    private String truncateWithEllipsis(String value, int maxLengthWithEllipsis) {
        if (value.length() > maxLengthWithEllipsis) {
            int maxLengthWithoutEllipsis = maxLengthWithEllipsis - 3;
            return AttributeValidator.truncateString(value, maxLengthWithoutEllipsis) + "...";
        }
        return value;
    }

    public SpanEventFactory setDecider(boolean decider) {
        builder.decider(decider);
        return this;
    }

    private void setErrorClass(Class<?> throwableClass, Integer errorStatus) {
        if (filter.shouldIncludeAgentAttribute(appName, "error.class")) {
            if (throwableClass != null) {
                builder.putAgentAttribute("error.class", throwableClass.getName());
            } else if (errorStatus != null) {
                builder.putAgentAttribute("error.class", errorStatus.toString());
            }
        }
    }

    private void setErrorMessage(String message) {
        if (filter.shouldIncludeAgentAttribute(appName, "error.message") && message != null && message.length() > 0) {
            builder.putAgentAttribute("error.message", message);
        }
    }

    private void setExpectedError(boolean expectedError) {
        if (filter.shouldIncludeAgentAttribute(appName, "error.expected") && expectedError) {
            builder.putAgentAttribute("error.expected", true);
        }
    }

    public SpanEventFactory setSpanError(SpanError spanError) {
        setExpectedError(spanError.isExpectedError());
        setErrorMessage(spanError.getErrorMessage());
        setErrorClass(spanError.getErrorClass(), spanError.getErrorStatus());
        return this;
    }

    public SpanEventFactory setIsRootSpanEvent(boolean isRoot) {
        if (isRoot) {
            builder.putIntrinsic("nr.entryPoint", true);
        }
        return this;
    }

    public SpanEventFactory setTrustedParent(String closestParent) {
        builder.putIntrinsic("trustedParentId", closestParent);
        return this;
    }

    public SpanEventFactory setTracingVendors(Set<String> stateVendorKeys) {
        if (stateVendorKeys != null && !stateVendorKeys.isEmpty()) {
            builder.putIntrinsic("tracingVendors", TRACE_STATE_VENDOR_JOINER.join(stateVendorKeys));
        }
        return this;
    }

    public SpanEventFactory setExternalParameterAttributes(ExternalParameters parameters) {
        if (parameters instanceof HttpParameters) {
            HttpParameters httpParameters = (HttpParameters) parameters;
            setCategory(SpanCategory.http);
            setUri(httpParameters.getUri());
            setHttpMethod(httpParameters.getProcedure());
            setHttpStatusCode(httpParameters.getStatusCode());
            setHttpStatusText(httpParameters.getStatusText());
            setHttpComponent((httpParameters).getLibrary());
            setKindFromUserAttributes();
        } else if (parameters instanceof DatastoreParameters) {
            DatastoreParameters datastoreParameters = (DatastoreParameters) parameters;
            setCategory(SpanCategory.datastore);
            setDatastoreComponent(datastoreParameters.getProduct());
            setDatabaseName(datastoreParameters.getDatabaseName());
            setDatabaseCollection(datastoreParameters.getCollection());
            setDatabaseOperation(datastoreParameters.getOperation());
            setServerAddress(datastoreParameters.getHost());
            setKindFromUserAttributes();
            if (datastoreParameters.getPort() != null) {
                setServerPort(datastoreParameters.getPort());
            }
            if (datastoreParameters instanceof SlowQueryDatastoreParameters) {
                SlowQueryDatastoreParameters<?> queryDatastoreParameters = (SlowQueryDatastoreParameters<?>) datastoreParameters;
                setDatabaseStatement(determineObfuscationLevel(queryDatastoreParameters));
            }
            if (datastoreParameters.getPort() != null) {
                setAddress(datastoreParameters.getHost(), String.valueOf(datastoreParameters.getPort()));
            } else {
                setAddress(datastoreParameters.getHost(), datastoreParameters.getPathOrId());
            }
        } else {
            setCategory(SpanCategory.generic);
        }
        return this;
    }

    private <T> String determineObfuscationLevel(SlowQueryDatastoreParameters<T> slowQueryDatastoreParameters) {
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        if (config.isHighSecurity() || config.getTransactionTracerConfig().getRecordSql().equals(SqlObfuscator.OFF_SETTING)) {
            return null;
        } else if (config.getTransactionTracerConfig().getRecordSql().equals(SqlObfuscator.RAW_SETTING)) {
            return getQueryConverter(slowQueryDatastoreParameters).toRawQueryString(slowQueryDatastoreParameters.getRawQuery());
        } else {
            return getQueryConverter(slowQueryDatastoreParameters).toObfuscatedQueryString(slowQueryDatastoreParameters.getRawQuery());
        }
    }

    private static <T> QueryConverter<T> getQueryConverter(SlowQueryDatastoreParameters<T> slowQueryDatastoreParameters) {
        final QueryConverter<T> queryConverter = slowQueryDatastoreParameters.getQueryConverter();
        if (queryConverter == SqlQueryConverter.INSTANCE) {
            return (QueryConverter<T>) ServiceFactory.getDatabaseService().getDefaultSqlObfuscator().getQueryConverter();
        }
        return queryConverter;
    }

    public SpanEvent build() {
        builder.timestamp(timestampSupplier.get());
        return builder.build();
    }
}
