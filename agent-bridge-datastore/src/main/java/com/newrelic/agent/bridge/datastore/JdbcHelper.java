/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JdbcHelper {

    private static final Pattern VENDOR_PATTERN = Pattern.compile("jdbc:([^:]*).*");

    private static final Pattern IN_MEMORY_PATTERN = Pattern.compile("jdbc:(?:[^:]*):" +
            "(?:memory|file|directory|res|mem)?:?" + "(?!.*?//.+:?\\d?)" + "([^;,?]+[^;,?:])");

    private static final Set<String> KNOWN_IN_MEMORY_DRIVERS = new HashSet<>(Arrays.asList("h2", "hsqldb",
            "derby", "sqlite"));

    private static final ThreadLocal<Boolean> connectionLookup = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    // This will contain every vendor type that we detected on the client system
    private static final Map<String, DatabaseVendor> typeToVendorLookup = new ConcurrentHashMap<>(10);
    private static final Map<Class<?>, DatabaseVendor> classToVendorLookup = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
    private static final Map<Statement, String> statementToSql = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
    private static final Map<Connection, String> connectionToIdentifier = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
    private static final Map<Connection, String> connectionToURL = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
    public static final String UNKNOWN = "unknown";

    private static final int cacheExpireTime = NewRelic.getAgent().getConfig().getValue("jdbc_helper_cache_expire_time", 7200);
    private static volatile Set<String> metadataCommentConfig = null;
    private static final Map<String, ConnectionFactory> urlToFactory = AgentBridge.collectionFactory.createConcurrentTimeBasedEvictionMap(cacheExpireTime);
    private static final Map<String, String> urlToDatabaseName = AgentBridge.collectionFactory.createConcurrentTimeBasedEvictionMap(cacheExpireTime);

    public static final String SQL_METADATA_COMMENTS_SVC_GUID = "nr_service_guid";
    public static final String SQL_METADATA_COMMENTS_SVC_NAME = "nr_service";
    public static final String SQL_METADATA_COMMENTS_TXN_NAME = "nr_txn";
    public static final String SQL_METADATA_COMMENTS_TRACE_ID = "nr_trace_id";
    private static final Set<String> VALID_SQL_METADATA_COMMENTS_OPTIONS = new HashSet<>(Arrays.asList(
            SQL_METADATA_COMMENTS_SVC_GUID,
            SQL_METADATA_COMMENTS_SVC_NAME,
            SQL_METADATA_COMMENTS_TXN_NAME,
            SQL_METADATA_COMMENTS_TRACE_ID
    ));

    private static volatile String cachedAppName = null;
    private static volatile String cachedEntityGuid = null;

    public static void putVendor(Class<?> driverOrDatastoreClass, DatabaseVendor databaseVendor) {
        classToVendorLookup.put(driverOrDatastoreClass, databaseVendor);
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Storing class: {0}, vendor: {1}", driverOrDatastoreClass, databaseVendor);

        typeToVendorLookup.put(databaseVendor.getType(), databaseVendor);
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Storing type: {0}, vendor: {1}", databaseVendor.getType(), databaseVendor);
    }

    public static DatabaseVendor getVendor(Class<?> driverOrDatastoreClass, String url) {
        DatabaseVendor vendor = classToVendorLookup.get(driverOrDatastoreClass);
        AgentBridge.getAgent().getLogger().log(Level.FINEST,"Getting class: {0}, url: {1}, vendor: {2}", driverOrDatastoreClass, url, vendor);

        if (vendor != null) {
            return vendor;
        }

        if (url != null) {
            Matcher matcher = VENDOR_PATTERN.matcher(url);
            if (matcher.matches()) {
                String type = matcher.group(1);
                if (type != null) {
                    AgentBridge.getAgent().getLogger().log(Level.FINEST, "Found type: {0}", type);
                    vendor = typeToVendorLookup.get(type);
                    if (vendor != null) {
                        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Found vendor: {0}", vendor);
                        return vendor;
                    }
                }
            }
        }

        AgentBridge.getAgent().getLogger().log(Level.FINEST, "No match found for vendor");
        return UnknownDatabaseVendor.INSTANCE;
    }

    public static boolean connectionFactoryExists(Connection connection) {
        if (connection != null) {
            String url = getConnectionURL(connection);
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "Found connection: {0}, url: {1}", connection, url);
            return url != null && urlToFactory.containsKey(url);
        }

        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Connection was null");
        return false;
    }

    public static void putConnectionFactory(String url, ConnectionFactory connectionFactory) {
        if (url == null) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "Can't store null url with connection factory");
            return;
        }

        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Storing url: {0} with connection factory: {1}", url, connectionFactory);
        urlToFactory.put(url, connectionFactory);
    }

    public static ConnectionFactory getConnectionFactory(Connection connection) {
        String url = getConnectionURL(connection);
        if (url != null) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "Getting connection factory for url: {0}, connection: {1}", url, connection);
            return urlToFactory.get(url);
        }

        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Connection factory url was null for connection: {0}", connection);
        return null;
    }

    public static boolean databaseNameExists(Connection connection) {
        if (connection != null) {
            String url = getConnectionURL(connection);
            return url != null && urlToDatabaseName.containsKey(url);
        }
        return false;
    }

    public static void putDatabaseName(String url, String databaseName) {
        if (url == null) {
            return;
        }

        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Storing database name: {0}", databaseName);
        urlToDatabaseName.put(url, databaseName);
    }

    public static String getCachedDatabaseName(Connection connection) {
        String url = getConnectionURL(connection);
        if (url != null) {
            return urlToDatabaseName.get(url);
        }
        return null;
    }

    public static void putSql(Statement statement, String sql) {
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Storing sql for statement: {0}", statement);
        statementToSql.put(statement, sql);
    }

    public static String getSql(Statement statement) {
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Getting sql for statement: {0}", statement);
        return statementToSql.get(statement);
    }

    public static Object[] growParameterArray(Object[] params, int missingIndex) {
        int length = Math.max(10, (int) (missingIndex * 1.2));
        Object[] newParams = new Object[length];
        System.arraycopy(params, 0, newParams, 0, params.length);
        return newParams;
    }

    /**
     * @param connection
     * @return connection URL String or null
     */
    public static String getConnectionURL(Connection connection) {
        if (connection != null) {

            final String cachedURL = connectionToURL.get(connection);
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "Cached url: {0} for connection: {1}", cachedURL, connection);

            // connectionToURL map does not support null values
            // We use UNKNOWN to remember null connection URL strings
            if (UNKNOWN.equals(cachedURL)) {
                return null;
            }

            Boolean metadataEnabled = AgentBridge.getAgent().getConfig().getValue("datastore_tracer.database_connection_metadata.enabled", true);
            if (!metadataEnabled) {
                AgentBridge.getAgent().getLogger().log(Level.FINE, "Unable to get connection url: connection_metadata config is disabled.");
                connectionToURL.put(connection, UNKNOWN);
                return null;
            }

            if (cachedURL != null) {
                return cachedURL;
            }

            try {
                if (!connectionLookup.get()) {
                    connectionLookup.set(Boolean.TRUE);
                    AgentBridge.getAgent().getLogger().log(Level.FINEST, "Getting connection metadata for connection: {0}", connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    if (metaData != null) {
                        String url = metaData.getURL();
                        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Getting url: {0} from connection metadata for connection: {1}", url, connection);
                        connectionToURL.put(connection, url == null ? UNKNOWN : url);
                        return url;
                    }
                }
            } catch (Throwable e) {
                // If any error occurs we'll return null
                AgentBridge.getAgent().getLogger().log(Level.FINER, e, "Unable to get connection url for: {0}", connection);
                connectionToURL.put(connection, UNKNOWN);
            } finally {
                connectionLookup.set(Boolean.FALSE);
            }
        }

        return null;
    }

    /**
     * Parse identifier from connection string. Only parses identifier if vendor is a supported in-memory database
     * vendor. See {@link #KNOWN_IN_MEMORY_DRIVERS}.
     *
     * @param connectionString URL connection string to parse.
     * @return identifier parsed from connection string if vendor is part of supported in-memory JDBC drivers,
     *         {@link JdbcHelper#UNKNOWN} otherwise.
     */
    public static String parseInMemoryIdentifier(String connectionString) {
        if (connectionString == null) {
            return UNKNOWN;
        }

        try {
            Matcher vendorMatcher = VENDOR_PATTERN.matcher(connectionString);
            if (!vendorMatcher.matches()) {
                return UNKNOWN;
            }

            String driverName = vendorMatcher.group(1);
            if (!KNOWN_IN_MEMORY_DRIVERS.contains(driverName)) {
                return UNKNOWN;
            }

            Matcher matcher = IN_MEMORY_PATTERN.matcher(connectionString);

            if (matcher.find()) {
                String identifier = matcher.group(1);
                AgentBridge.getAgent().getLogger().log(Level.FINEST, "Parsed database identifier: {0}", identifier);
                return identifier;
            }
            return UNKNOWN;
        } catch (Throwable t) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, t, "Exception thrown when parsing database identifier.");
        }

        return UNKNOWN;
    }



    /**
     * Parse and cache identifier of in-memory database from Connection string url.
     *
     * @return Parsed identifier. If unable to parse connection string, returns {@link #UNKNOWN}
     */
    public static String parseAndCacheInMemoryIdentifier(Connection connection) {
        if (connection == null) {
            return UNKNOWN;
        }
        String identifier = parseInMemoryIdentifier(getConnectionURL(connection));
        identifier = identifier == null ? UNKNOWN : identifier;
        connectionToIdentifier.put(connection, identifier);
        return identifier;
    }

    /**
     * @return cached identifier for a connection, may return null.
     */
    public static String getCachedIdentifierForConnection(Connection connection) {
        if (connection == null) {
            return null;
        } else {
            String identifier = connectionToIdentifier.get(connection);
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "Identifier for connection: {0} is: {1}", connection, identifier);
            return identifier;
        }
    }

    public static String getDatabaseName(Connection connection) {
        try {
            if (connection == null) {
                return UNKNOWN;
            }
            String databaseName = getCachedDatabaseName(connection);
            if (databaseName == null) {
                databaseName = connection.getCatalog();
                if (databaseName != null) {
                    putDatabaseName(getConnectionURL(connection), databaseName);
                }
            }
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "Returning database name: {0} for connection: {1}", databaseName, connection);
            return databaseName;
        } catch (Throwable t) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, t, "Unable to get database name for connection: {0}", connection);
            return UNKNOWN;
        }
    }

    /**
     * This can be called to invalidate the configuration for metadata comments that
     * are added to all executed SQL. Specifically, it's called from
     * com.newrelic.agent.database.DatabaseService (agent project) when the
     * AgentConfigListener.configChanged method is called. The JdbcHelper class
     * is unable to implement AgentConfigListener since those classes aren't
     * visible to the bridge projects.
     */
    public static void invalidateMetadataCommentConfig() {
        metadataCommentConfig = null;
    }

    /**
     * Add the SQL metadata comment to the target SQL, if necessary. If
     * the comment has already been added to this statement, simply return
     * the original SQL statement.
     *
     * @param sql the target SQL statement
     *
     * @return a SQL statement which might have the metadata comment prepended to it
     */
    public static String addSqlMetadataCommentIfNeeded(String sql) {
        AgentBridge.getAgent().getLogger().log(Level.INFO, "MSSQL: in addSqlMetadataCommentIfNeeded0: " + (sql == null ? "[null]" : sql));
        if (sql == null || sql.isEmpty()) {
            return sql;
        }

        Set<String> config = getMetadataCommentConfig();
        AgentBridge.getAgent().getLogger().log(Level.INFO, "MSSQL: in addSqlMetadataCommentIfNeeded1: " + config);
        if (!config.isEmpty()) {
            // Check if comment already exists
            if (sql.startsWith("/*nr_")) {
                return sql;
            }

            String comment = generateSqlMetadataComment(config);
            AgentBridge.getAgent().getLogger().log(Level.INFO, "MSSQL: in addSqlMetadataCommentIfNeeded2: " + comment);
            if (comment.isEmpty()) {
                return sql;
            } else {
                return comment + sql;
            }
        }

        return sql;
    }

    /**
     * If a transaction is in progress, create a comment to be prepended to the statement that contains the
     * attributes specified by the configuration.
     *
     * @return the SQL metadata comment if a transaction is in progress, an empty String otherwise
     */
    private static String generateSqlMetadataComment(Set<String> metadataCommentConfig) {
        com.newrelic.api.agent.Transaction transaction = NewRelic.getAgent().getTransaction();
        if (transaction == NoOpTransaction.INSTANCE) {
            return "";
        }

        boolean attributeAdded = false;
        StringBuilder comment = new StringBuilder(64);
        comment.append("/*");

        if (metadataCommentConfig.contains(SQL_METADATA_COMMENTS_SVC_NAME)) {
            String appName = getAppName();
            if (!appName.contains("*/")) {
                comment.append(SQL_METADATA_COMMENTS_SVC_NAME).append("=\"").append(appName).append("\"");
                attributeAdded = true;
            }
        }

        if (metadataCommentConfig.contains(SQL_METADATA_COMMENTS_SVC_GUID)) {
            String guid = getEntityGuid();
            if ((guid != null) && (!guid.isEmpty())) {
                comment.append(attributeAdded ? "," : "");
                comment.append(SQL_METADATA_COMMENTS_SVC_GUID).append("=\"").append(guid).append("\"");
                attributeAdded = true;
            }
        }

        if (metadataCommentConfig.contains(SQL_METADATA_COMMENTS_TXN_NAME)) {
            String txnName = transaction.getTransactionName();
            if (!txnName.contains("*/")) {
                comment.append(attributeAdded ? "," : "");
                comment.append(SQL_METADATA_COMMENTS_TXN_NAME).append("=\"").append(txnName).append("\"");
                attributeAdded = true;
            }
        }

        if (metadataCommentConfig.contains(SQL_METADATA_COMMENTS_TRACE_ID)) {
            comment.append(attributeAdded ? "," : "");
            comment.append(SQL_METADATA_COMMENTS_TRACE_ID).append("=\"").append(NewRelic.getAgent().getTraceMetadata().getTraceId()).append("\"");
        }

        // Only return comment if metadata was added
        if (comment.length() > 2) {
            comment.append("*/");

            Logger logger = AgentBridge.getAgent().getLogger();
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "Adding metadata comment to SQL statement: {0}", comment);
            }
            return comment.toString();
        }

        return "";
    }

    /**
     * This initializes the metadata comment config when the config services is actually
     * fully spun up (or the config is invalidated). If the service isn't initialized,
     * return an empty config set. If it is initialized, cache the result and return
     * it on subsequent calls.
     */
    private static Set<String> getMetadataCommentConfig() {
        if (metadataCommentConfig == null) {
            synchronized (JdbcHelper.class) {
                if (metadataCommentConfig == null) {
                    try {
                        Set<String> config = parseSqlMetadataCommentsConfig();
                        // Only cache if we got a non-null value (config service is ready)
                        if (config != null) {
                            metadataCommentConfig = config;
                        }
                    } catch (Exception ignored) {
                        // Config service not ready yet, will retry on next call
                    }
                }
            }
        }
        return (metadataCommentConfig != null) ? metadataCommentConfig : Collections.emptySet();
    }

    /**
     * Parse the sql_metadata_comments config and return a Set of valid values contained in
     * the supplied configuration String. If no valid options are present, an empty Set
     * will be returned.
     *
     * @return a Set of valid sql_metadata_comment Strings
     */
    private static Set<String> parseSqlMetadataCommentsConfig() {
        String options = NewRelic.getAgent().getConfig().getValue("transaction_tracer.sql_metadata_comments");
        if (options == null) {
            return null;
        }

        return Arrays.stream(options.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && VALID_SQL_METADATA_COMMENTS_OPTIONS.contains(s))
                .collect(Collectors.toSet());
    }

    /**
     * Retrieves the cached app_name value and initialize the value on first access.
     */
    private static String getAppName() {
        if (cachedAppName == null) {
            synchronized (JdbcHelper.class) {
                if (cachedAppName == null) {
                    cachedAppName = NewRelic.getAgent().getConfig().getValue("app_name");
                }
            }
        }

        return cachedAppName;
    }

    /**
     * Retrieves the cached entity GUID value and initialize the value on first access.
     * If the entity GUID is not yet set (empty string), this method will retry on subsequent
     * calls until a valid value is obtained.
     */
    static String getEntityGuid() {
        // Check for both null and empty string - empty string indicates the RPM service
        // hasn't connected yet and we should retry on the next call
        if (cachedEntityGuid == null || cachedEntityGuid.isEmpty()) {
            synchronized (JdbcHelper.class) {
                if (cachedEntityGuid == null || cachedEntityGuid.isEmpty()) {
                    cachedEntityGuid = AgentBridge.getAgent().getEntityGuid(false);
                }
            }
        }

        return cachedEntityGuid;
    }

    /**
     * Resets the cached entity GUID. Only for testing purposes.
     */
    static void resetEntityGuidCache() {
        synchronized (JdbcHelper.class) {
            cachedEntityGuid = null;
        }
    }
}