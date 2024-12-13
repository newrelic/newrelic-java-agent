/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.internal.WeavePackageType;

/**
 * standard metric names for RPM.
 *
 * NOTE: until we make RPM more platform agnostic, many metric names will be "shoehorned" into Rails equivalents. We
 * should fix this before shipping any java agent.
 */
public class MetricNames {

    public static final char SEGMENT_DELIMITER = '/';
    public static final String SEGMENT_DELIMITER_STRING = "/";
    public static final String ALL = "all";
    public static final String ALL_WEB = "allWeb";
    public static final String ALL_OTHER = "allOther";

    public static final String CLIENT_APPLICATION_FORMAT = "ClientApplication/{0}/all";

    public static final String BREAKER_TRIPPED = "AgentCheck/CircuitBreaker/tripped/all";
    // The breaker metrics can accommodate multiple breakers (each having its own cause metric)
    public static final String BREAKER_TRIPPED_MEMORY = "AgentCheck/CircuitBreaker/tripped/memory";

    public static final String CPU = "CPU/User Time";
    public static final String CPU_UTILIZATION = "CPU/User/Utilization";

    public static final String MEMORY = "Memory/Physical";
    public static final String MEMORY_USED = "Memory/Used";

    public static final String HEAP_USED = "Memory/Heap/Used";
    public static final String HEAP_COMMITTED = "Memory/Heap/Committed";
    public static final String HEAP_MAX = "Memory/Heap/Max";
    public static final String HEAP_UTILIZATION = "Memory/Heap/Utilization";

    public static final String NON_HEAP_USED = "Memory/NonHeap/Used";
    public static final String NON_HEAP_COMMITTED = "Memory/NonHeap/Committed";
    public static final String NON_HEAP_MAX = "Memory/NonHeap/Max";

    public static final String MEMORY_POOL_USED_MASK = "MemoryPool/{0}/{1}/Used";
    public static final String MEMORY_POOL_COMMITTED_MASK = "MemoryPool/{0}/{1}/Committed";
    public static final String MEMORY_POOL_MAX_MASK = "MemoryPool/{0}/{1}/Max";

    public static final String THREAD_COUNT = "Threads/all";
    public static final String THREAD_DEADLOCK_COUNT = "Threads/Deadlocks/all";

    public static final String TOTAL_TIME = "TotalTime";
    public static final String FIRST_BYTE = "TimeToFirstByte";
    public static final String LAST_BYTE = "TimeToLastByte";

    public static final String CPU_PREFIX = "CPU/";
    public static final String CPU_WEB = "CPU/WebTransaction";
    public static final String CPU_OTHER = "CPU/OTherTransaction";

    public static final String WEB_TRANSACTION = "WebTransaction";
    public static final String WEB_TRANSACTION_TOTAL_TIME = WEB_TRANSACTION + TOTAL_TIME;
    public static final String WEB_TRANSACTION_FIRST_BYTE = WEB_TRANSACTION + FIRST_BYTE;
    public static final String WEB_TRANSACTION_LAST_BYTE = WEB_TRANSACTION + LAST_BYTE;
    public static final String UNKNOWN = "Unknown";
    public static final String UNKNOWN_HOST = "UnknownHost";
    public static final String URI = "Uri";
    public static final String URI_WEB_TRANSACTION = WEB_TRANSACTION + "/Uri";
    public static final String NORMALIZED_URI = "NormalizedUri";
    public static final String NORMALIZED_URI_WEB_TRANSACTION = WEB_TRANSACTION + "/NormalizedUri";
    public static final String OTHER_TRANSACTION = "OtherTransaction";
    public static final String OTHER_TRANSACTION_INITIALIZER = OTHER_TRANSACTION + "/Initializer";
    public static final String OTHER_TRANSACTION_JOB = OTHER_TRANSACTION + "/Job";
    public static final String OTHER_TRANSACTION_ALL = OTHER_TRANSACTION + "/all";
    public static final String OTHER_TRANSACTION_TOTAL_TIME = OTHER_TRANSACTION + TOTAL_TIME;
    public static final String OTHER_TRANSACTION_TOTAL_TIME_ALL = OTHER_TRANSACTION + TOTAL_TIME + "/all";
    public static final String OTHER_TRANSACTION_CUSTOM = OTHER_TRANSACTION + "/Custom";
    public static final String CUSTOM = "Custom";
    public static final String JAVA = "Java";
    public static final String DISPATCHER = "HttpDispatcher";
    public static final String REQUEST_DISPATCHER = "RequestDispatcher";
    public static final String GRAPHQL = "GraphQL";
    public static final String APDEX = "Apdex";
    public static final String APDEX_OTHER = "ApdexOther";
    public static final String APDEX_OTHER_TRANSACTION = "ApdexOther/Transaction";
    public static final String ERRORS = "Errors";
    public static final String ERRORS_EXPECTED = "ErrorsExpected";
    public static final String ERRORS_SLASH = "Errors/";
    public static final String ERRORS_ALL = ERRORS + "/" + ALL;
    public static final String ERRORS_EXPECTED_ALL = ERRORS_EXPECTED + "/" + ALL;
    public static final String WEB_TRANSACTION_ERRORS_ALL = ERRORS + "/" + ALL_WEB;
    public static final String OTHER_TRANSACTION_ERRORS_ALL = ERRORS + "/" + ALL_OTHER;

    public static final String SESSION_COUNT = "Sessions";

