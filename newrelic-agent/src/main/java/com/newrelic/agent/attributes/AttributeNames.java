/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

public final class AttributeNames {

    // intrinsics: properties which cannot be filterable by users
    public static final String CLIENT_CROSS_PROCESS_ID_PARAMETER_NAME = "client_cross_process_id";
    public static final String EXPLAIN_PLAN_CLAMP = "explain_plan_clamp";
    public static final String REFERRING_TRANSACTION_TRACE_ID_PARAMETER_NAME = "referring_transaction_guid";
    public static final String TRIP_ID_PARAMETER_NAME = "trip_id";
    public static final String PATH_HASH_PARAMETER_NAME = "path_hash";
    public static final String SEGMENT_CLAMP = "segment_clamp";
    public static final String SIZE_LIMIT_PARAMETER_NAME = "size_limit";
    public static final String STACK_TRACE_CLAMP = "stack_trace_clamp";
    public static final String TOKEN_CLAMP = "tokenClamp";
    public static final String TRANSACTION_TRACE_ID_PARAMETER_NAME = "transaction_guid";
    public static final String CPU_TIME_PARAMETER_NAME = "cpuTime";
    public static final String GC_TIME_PARAMETER_NAME = "gc_time";
    public static final String SYNTHETICS_RESOURCE_ID = "synthetics_resource_id";
    public static final String SYNTHETICS_MONITOR_ID = "synthetics_monitor_id";
    public static final String SYNTHETICS_JOB_ID = "synthetics_job_id";
    public static final String SYNTHETICS_TYPE = "synthetics_type";
    public static final String SYNTHETICS_INITIATOR = "synthetics_initiator";
    public static final String SYNTHETICS_VERSION = "synthetics_version";
    public static final String TIMEOUT_CAUSE = "nr.timeoutCause";
    public static final String ERROR_EXPECTED = "error.expected";

    public static final String CODE_STACKTRACE = "code.stacktrace";
    public static final String COMPONENT = "component";
    public static final String HTTP_METHOD = "http.method";
    public static final String HTTP_STATUS_CODE = "http.statusCode";
    public static final String HTTP_STATUS_TEXT = "http.statusText";
    public static final String HTTP_STATUS = "httpResponseCode";
    public static final String HTTP_STATUS_MESSAGE = "httpResponseMessage";

    public static final String LOCK_THREAD_NAME = "jvm.lock_thread_name";
    public static final String THREAD_NAME = "jvm.thread_name";
    public static final String THREAD_ID = "thread.id";

    public static final String MESSAGE_REQUEST_PREFIX = "message.parameters.";

    public static final String HTTP_REQUEST_PREFIX = "request.parameters.";
    public static final String REQUEST_REFERER_PARAMETER_NAME = "request.headers.referer";
    public static final String REQUEST_ACCEPT_PARAMETER_NAME = "request.headers.accept";
    public static final String REQUEST_CONTENT_LENGTH_PARAMETER_NAME = "request.headers.contentLength";
    public static final String REQUEST_HOST_PARAMETER_NAME = "request.headers.host";
    public static final String REQUEST_USER_AGENT_PARAMETER_NAME = "request.headers.userAgent";
    public static final String REQUEST_METHOD_PARAMETER_NAME = "request.method";

    // cloud provider identifier for the resource being used
    public static final String CLOUD_RESOURCE_ID = "cloud.resource_id";
    public static final String RESPONSE_CONTENT_TYPE_PARAMETER_NAME = "response.headers.contentType";

    // high security matches
    public static final String HTTP_REQUEST_STAR = "request.parameters.*";
    public static final String MESSAGE_REQUEST_STAR = "message.parameters.*";

    // other starred properties off by default for some locations
    public static final String SOLR_STAR = "library.solr.*";
    public static final String JVM_STAR = "jvm.*";

    // these should go up on every transaction if set in the JVM
    public static final String DISPLAY_HOST = "host.displayName";
    public static final String INSTANCE_NAME = "process.instanceName";

    // the agent treats request URI as a fully configurable attribute
    public static final String REQUEST_URI = "request.uri";

    public static final String PRIORITY = "priority";
    public static final String PORT = "port";

    public static final String QUEUE_DURATION = "queueDuration";

    // Code Level Metrics
    public static final String CLM_NAMESPACE = "code.namespace";
    public static final String CLM_FUNCTION = "code.function";
}
