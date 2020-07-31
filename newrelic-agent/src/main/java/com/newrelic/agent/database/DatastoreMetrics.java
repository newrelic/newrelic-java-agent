/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.config.Hostname;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TracedMethod;

/**
 * Utility class for sending Datastore metrics.
 */
public class DatastoreMetrics {

    public static final String METRIC_NAMESPACE = "Datastore";

    public static final String SLASH = "/";
    public static final String SLASH_ALL = "/all";
    public static final String SLASH_ALL_WEB = "/allWeb";
    public static final String SLASH_ALL_OTHER = "/allOther";
    public static final String DATASTORE = METRIC_NAMESPACE + SLASH;
    public static final String STATEMENT = "Datastore/statement/";
    public static final String OPERATION = "Datastore/operation/";

    public static final String ALL = METRIC_NAMESPACE + "/all";// Datastore/all
    public static final String ALL_WEB = METRIC_NAMESPACE + "/allWeb";// Datastore/allWeb
    public static final String ALL_OTHER = METRIC_NAMESPACE + "/allOther";// Datastore/allOther

    private static final String VENDOR_ALL = METRIC_NAMESPACE + "/{0}/all";// Datastore/MongoDB/all
    private static final String VENDOR_ALL_WEB = METRIC_NAMESPACE + "/{0}/allWeb";// Datastore/Oracle/allWeb
    private static final String VENDOR_ALL_OTHER = METRIC_NAMESPACE + "/{0}/allOther";// Datastore/Postgres/allOther

    public static final String STATEMENT_METRIC = METRIC_NAMESPACE + "/statement/{0}/{1}/{2}";// Datastore/statement/Cassandra/users/update
    public static final String OPERATION_METRIC = METRIC_NAMESPACE + "/operation/{0}/{1}";// Datastore/operation/MySQL/remove

    // INSTANCE_METRIC + INSTANCE_ID = Datastore/instance/MySQL/hans/27017
    public static final String INSTANCE_METRIC_BASE = METRIC_NAMESPACE + "/instance/{0}/";
    public static final String DATASTORE_INSTANCE = METRIC_NAMESPACE + "/instance/";
    public static final String INSTANCE_ID = "{0}/{1}"; // hans/27107

    /**
     * What to use when you can't get the operation
     */
    public static final String DEFAULT_OPERATION = "other";
    /**
     * What to use when you can't get the table
     */
    public static final String DEFAULT_TABLE = "other";

    /**
     * An attribute on a slow query (if present) to denote the actual instance the datastore call was made to
     */
    public static final String INSTANCE_ATTRIBUTE = "instance";

    /**
     * An attribute on a slow query (if present) that records the input query that generated the raw slow query.
     */
    public static final String INPUT_QUERY_ATTRIBUTE = "input_query";

    /**
     * Used as a value in the input_query attribute above
     */
    public static final String INPUT_QUERY_LABEL_PARAMETER = "label";

    /**
     * Used as a value in the input_query attribute above
     */
    public static final String INPUT_QUERY_QUERY_PARAMETER = "query";

    public static final String DATASTORE_HOST = "host";
    public static final String DATASTORE_PORT_PATH_OR_ID = "port_path_or_id";
    public static final String DB_INSTANCE = "db.instance";

    public static String HOSTNAME = Hostname.getHostname(ServiceFactory.getConfigService().getDefaultAgentConfig());

    public static final String UNKNOWN = "unknown";

    public static void collectDatastoreMetrics(String datastoreVendor, Transaction tx, TracedMethod method,
                                               String table, String operation, String host, Integer port, String identifier,
                                               String databaseName) {
        // The scoped metric will also generate an unscoped metric with the same name. See StatsEngineImpl
        if (null == table) {
            method.setMetricName(new StringBuilder(OPERATION).append(datastoreVendor).append(SLASH).append(operation)
                                                             .toString());
        } else {

            method.addRollupMetricName(new StringBuilder(OPERATION).append(datastoreVendor).append(SLASH)
                                                                   .append(operation).toString());
            method.setMetricName(new StringBuilder(STATEMENT).append(datastoreVendor).append(SLASH).append(table)
                                                             .append(SLASH).append(operation).toString());
        }

        recordDatastoreSupportabilityMetrics(datastoreVendor, host, port, identifier, databaseName);

        boolean allUnknown = host == null && port == null && identifier == null;
        if (ServiceFactory.getConfigService().getDefaultAgentConfig().getDatastoreConfig().isInstanceReportingEnabled()
                && !allUnknown) {
            method.addRollupMetricName(buildInstanceMetric(datastoreVendor, host, port, identifier));
        }

        method.addRollupMetricName(ALL);
        method.addRollupMetricName(new StringBuilder(DATASTORE).append(datastoreVendor).append(SLASH_ALL).toString());
        if (tx.isWebTransaction()) {
            method.addRollupMetricName(ALL_WEB);
            method.addRollupMetricName(new StringBuilder(DATASTORE).append(datastoreVendor).append(SLASH_ALL_WEB)
                                                                   .toString());
        } else {
            method.addRollupMetricName(ALL_OTHER);
            method.addRollupMetricName(new StringBuilder(DATASTORE).append(datastoreVendor).append(SLASH_ALL_OTHER)
                                                                   .toString());
        }
    }