    public static final String EXTERNAL_PATH = "External";
    public static final String EXTERNAL_METRIC_FORMAT = EXTERNAL_PATH + "/{0}/{1}"; // External/amazon/S3
    public static final String EXTERNAL_TRANSACTION_SEGMENT_FORMAT = EXTERNAL_METRIC_FORMAT + "/{2}"; // External/amazon/S3/listBuckets
    public static final String EXTERNAL_ALL = "External/all";
    public static final String EXTERNAL_ERRORS = "ExternalErrors";

    // a rollup for all external errors
    public static final String EXTERNAL_ERRORS_ALL = "ExternalErrors/all";

    public static final String WEB_TRANSACTION_EXTERNAL_ALL = EXTERNAL_PATH + SEGMENT_DELIMITER + ALL_WEB;
    public static final String OTHER_TRANSACTION_EXTERNAL_ALL = EXTERNAL_PATH + SEGMENT_DELIMITER + ALL_OTHER;

    public static final String JAVA_OTHER = "Java/other";

    public static final String JMX = "JMX";
    public static final String JMX_WITH_SLASH = JMX + "/";
    public static final String JMX_CUSTOM = "JmxBuiltIn";
    public static final String JMX_THREAD_POOL = "JmxBuiltIn/ThreadPool/";
    // Full Metric: JMXBuiltIn/ThreadPool/{name}/Max
    public static final String JMX_THREAD_POOL_MAX = "Max";
    public static final String JMX_THREAD_POOL_ACTIVE = "Active";
    public static final String JMX_THREAD_POOL_IDLE = "Idle";
    public static final String JMX_THREAD_POOL_STANDBY = "Standby";

    public static final String JMX_SESSION = "JmxBuiltIn/Session/";
    // Full Metric: JMXBuiltIn/Session/{webAppName}/averageAliveTime
    public static final String JMX_SESSION_ALIVE_TIME = "AverageAliveTime";
    public static final String JMX_SESSION_ACTIVE = "Active";
    public static final String JMX_SESSION_REJECTED = "Rejected";
    public static final String JMX_SESSION_EXPIRED = "Expired";

    public static final String JMX_TRANSACITON = "JmxBuiltIn/Transactions/";
    // Full Metric: JMXBuiltIn/Transaction/Currently/Active
    public static final String JMX_TRANS_ACTIVE = "Currently/Active";
    public static final String JMX_TRANS_NESTED = "Created/Nested";
    public static final String JMX_TRANS_TOP_LEVEL = "Created/Top Level";
    public static final String JMX_TRANS_COMMITTED = "Outcome/Committed";
    public static final String JMX_TRANS_ROLLED_BACK = "Outcome/Rolled Back";

    public static final String JMX_THREAD = "JmxBuiltIn/Threads/";
    // Full Metric: JMXBuiltIn/Threading/Thread Count
    public static final String JMX_THREAD_COUNT = "Thread Count";
    // Full Metric: JMXBuiltIn/Threading/TotalStartedCount
    public static final String JMX_THREAD_TOTAL_COUNT = "TotalStartedCount";

    public static final String JMX_CLASSES = "JmxBuiltIn/Classes/";
    // Full Metric: JMXBuiltIn/Classes/Loaded
    public static final String JMX_LOADED_CLASSES = "Loaded";
    public static final String JMX_UNLOADED_CLASSES = "Unloaded";

    public static final String JMX_DATASOURCES = "JmxBuiltIn/DataSources/";
    // Full Metric: JMXBuiltIn/DataSources/{datasource name}/Connections/Available
    public static final String JMX_CONNECTIONS_AVAILABLE = "Connections/Available";
    public static final String JMX_CONNECTIONS_POOL_SIZE = "Connections/PoolSize";
    public static final String JMX_CONNECTIONS_CREATED = "Connections/Created";
    public static final String JMX_CONNECTIONS_ACTIVE = "Connections/Active";
    public static final String JMX_CONNECTIONS_LEAKED = "Connections/Leaked";
    public static final String JMX_CONNECTIONS_MAX = "Connections/Max";
    public static final String JMX_CONNECTIONS_IDLE = "Connections/Idle";
    public static final String JMX_CONNECTION_WAITING_REQUEST_COUNT = "Requests/Currently Waiting";
    public static final String JMX_CONNECTION_TOTAL_REQUEST_COUNT = "Requests/Count";
    public static final String JMX_CONNECTION_REQUEST_SUCCESS = "Requests/Successful";
    public static final String JMX_CONNECTION_REQUEST_FAILURE = "Requests/Failed";
    public static final String JMX_CONNECTIONS_CACHE_SIZE = "Statement Cache/Size";
    public static final String JMX_CONNECTIONS_MANAGED_COUNT = "Connections/Managed";
    public static final String JMX_CONNECTIONS_DESTROYED = "Connections/Destroyed";
    public static final String JMX_CONNECTIONS_HANDLE_COUNT = "Connections/Handle";
    public static final String JMX_CONNECTIONS_WAIT_TIME = "Connections/Wait time";

    public static final String JMX_EJB_POOL = "JmxBuiltIn/EJB/Pool/Bean/";
    // Full Metric: JmxBuiltIn/EJB/Pool/Bean/{bean name}/Active
    public static final String JMX_ACTIVE_BEANS = "Beans/Active";
    public static final String JMX_AVAILABLE_BEANS = "Beans/Available";
    public static final String JMX_DESTROY_BEANS = "Beans/Destroyed";
    public static final String JMX_FAILED_ATTEMPTS = "Attempts/Failed";
    public static final String JMX_SUCCESSFUL_ATTEMPTS = "Attempts/Successful";
    public static final String JMX_THREADS_WAITING = "Threads/Waiting";

