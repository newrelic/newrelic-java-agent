/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.google.common.base.Joiner;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.attributes.AttributeValidator;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.interfaces.backport.Supplier;
import com.newrelic.agent.model.AttributeFilter;
import com.newrelic.agent.model.SpanCategory;
import com.newrelic.agent.model.SpanError;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.ExternalsUtil;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.SlowQueryDatastoreParameters;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

import static com.newrelic.agent.json.AttributeFilters.SPAN_EVENTS_ATTRIBUTE_FILTER;
import static com.newrelic.agent.model.SpanEvent.SPAN;

/**
 * This wraps up some of the rather complex logic involved in creating an instance of a SpanEvent.
 */
public class SpanEventFactory {

    private static final Joiner TRACE_STATE_VENDOR_JOINER = Joiner.on(",");
    // Truncate `db.statement` at 2000 characters
    private static final int DB_STATEMENT_TRUNCATE_LENGTH = 2000;

    public static Supplier<Long> DEFAULT_SYSTEM_TIMESTAMP_SUPPLIER = new Supplier<Long>() {
        @Override
        public Long get() {
            return System.currentTimeMillis();
        }
    };

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
        Object spanKind = builder.getSpanKindFromUserAttributes();
        builder.putIntrinsic("span.kind", spanKind);
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
        if (filter.shouldIncludeAgentAttribute(appName, AttributeNames.HTTP_STATUS_CODE)) {
            builder.putAgentAttribute(AttributeNames.HTTP_STATUS_CODE, statusCode);
        }
        if (filter.shouldIncludeAgentAttribute(appName, AttributeNames.HTTP_STATUS)) {
            builder.putAgentAttribute(AttributeNames.HTTP_STATUS, statusCode);
        }
        return this;
    }

    public SpanEventFactory setHttpStatusText(String statusText) {
        if (filter.shouldIncludeAgentAttribute(appName, AttributeNames.HTTP_STATUS_TEXT)) {
            builder.putAgentAttribute(AttributeNames.HTTP_STATUS_TEXT, statusText);
        }
        return this;
    }

    // datastore parameter
    public SpanEventFactory setDatabaseName(String databaseName) {
        builder.putIntrinsic("db.instance", databaseName);
        return this;
    }

    // datastore parameter
    public SpanEventFactory setDatastoreComponent(String component) {
        builder.putIntrinsic("component", component);
        return this;
    }

    // datastore parameter
    public SpanEventFactory setHostName(String host) {
        builder.putIntrinsic("peer.hostname", host);
        return this;
    }

    // datastore parameter
    public SpanEventFactory setAddress(String hostName, String portPathOrId) {
        if (portPathOrId != null && hostName != null) {
            String address = MessageFormat.format("{0}:{1}", hostName, portPathOrId);
            builder.putIntrinsic("peer.address", address);
        }
        return this;
    }

    // datastore parameter
    public SpanEventFactory setDatabaseStatement(String query) {
        if (query != null) {
            builder.putIntrinsic("db.statement", truncateWithEllipsis(query, DB_STATEMENT_TRUNCATE_LENGTH));
        }
        return this;
    }

    // datastore parameter
    private SpanEventFactory setDatabaseCollection(String collection) {
        builder.putIntrinsic("db.collection", collection);
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
            setHostName(datastoreParameters.getHost());
            setKindFromUserAttributes();
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
            return slowQueryDatastoreParameters.getQueryConverter().toRawQueryString(slowQueryDatastoreParameters.getQuery());
        } else {
            return slowQueryDatastoreParameters.getQueryConverter().toObfuscatedQueryString(slowQueryDatastoreParameters.getQuery());
        }
    }

    public SpanEvent build() {
        builder.timestamp(timestampSupplier.get());
        return builder.build();
    }
}