    private static void recordDatastoreSupportabilityMetrics(String vendor, String host, Integer port,
            String identifier, String databaseName) {
        StatsService statsService = ServiceFactory.getStatsService();

        if (host == null) {
            String unknownHostMetric = new StringBuilder(MetricNames.SUPPORTABILITY_DATASTORE_PREFIX)
                    .append(vendor).append(MetricNames.SUPPORTABILITY_DATASTORE_UNKNOWN_HOST).toString();
            statsService.doStatsWork(StatsWorks.getIncrementCounterWork(unknownHostMetric, 1));
        }

        if (port == null && identifier == null) {
            String unknownPortMetric = new StringBuilder(MetricNames.SUPPORTABILITY_DATASTORE_PREFIX)
                    .append(vendor).append(MetricNames.SUPPORTABILITY_DATASTORE_UNKNOWN_PORT).toString();
            statsService.doStatsWork(StatsWorks.getIncrementCounterWork(unknownPortMetric, 1));
        }

        if (databaseName == null) {
            String unknownDatabaseNameMetric = new StringBuilder(MetricNames.SUPPORTABILITY_DATASTORE_PREFIX)
                    .append(vendor).append(MetricNames.SUPPORTABILITY_DATASTORE_UNKNOWN_DATABASE_NAME).toString();
            statsService.doStatsWork(StatsWorks.getIncrementCounterWork(unknownDatabaseNameMetric, 1));
        }
    }

    public static String replaceIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return UNKNOWN;
        }
        return identifier;
    }

    public static String replacePort(Integer port) {
        if (port == null || port == -1) {
            return UNKNOWN;
        }
        return String.valueOf(port);
    }

    public static String replaceLocalhost(String host) {
        if (host == null || host.isEmpty()) {
            return UNKNOWN;
        }

        if ("localhost".equals(host) || "127.0.0.1".equals(host) || "0.0.0.0".equals(host)
                || "0:0:0:0:0:0:0:1".equals(host) || "::1".equals(host) || "0:0:0:0:0:0:0:0".equals(host)
                || "::".equals(host)) {
            return HOSTNAME;
        }

        return host;
    }

    /**
     * Send a supportability metric for unparsed queries.
     */
    public static void unparsedQuerySupportability(String datastoreVendor) {
        NewRelic.incrementCounter(new StringBuilder(MetricNames.SUPPORTABILITY_DATASTORE_PREFIX)
                .append(datastoreVendor).append(MetricNames.SUPPORTABILITY_DATASTORE_UNPARSED_QUERY).toString());
    }

    public static String buildInstanceIdentifier(String host, Integer port, String id) {
        String hostname = replaceLocalhost(host);
        String identifier = null;

        // According to the cross agent tests, if this is a filesystem database, and database host is not known, report
        // hostname
        if (id != null) {
            identifier = replaceIdentifier(id);
            if (host == null || host.isEmpty()) {
                hostname = HOSTNAME;
            }
        } else {
            identifier = replacePort(port);
        }

        return new StringBuilder(hostname).append(SLASH).append(identifier).toString();
    }

    public static String buildInstanceMetric(String dbVendor, String host, Integer port, String id) {
        String instanceId = buildInstanceIdentifier(host, port, id);
        return new StringBuilder(DATASTORE_INSTANCE).append(dbVendor).append(SLASH).append(instanceId).toString();
    }

    /**
     * This is for testing only!
     */
    public static void setHostname(String hostname) {
        HOSTNAME = hostname;
    }

    public static String getIdentifierOrPort(Integer port, String identifier) {
        if (identifier != null) {
            return replaceIdentifier(identifier);
        }
        return replacePort(port);
    }
}