    public static final String JMX_JTA = "JmxBuiltIn/JTA/";
    // Full Metric: JmxBuiltIn/JTA/{name}/Count
    public static final String JMX_EJB_TRANSACTION_APPLICATION = "JmxBuiltIn/EJB/Transactions/Application/";
    // Full Metric: JmxBuiltIn/EJBTransaction/Application/{application name}/Count
    public static final String JMX_EJB_TRANSACTION_MODULE = "JmxBuiltIn/EJB/Transactions/Module/";
    // Full Metric: JmxBuiltIn/EJBTransaction/Module/{application name}/{module name}/Access
    public static final String JMX_EJB_TRANSACTION_BEAN = "JmxBuiltIn/EJB/Transactions/Bean/";
    // Full Metric: JmxBuiltIn/EJBTransaction/Bean/{application name}/{module name}/{bean name}/Access
    public static final String JMX_COUNT = "Count";
    public static final String JMX_COMMIT = "Committed";
    public static final String JMX_ROLLBACK = "Rolled Back";
    public static final String JMX_ABANDONED = "Abandoned";
    public static final String JMX_TIMEOUT = "Timed Out";

    public static final String NETWORK_INBOUND_STATUS_CODE = "Network/Inbound/StatusCode/";

    public static final String SOLR = "Solr";
    public static final String SOLR_CLIENT = "SolrClient";
    public static final String SOLR_ALL = SOLR + "/all";
    public static final String ORM = "ORM";
    public static final String ORM_ALL = ORM + "/all";

    // public static final String QUEUE_WAIT = "WebFrontend/Mongrel/Average Queue Time";
    public static final String QUEUE_TIME = "WebFrontend/QueueTime";
    public static final String GC_CUMULATIVE = "GC/cumulative";

    public static final String STRUTS_ACTION = "StrutsAction";
    public static final String STRUTS_ACTION_PREFIX = STRUTS_ACTION + "/";

    public static final String TIMEOUT_ASYNC = "Java/Timeout/asyncActivityNotStarted";

    public static final String SUPPORTABILITY_AZURE_SITE_EXT_INSTALL_TYPE = "Supportability/Java/InstallType";

    public static final String SUPPORTABILITY_JAVA_AGENTVERSION = "Supportability/Java/AgentVersion/{0}";

    public static final String SUPPORTABILITY_HARVEST_SERVICE_RESPONSE_TIME = "Supportability/Harvest";

    public static final String SUPPORTABILITY_ERROR_SERVICE_TRANSACTION_ERROR_SENT = "Supportability/Events/TransactionError/Sent";
    public static final String SUPPORTABILITY_ERROR_SERVICE_TRANSACTION_ERROR_SEEN = "Supportability/Events/TransactionError/Seen";

    public static final String SUPPORTABILITY_TRANSACTION_EVENT_SERVICE_TRANSACTION_EVENT_SENT = "Supportability/Events/TransactionEvent/Sent";
    public static final String SUPPORTABILITY_TRANSACTION_EVENT_SERVICE_TRANSACTION_EVENT_SEEN = "Supportability/Events/TransactionEvent/Seen";

    public static final String SUPPORTABILITY_INSIGHTS_SERVICE_CUSTOMER_SENT = "Supportability/Events/Customer/Sent";
    public static final String SUPPORTABILITY_INSIGHTS_SERVICE_CUSTOMER_SEEN = "Supportability/Events/Customer/Seen";

    public static final String SUPPORTABILITY_LOGGING_FORWARDING_SENT = "Supportability/Logging/Forwarding/Sent";
    public static final String SUPPORTABILITY_LOGGING_FORWARDING_SEEN = "Supportability/Logging/Forwarding/Seen";
    public static final String LOGGING_FORWARDING_DROPPED = "Logging/Forwarding/Dropped";

    public static final String SUPPORTABILITY_LOGGING_METRICS_JAVA_ENABLED = "Supportability/Logging/Metrics/Java/enabled";
    public static final String SUPPORTABILITY_LOGGING_METRICS_JAVA_DISABLED = "Supportability/Logging/Metrics/Java/disabled";
    public static final String SUPPORTABILITY_LOGGING_FORWARDING_JAVA_ENABLED = "Supportability/Logging/Forwarding/Java/enabled";
    public static final String SUPPORTABILITY_LOGGING_FORWARDING_JAVA_DISABLED = "Supportability/Logging/Forwarding/Java/disabled";
    public static final String SUPPORTABILITY_LOGGING_LOCAL_DECORATING_JAVA_ENABLED = "Supportability/Logging/LocalDecorating/Java/enabled";
    public static final String SUPPORTABILITY_LOGGING_LOCAL_DECORATING_JAVA_DISABLED = "Supportability/Logging/LocalDecorating/Java/disabled";

    public static final String SUPPORTABILITY_EVENT_HARVEST_REPORT_PERIOD_IN_SECONDS = "Supportability/EventHarvest/ReportPeriod";
    public static final String SUPPORTABILITY_ERROR_SERVICE_REPORT_PERIOD_IN_SECONDS = "Supportability/EventHarvest/ErrorEventData/ReportPeriod";
    public static final String SUPPORTABILITY_INSIGHTS_SERVICE_REPORT_PERIOD_IN_SECONDS = "Supportability/EventHarvest/CustomEventData/ReportPeriod";
    public static final String SUPPORTABILITY_LOG_SENDER_SERVICE_REPORT_PERIOD_IN_SECONDS = "Supportability/EventHarvest/LogEventData/ReportPeriod";
    public static final String SUPPORTABILITY_SPAN_EVENT_SERVICE_REPORT_PERIOD_IN_SECONDS = "Supportability/EventHarvest/SpanEventData/ReportPeriod";
    public static final String SUPPORTABILITY_ANALYTIC_EVENT_SERVICE_REPORT_PERIOD_IN_SECONDS = "Supportability/EventHarvest/AnalyticEventData/ReportPeriod";

    public static final String SUPPORTABILITY_ERROR_EVENT_DATA_HARVEST_LIMIT = "Supportability/EventHarvest/ErrorEventData/HarvestLimit";
    public static final String SUPPORTABILITY_CUSTOM_EVENT_DATA_HARVEST_LIMIT = "Supportability/EventHarvest/CustomEventData/HarvestLimit";
    public static final String SUPPORTABILITY_LOG_EVENT_DATA_HARVEST_LIMIT = "Supportability/EventHarvest/LogEventData/HarvestLimit";
    public static final String SUPPORTABILITY_ANALYTIC_EVENT_DATA_HARVEST_LIMIT = "Supportability/EventHarvest/AnalyticEventData/HarvestLimit";
    public static final String SUPPORTABILITY_SPAN_EVENT_DATA_HARVEST_LIMIT = "Supportability/EventHarvest/SpanEventData/HarvestLimit";

    public static final String SUPPORTABILITY_CONNECT_MISSING_EVENT_DATA = "Supportability/Agent/Collector/MissingEventHarvestConfig";

    public static final String SUPPORTABILITY_METRIC_HARVEST_INTERVAL = "Supportability/MetricHarvest/interval";
    public static final String SUPPORTABILITY_METRIC_HARVEST_TRANSMIT = "Supportability/MetricHarvest/transmit";
    public static final String SUPPORTABILITY_METRIC_HARVEST_COUNT = "Supportability/MetricHarvest/count";
    public static final String AGENT_METRICS_COUNT = "Agent/Metrics/Count";

    public static final String SUPPORTABILITY_ERROR_SERVICE_EVENT_HARVEST_INTERVAL = "Supportability/EventHarvest/TransactionError/interval";
    public static final String SUPPORTABILITY_ERROR_SERVICE_EVENT_HARVEST_TRANSMIT = "Supportability/EventHarvest/TransactionError/transmit";

    public static final String SUPPORTABILITY_TRANSACTION_EVENT_SERVICE_EVENT_HARVEST_INTERVAL = "Supportability/EventHarvest/TransactionEvent/interval";
    public static final String SUPPORTABILITY_TRANSACTION_EVENT_SERVICE_EVENT_HARVEST_TRANSMIT = "Supportability/EventHarvest/TransactionEvent/transmit";

    public static final String SUPPORTABILITY_INSIGHTS_SERVICE_EVENT_HARVEST_INTERVAL = "Supportability/EventHarvest/Customer/interval";
    public static final String SUPPORTABILITY_INSIGHTS_SERVICE_EVENT_HARVEST_TRANSMIT = "Supportability/EventHarvest/Customer/transmit";

    public static final String SUPPORTABILITY_LOG_SENDER_SERVICE_EVENT_HARVEST_INTERVAL = "Supportability/EventHarvest/LogEvent/interval";
    public static final String SUPPORTABILITY_LOG_SENDER_SERVICE_EVENT_HARVEST_TRANSMIT = "Supportability/EventHarvest/LogEvent/transmit";

    public static final String SUPPORTABILITY_SPAN_SERVICE_EVENT_HARVEST_INTERVAL = "Supportability/EventHarvest/SpanEvent/interval";
    public static final String SUPPORTABILITY_SPAN_SERVICE_EVENT_HARVEST_TRANSMIT = "Supportability/EventHarvest/SpanEvent/transmit";

    public static final String SUPPORTABILITY_ASYNC_FINISH_SEGMENT_NOT_CALLED = "Supportability/Timeout/finishSegmentNotCalled";

    public static final String SUPPORTABILITY_TRANSACTION_SIZE = "Supportability/TransactionSize";
    public static final String SUPPORTABILITY_TRANSACTION_SIZE_CLAMP = "Supportability/TransactionSizeClamp";
    public static final String SUPPORTABILITY_TRANSACTION_SEGMENT_CLAMP = "Supportability/TransactionSegmentClamp";

    public static final String SUPPORTABILITY_ASYNC_TOKEN_CREATE = "Supportability/Async/Token/Create";
    public static final String SUPPORTABILITY_ASYNC_TOKEN_EXPIRE = "Supportability/Async/Token/Expire";
    public static final String SUPPORTABILITY_ASYNC_TOKEN_TIMEOUT = "Supportability/Async/Token/Timeout";
    public static final String SUPPORTABILITY_ASYNC_TOKEN_TIMEOUT_CAUSE = "Supportability/Async/Token/Timeout/Cause/{0}";
    public static final String SUPPORTABILITY_ASYNC_TOKEN_LINK_SUCCESS = "Supportability/Async/Token/Link/Success";
    public static final String SUPPORTABILITY_ASYNC_TOKEN_LINK_IGNORE = "Supportability/Async/Token/Link/Ignore";

    // cloud metadata errors
    public static final String SUPPORTABILITY_AWS_ERROR = "Supportability/utilization/aws/error";
    public static final String SUPPORTABILITY_PCF_ERROR = "Supportability/utilization/pcf/error";
    public static final String SUPPORTABILITY_AZURE_ERROR = "Supportability/utilization/azure/error";
    public static final String SUPPORTABILITY_GCP_ERROR = "Supportability/utilization/gcp/error";
    public static final String SUPPORTABILITY_BOOT_ID_ERROR = "Supportability/utilization/boot_id/error";
    public static final String SUPPORTABILITY_MEMORY_ERROR = "Supportability/utilization/memory/error";
    public static final String SUPPORTABILITY_KUBERNETES_ERROR = "Supportability/utilization/kubernetes/error";

    public static final String SUPPORTABILITY_DATASTORE_PREFIX = "Supportability/Datastore/";
    public static final String SUPPORTABILITY_DATASTORE_UNPARSED_QUERY = "/unparsedQuery";
    public static final String SUPPORTABILITY_DATASTORE_UNKNOWN_HOST = "/unknownHost";
    public static final String SUPPORTABILITY_DATASTORE_UNKNOWN_PORT = "/unknownPort";
    public static final String SUPPORTABILITY_DATASTORE_UNKNOWN_DATABASE_NAME = "/unknownDatabaseName";

    public static final String SUPPORTABILITY_INIT = "Supportability/Instrumented/";
    public static final String SUPPORTABILITY_INSTRUMENT = SUPPORTABILITY_INIT + "{0}/{1}{2}";
    public static final String SUPPORTABILITY_PROXY = SUPPORTABILITY_INIT + "Proxy";

    public static final String SUPPORTABILITY_WEAVE_LOADED = "Supportability/WeaveInstrumentation/Loaded/{0}/{1}";
    public static final String SUPPORTABILITY_WEAVE_CUSTOM_LOADED = "Supportability/WeaveInstrumentation/Loaded/Custom/{0}/{1}";
    public static final String SUPPORTABILITY_WEAVE_SKIPPED = "Supportability/WeaveInstrumentation/Skipped/{0}/{1}";
    public static final String SUPPORTABILITY_WEAVE_CUSTOM_SKIPPED = "Supportability/WeaveInstrumentation/Skipped/Custom/{0}/{1}";
    public static final String SUPPORTABILITY_WEAVE_CLASS = "Supportability/WeaveInstrumentation/WeaveClass/{0}/{1}";

    public static final String SUPPORTABILITY_LOADED_CLASSES_SOURCE_VERSION = "Supportability/LoadedClasses/{0}/{1}/count";
    public static final String SUPPORTABILITY_SOURCE_LANGUAGE_VERSION = "Supportability/SourceLanguage/{0}/{1}";
    public static final String SUPPORTABILITY_JVM_VENDOR = "Supportability/Jvm/Vendor/{0}";

    public static final String SUPPORTABILITY_POINTCUT_LOADED = "Supportability/PointCutInstrumentation/Loaded/{0}";

    public static final String SUPPORTABILITY_TIMING = "Supportability/Timing/";
    public static final String SUPPORTABILITY_TIMING_PREMAIN = SUPPORTABILITY_TIMING + "Premain";

    public static final String SUPPORTABILITY_LITE_MODE = "Supportability/litemode"; // feature is enabled

    // MessageBroker/<Framework>/<DestinationType>/Produce/Named/<DestinationName>
    // e.g., MessageBroker/JMS/Queue/Produce/Named/QueueName
    public static final String MESSAGE_BROKER_PRODUCE_NAMED = "MessageBroker/{0}/{1}/Produce/Named/{2}";
    public static final String MESSAGE_BROKER_PRODUCE_TEMP = "MessageBroker/{0}/{1}/Produce/Temp";
    public static final String MESSAGE_BROKER_CONSUME_NAMED = "MessageBroker/{0}/{1}/Consume/Named/{2}";
    public static final String MESSAGE_BROKER_CONSUME_TEMP = "MessageBroker/{0}/{1}/Consume/Temp";

    //  API tracking supportability metrics that include API source. e.g. Supportability/API/Ignore/{source}
    // tokens
    public static final String SUPPORTABILITY_API_TOKEN = "Token";
    public static final String SUPPORTABILITY_API_TOKEN_LINK = "Token/Link";
    public static final String SUPPORTABILITY_API_TOKEN_EXPIRE = "Token/Expire";

    // transaction
    public static final String SUPPORTABILITY_API_SEGMENT = "Segment";
    public static final String SUPPORTABILITY_API_SEGMENT_END = "Segment/End";
    public static final String SUPPORTABILITY_API_SEGMENT_IGNORE = "Segment/Ignore";
    public static final String SUPPORTABILITY_API_SEGMENT_SET_METRIC_NAME = "Segment/SetMetricName";
    public static final String SUPPORTABILITY_API_IGNORE = "Ignore";
    public static final String SUPPORTABILITY_API_IGNORE_APDEX = "IgnoreApdex";
    public static final String SUPPORTABILITY_API_IGNORE_ERRORS = "IgnoreErrors";
    public static final String SUPPORTABILITY_API_SET_TRANSACTION_NAME = "SetTransactionName";

    // cat
    public static final String SUPPORTABILITY_API_PROCESS_REQUEST_METADATA = "ProcessRequestMetadata";
    public static final String SUPPORTABILITY_API_PROCESS_RESPONSE_METADATA = "ProcessResponseMetadata";

    // external
    public static final String SUPPORTABILITY_API_REPORT_AS_EXTERNAL = "ReportAsExternal";

    // insights
    public static final String SUPPORTABILITY_API_RECORD_CUSTOM_EVENT = "RecordCustomEvent";

    public static final String SUPPORTABILITY_API_RECORD_LOG_EVENT = "RecordLogEvent";

    // attributes
    public static final String SUPPORTABILITY_API_ADD_CUSTOM_PARAMETER = "AddCustomParameter";

    // newrelic api
    public static final String SUPPORTABILITY_API_NOTICE_ERROR = "NoticeError";
    public static final String SUPPORTABILITY_API_SET_APP_SERVER_PORT = "SetAppServerPort";
    public static final String SUPPORTABILITY_API_SET_INSTANCE_NAME = "SetInstanceName";
    public static final String SUPPORTABILITY_API_SET_PRODUCT_NAME = "SetProductName";
    public static final String SUPPORTABILITY_API_SET_SERVER_INFO = "SetServerInfo";
    public static final String SUPPORTABILITY_API_SET_USER_NAME = "SetUserName";
    public static final String SUPPORTABILITY_API_SET_ACCOUNT_NAME = "SetAccountName";
    public static final String SUPPORTABILITY_API_SET_USER_ID = "SetUserId";

    // Cloud API
    public static final String SUPPORTABILITY_API_CLOUD_SET_ACCOUNT_INFO_CLIENT = "Cloud/SetAccountInfoClient/";
    public static final String SUPPORTABILITY_API_CLOUD_SET_ACCOUNT_INFO = "Cloud/SetAccountInfo/";
    public static final String SUPPORTABILITY_CONFIG_AWS_ACCOUNT_ID = "Supportability/Cloud/ConfigAccountInfo/aws_account_id";

    //Transaction supportability metrics
    public static final String SUPPORTABILITY_TRANSACTION_STARTED = "Supportability/Transaction/StartedCount";
    public static final String SUPPORTABILITY_TRANSACTION_FINISHED = "Supportability/Transaction/FinishedCount";
    public static final String SUPPORTABILITY_TRANSACTION_CANCELLED = "Supportability/Transaction/CancelledCount";
    public static final String SUPPORTABILITY_HARVEST_TRANSACTION_STARTED = "Supportability/Transaction/Harvest/StartedCount";
    public static final String SUPPORTABILITY_HARVEST_TRANSACTION_FINISHED = "Supportability/Transaction/Harvest/FinishedCount";
    public static final String SUPPORTABILITY_HARVEST_TRANSACTION_CANCELLED = "Supportability/Transaction/Harvest/CancelledCount";
    public static final String SUPPORTABILITY_TRANSACTION_REQUEST_INITIALIZED = "Supportability/Transaction/RequestInitialized";
    public static final String SUPPORTABILITY_TRANSACTION_REQUEST_DESTROYED = "Supportability/Transaction/RequestDestroyed";
    //This metric increments if requestInitialized was started but there was already a transaction present
    public static final String SUPPORTABILITY_TRANSACTION_REQUEST_INITIALIZED_STARTED = "Supportability/Transaction/RequestInitialized/TransactionStarted";

    public static final String SUPPORTABILITY_CUSTOM_REQUEST_HEADER = "CustomRequestHeader/Config/RequestHeader";
    public static final String SUPPORTABILITY_CUSTOM_REQUEST_HEADER_ALIAS = "CustomRequestHeader/Config/Alias";

    //Legacy async API supportability metrics
    public static final String SUPPORTABILITY_ASYNC_API_LEGACY_SUSPEND = "Supportability/API/LegacyAsync/Suspend";
    public static final String SUPPORTABILITY_ASYNC_API_LEGACY_RESUME = "Supportability/API/LegacyAsync/Resume";
    public static final String SUPPORTABILITY_ASYNC_API_LEGACY_COMPLETE = "Supportability/API/LegacyAsync/Complete";
    public static final String SUPPORTABILITY_ASYNC_API_LEGACY_ERROR = "Supportability/API/LegacyAsync/Error";
    public static final String SUPPORTABILITY_ASYNC_API_LEGACY_SKIP_SUSPEND = "Supportability/API/LegacyAsync/SkipSuspend";

    //This times the transform method in InstrumentationContextmanager to indicate classloading overhead
    public static final String SUPPORTABILITY_CLASSLOADER_TRANSFORM_TIME = "Supportability/Classloader/TransformTime";

    //HTTP supportability metrics broken down by response
    public static final String SUPPORTABILITY_HTTP_CODE = "Supportability/Collector/HttpCode/{0}";

    //Supportability metrics for requests to agent endpoints
    public static final String SUPPORTABILITY_AGENT_ENDPOINT_HTTP_ERROR = "Supportability/Agent/Collector/HTTPError/{0}"; // {response code}
    public static final String SUPPORTABILITY_AGENT_ENDPOINT_ATTEMPTS = "Supportability/Agent/Collector/{0}/Attempts"; // {endpoint method}
    public static final String SUPPORTABILITY_AGENT_ENDPOINT_DURATION = "Supportability/Agent/Collector/{0}/Duration"; // {endpoint method}
    public static final String SUPPORTABILITY_CONNECTION_NEW = "Supportability/Agent/Collector/Connection/New";
    public static final String SUPPORTABILITY_CONNECTION_REUSED = "Supportability/Agent/Collector/Connection/Reused";

    //Supportability metric indicating that the payload was too large
    public static final String SUPPORTABILITY_PAYLOAD_SIZE_EXCEEDS_MAX = "Supportability/Agent/Collector/MaxPayloadSizeLimit/{0}";

    // Supportability metrics for uncompressed data payloads used to measure usage
    // {0} = destination (Collector, OTLP, or InfiniteTracing).
    public static final String SUPPORTABILITY_DATA_USAGE_DESTINATION_OUTPUT_BYTES = "Supportability/Java/{0}/Output/Bytes";
    // {0} = destination (Collector, OTLP, or InfiniteTracing). {1} = agent endpoint (connect, analytic_event_data, error_data, etc)
    public static final String SUPPORTABILITY_DATA_USAGE_DESTINATION_ENDPOINT_OUTPUT_BYTES = "Supportability/Java/{0}/{1}/Output/Bytes";

    public static final String SUPPORTABILITY_AGENT_CONNECT_BACKOFF_ATTEMPTS = "Supportability/Agent/Collector/Connect/BackoffAttempts";

    // expected errors
    public static final String SUPPORTABILITY_API_EXPECTED_ERROR_API_MESSAGE = "ExpectedError/Api/Message";
    public static final String SUPPORTABILITY_API_EXPECTED_ERROR_API_THROWABLE = "ExpectedError/Api/Throwable";
    public static final String SUPPORTABILITY_API_EXPECTED_ERROR_CONFIG_CLASS = "ExpectedError/Config/Class";
    public static final String SUPPORTABILITY_API_EXPECTED_ERROR_CONFIG_CLASS_MESSAGE = "ExpectedError/Config/ClassMessage";

    // ignore errors
    public static final String SUPPORTABILITY_API_IGNORE_ERROR_CONFIG_LEGACY = "IgnoreError/Config/Legacy";
    public static final String SUPPORTABILITY_API_IGNORE_ERROR_CONFIG_CLASS = "IgnoreError/Config/Class";
    public static final String SUPPORTABILITY_API_IGNORE_ERROR_CONFIG_CLASS_MESSAGE = "IgnoreError/Config/ClassMessage";

    // Distributed tracing
    public static final String SUPPORTABILITY_DISTRIBUTED_TRACING = "Supportability/DistributedTracing"; // feature is enabled
    public static final String SUPPORTABILITY_DISTRIBUTED_TRACING_EXCLUDE_NEWRELIC_HEADER = "Supportability/DistributedTracing/ExcludeNewRelicHeader/{0}";
    public static final String SUPPORTABILITY_ACCEPT_PAYLOAD_SUCCESS = "Supportability/DistributedTrace/AcceptPayload/Success";
    public static final String SUPPORTABILITY_ACCEPT_PAYLOAD_EXCEPTION = "Supportability/DistributedTrace/AcceptPayload/Exception";
    public static final String SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_MULTIPLE_ACCEPT = "Supportability/DistributedTrace/AcceptPayload/Ignored/Multiple";
    public static final String SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_MAJOR_VERSION = "Supportability/DistributedTrace/AcceptPayload/Ignored/MajorVersion";
    public static final String SUPPORTABILITY_ACCEPT_PAYLOAD_CREATE_BEFORE_ACCEPT = "Supportability/DistributedTrace/AcceptPayload/Ignored/CreateBeforeAccept";
    public static final String SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_UNTRUSTED_ACCOUNT = "Supportability/DistributedTrace/AcceptPayload/Ignored/UntrustedAccount";
    public static final String SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_NULL = "Supportability/DistributedTrace/AcceptPayload/Ignored/Null";
    public static final String SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_PARSE_EXCEPTION = "Supportability/DistributedTrace/AcceptPayload/ParseException";
    public static final String SUPPORTABILITY_CREATE_PAYLOAD_SUCCESS = "Supportability/DistributedTrace/CreatePayload/Success";
    public static final String SUPPORTABILITY_CREATE_PAYLOAD_EXCEPTION = "Supportability/DistributedTrace/CreatePayload/Exception";

    // W3C trace context generic
    public static final String SUPPORTABILITY_TRACE_CONTEXT_ACCEPT_SUCCESS = "Supportability/TraceContext/Accept/Success";
    public static final String SUPPORTABILITY_TRACE_CONTEXT_ACCEPT_EXCEPTION = "Supportability/TraceContext/Accept/Exception";
    public static final String SUPPORTABILITY_TRACE_CONTEXT_CREATE_SUCCESS = "Supportability/TraceContext/Create/Success";
    public static final String SUPPORTABILITY_TRACE_CONTEXT_CREATE_EXCEPTION = "Supportability/TraceContext/Create/Exception";

    // W3C trace context parent
    public static final String SUPPORTABILITY_TRACE_CONTEXT_NULL_PARENT = "Supportability/TraceContext/TraceParent/Ignored/NullHeaders";
    public static final String SUPPORTABILITY_TRACE_CONTEXT_INVALID_PARENT_HEADER_COUNT = "Supportability/TraceContext/TraceParent/Ignored/Multiple";
    public static final String SUPPORTABILITY_TRACE_CONTEXT_INVALID_PARENT_FIELD_COUNT = "Supportability/TraceContext/TraceParent/Ignored/InvalidFieldCount";
    public static final String SUPPORTABILITY_TRACE_CONTEXT_INVALID_PARENT_INVALID = "Supportability/TraceContext/TraceParent/Ignored/Invalid";
    public static final String SUPPORTABILITY_TRACE_CONTEXT_PARENT_PARSE_EXCEPTION = "Supportability/TraceContext/TraceParent/Parse/Exception";

    // W3C trace context state
    public static final String SUPPORTABILITY_TRACE_CONTEXT_INVALID_STATE_HEADER_COUNT = "Supportability/TraceContext/TraceState/Ignored/Empty";
    public static final String SUPPORTABILITY_TRACE_CONTEXT_INVALID_STATE_VENDOR_COUNT = "Supportability/TraceContext/TraceState/Ignored/InvalidVendorCount";
    public static final String SUPPORTABILITY_TRACE_CONTEXT_INVALID_NR_ENTRY = "Supportability/TraceContext/TraceState/InvalidNrEntry";
    public static final String SUPPORTABILITY_TRACE_CONTEXT_NO_NR_ENTRY = "Supportability/TraceContext/TraceState/NoNrEntry";
    public static final String SUPPORTABILITY_TRACE_CONTEXT_INVALID_STATE_FIELD_COUNT = "Supportability/TraceContext/TraceState/Ignored/InvalidFieldCount";
    public static final String SUPPORTABILITY_TRACE_CONTEXT_INVALID_VENDOR_VERSION = "Supportability/TraceContext/TraceState/Ignored/InvalidVendorVersion";
    public static final String SUPPORTABILITY_TRACE_CONTEXT_INVALID_PARENT_TYPE = "Supportability/TraceContext/TraceState/Ignored/InvalidParentType";
    public static final String SUPPORTABILITY_TRACE_CONTEXT_UNTRUSTED_ACCOUNT = "Supportability/TraceContext/TraceState/Ignored/UntrustedAccount";
    public static final String SUPPORTABILITY_TRACE_CONTEXT_STATE_PARSE_EXCEPTION = "Supportability/TraceContext/TraceState/Parse/Exception";

    // Span events
    public static final String SUPPORTABILITY_SPAN_EVENTS = "Supportability/SpanEvents"; // feature is enabled
    public static final String SUPPORTABILITY_SPAN_EVENT_TOTAL_EVENTS_SENT = "Supportability/SpanEvent/TotalEventsSent";
    public static final String SUPPORTABILITY_SPAN_EVENT_TOTAL_EVENTS_SEEN = "Supportability/SpanEvent/TotalEventsSeen";
    public static final String SUPPORTABILITY_SPAN_EVENT_LIMIT = "Supportability/SpanEvent/Limit";
    public static final String SUPPORTABILITY_SPAN_EVENT_TOTAL_EVENTS_DISCARDED = "Supportability/SpanEvent/Discarded";

    public static final String SUPPORTABILITY_INTERNAL_CUSTOM_EVENTS_TOTAL_EVENTS_SENT = "Supportability/InternalCustomEvents/TotalEventsSent";
    public static final String SUPPORTABILITY_INTERNAL_CUSTOM_EVENTS_TOTAL_EVENTS_SEEN = "Supportability/InternalCustomEvents/TotalEventsSeen";
    public static final String SUPPORTABILITY_INTERNAL_CUSTOM_EVENTS_TOTAL_EVENTS_DISCARDED = "Supportability/InternalCustomEvents/Discarded";

    // Deprecated features
    public static final String SUPPORTABILITY_DEPRECATED_CONFIG_JAR_COLLECTOR = "Supportability/Deprecated/Config/JarCollector";

    // JMX
    public static final String LINKING_METADATA_MBEAN = "Supportability/LinkingMetadataMBean/Enabled";

    // parent.type/parent.account/parent.appId/transport
    public static final String PARENT_DATA = "{0}/{1}/{2}/{3}/{4}";
    public static final String PARENT_DATA_ALL = "all";
    public static final String PARENT_DATA_ALL_WEB = "allWeb";
    public static final String PARENT_DATA_ALL_OTHER = "allOther";
    public static final String DURATION_BY_PARENT = "DurationByCaller/" + PARENT_DATA;
    public static final String DURATION_BY_PARENT_UNKNOWN_ALL = "DurationByCaller/Unknown/Unknown/Unknown/{0}/all";
    public static final String DURATION_BY_PARENT_UNKNOWN_ALL_WEB = "DurationByCaller/Unknown/Unknown/Unknown/{0}/allWeb";
    public static final String ERRORS_BY_PARENT = "ErrorsByCaller/" + PARENT_DATA;
    public static final String ERRORS_BY_PARENT_UNKNOWN = "ErrorsByCaller/Unknown/Unknown/Unknown/{0}/all";
    public static final String TRANSPORT_DURATION_BY_PARENT = "TransportDuration/" + PARENT_DATA;

    // JFR Service
    public static final String SUPPORTABILITY_JFR_SERVICE_STARTED_SUCCESS = "Supportability/JfrService/Started/Success";
    public static final String SUPPORTABILITY_JFR_SERVICE_STOPPED_SUCCESS = "Supportability/JfrService/Stopped/Success";
    public static final String SUPPORTABILITY_JFR_SERVICE_STARTED_FAIL = "Supportability/JfrService/Started/Fail";
    public static final String SUPPORTABILITY_JFR_SERVICE_CONFIGURED_QUEUE_SIZE = "Supportability/JfrService/Config/QueueSize";
    public static final String SUPPORTABILITY_JFR_SERVICE_CONFIGURED_HARVEST_INTERVAL = "Supportability/JfrService/Config/HarvestInterval";

    // Error Grouping
    public static final String SUPPORTABILITY_ERROR_GROUPING_CALLBACK_ENABLED = "Supportability/ErrorGrouping/Callback/enabled";
    public static final String SUPPORTABILITY_ERROR_GROUPING_CALLBACK_EXECUTION_TIME = "Supportability/ErrorGrouping/Callback/ExecutionTime";

    // Slow transaction detection
    public static final String SUPPORTABILITY_SLOW_TXN_DETECTION_ENABLED = "Supportability/SlowTransactionDetection/enabled";
    public static final String SUPPORTABILITY_SLOW_TXN_DETECTION_DISABLED = "Supportability/SlowTransactionDetection/disabled";

    // AiMonitoring Callback Set
    public static final String SUPPORTABILITY_AI_MONITORING_TOKEN_COUNT_CALLBACK_SET = "Supportability/AiMonitoringTokenCountCallback/Set";

    // Super Agent Integration
    public static final String SUPPORTABILITY_SUPERAGENT_HEALTH_REPORTING_ENABLED = "Supportability/SuperAgent/Health/enabled";

    /**
     * Utility method for adding supportability metrics to APIs
     *
     * @param metricName string from MetricNames class
     */
    public static void recordApiSupportabilityMetric(String metricName) {
        WeavePackageType weavePackageType = AgentBridge.currentApiSource.get();
        // ignore Internal API sources
        if (!weavePackageType.isInternal()) {
            NewRelic.incrementCounter(weavePackageType.getSupportabilityMetric(metricName));
        }
    }

}
