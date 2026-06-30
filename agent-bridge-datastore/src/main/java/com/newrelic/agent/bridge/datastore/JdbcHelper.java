/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    //statement cache settings
    /**
     * In extreme high-throughput scenarios, weak keyed eviction may throttle the CPU when it tries to perform maintenance on statement caches.
     * Setting -Dnewrelic.config.jdbc_statement_weak_key_caching.enabled=false will convert the caches to ordinary ConcurrentHashMaps
     * to alleviate this maintenance overhead.
     * <p>
     * Statements are only removed from the caches on Statement.close(), so this config MUST be enabled with caution. If the user does not properly close their
     * statements, setting -Dnewrelic.config.jdbc_statement_weak_key_caching.enabled=false will cause memory issues.
     */
    private static final String JDBC_STATEMENT_WEAK_KEY_CACHING_ENABLED = "jdbc_statement_weak_key_caching.enabled";
    private static final boolean JDBC_STATEMENT_WEAK_KEY_CACHING_ENABLED_DEFAULT = Boolean.TRUE;
    private static final int INITIAL_STATEMENT_CACHE_CAPACITY = 1024;
    private static final Map<Statement, Object[]> statementToParams = initStatementCache("statementToParams");
    private static final Map<Statement, String> statementToSql = initStatementCache("statementToSql");

    // This will contain every vendor type that we detected on the client system
    private static final Map<String, DatabaseVendor> typeToVendorLookup = new ConcurrentHashMap<>(10);
    private static final Map<Class<?>, DatabaseVendor> classToVendorLookup = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
    private static final Map<Connection, String> connectionToIdentifier = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
    private static final Map<Connection, String> connectionToURL = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
    public static final String UNKNOWN = "unknown";

    private static final int cacheExpireTime = NewRelic.getAgent().getConfig().getValue("jdbc_helper_cache_expire_time", 7200);
    private static final Map<String, ConnectionFactory> urlToFactory = AgentBridge.collectionFactory.createConcurrentTimeBasedEvictionMap(cacheExpireTime);
    private static final Map<String, String> urlToDatabaseName = AgentBridge.collectionFactory.createConcurrentTimeBasedEvictionMap(cacheExpireTime);

    public static final String SQL_METADATA_COMMENTS_SVC_GUID = "nr_service_guid";

    private static volatile Boolean isSqlMetadataCommentsEnabled = null;
    private static volatile String cachedServiceGuid = null;

    public static Object[] getParams(Statement statement) {
        return statementToParams.get(statement);
    }

    public static void putParams(Statement statement, Object[] params) {
        statementToParams.put(statement, params);
    }

    public static String getSql(Statement statement) {
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Getting sql for statement: {0}", statement);
        return statementToSql.get(statement);
    }

    public static void putSql(Statement statement, String sql) {
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Storing sql for statement: {0}", statement);
        statementToSql.put(statement, sql);
    }

    public static void removeStatement(Statement statement) {
        if (statement != null) {
            statementToParams.remove(statement);
            statementToSql.remove(statement);
        }
    }

    public static void putVendor(Class<?> driverOrDatastoreClass, DatabaseVendor databaseVendor) {
        classToVendorLookup.put(driverOrDatastoreClass, databaseVendor);
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Storing class: {0}, vendor: {1}", driverOrDatastoreClass, databaseVendor);

        typeToVendorLookup.put(databaseVendor.getType(), databaseVendor);
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Storing type: {0}, vendor: {1}", databaseVendor.getType(), databaseVendor);
    }

    public static DatabaseVendor getVendor(Class<?> driverOrDatastoreClass, String url) {
        DatabaseVendor vendor = classToVendorLookup.get(driverOrDatastoreClass);
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Getting class: {0}, url: {1}, vendor: {2}", driverOrDatastoreClass, url, vendor);

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
     * {@link JdbcHelper#UNKNOWN} otherwise.
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
        isSqlMetadataCommentsEnabled = null;
    }

    /**
     * Add the SQL metadata comment to the target SQL, if necessary. If
     * the comment has already been added to this statement, simply return
     * the original SQL statement.
     *
     * @param sql the target SQL statement
     * @return a SQL statement which might have the metadata comment prepended to it
     */
    public static String addSqlMetadataCommentIfNeeded(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }

        Boolean isEnabled = isSqlMetadataCommentsEnabled();
        if (isEnabled) {
            // Check if comment already exists
            if (sql.startsWith("/*nr_")) {
                return sql;
            }

            String comment = generateSqlMetadataComment();
            if (comment.isEmpty()) {
                return sql;
            } else {
                return comment + sql;
            }
        }

        return sql;
    }

    /**
     * If a transaction is in progress and isSqlMetadataCommentsEnabled is true, create a comment to be prepended to the
     * statement that contains the service GUID
     *
     * @return the SQL metadata comment if a transaction is in progress, an empty String otherwise
     */
    private static String generateSqlMetadataComment() {
        // The check of isSqlMetadataCommentsEnabled happens in the addSqlMetadataCommentIfNeeded method
        // which gates the execution of this method
        StringBuilder comment = new StringBuilder(64);
        String guid = getEntityGuid();

        // Prevent SQL comment injection if GUID contains comment-closing sequence
        if (guid != null && guid.contains("*/")) {
            return "";
        }

        if ((guid != null) && (!guid.isEmpty())) {
            comment.append("/*");
            comment.append(SQL_METADATA_COMMENTS_SVC_GUID).append("=\"").append(guid).append("\"");
        }

        // Only return comment if metadata was added
        if (comment.length() > 0) {
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
    private static Boolean isSqlMetadataCommentsEnabled() {
        if (isSqlMetadataCommentsEnabled == null) {
            synchronized (JdbcHelper.class) {
                if (isSqlMetadataCommentsEnabled == null) {
                    try {
                        Boolean isEnabled = NewRelic.getAgent().getConfig().getValue("transaction_tracer.sql_metadata_comments.enabled");
                        // Only cache if we got a non-null value (config service is ready)
                        if (isEnabled != null) {
                            isSqlMetadataCommentsEnabled = isEnabled;
                        }
                    } catch (Exception ignored) {
                        // Config service not ready yet, will retry on next call
                    }
                }
            }
        }
        return (isSqlMetadataCommentsEnabled != null) ? isSqlMetadataCommentsEnabled : Boolean.FALSE;
    }

    /**
     * Checks the config to see whether weak key caching is enabled.
     * <p>
     * If weak key caching is enabled (default) a Caffeine-backed Weak Keyed cache will be returned.
     * If weak key caching is not enabled a vanilla Concurrent Hash Map will be returned.
     */
    static <V> Map<Statement, V> initStatementCache(String cacheName) {
        boolean weakKeyCachingEnabled = NewRelic.getAgent()
                .getConfig()
                .getValue(JDBC_STATEMENT_WEAK_KEY_CACHING_ENABLED, JDBC_STATEMENT_WEAK_KEY_CACHING_ENABLED_DEFAULT);
        if (weakKeyCachingEnabled) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "JDBC Statement weak key caching is enabled. Using default Weak Keyed Cache for {0}.", cacheName);
            return AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
        } else {
            NewRelic.getAgent()
                    .getLogger()
                    .log(Level.INFO,
                            "JDBC Statement weak key caching is disabled. Using a ConcurrentHashMap for {0}. All JDBC Statements MUST be closed when this setting is enabled.",
                            cacheName);
            return AgentBridge.collectionFactory.createVanillaJavaConcurrentHashMap(INITIAL_STATEMENT_CACHE_CAPACITY);
        }
    }

    /**
     * Retrieves the cached entity GUID value and initialize the value on first access.
     * If the entity GUID is not yet set (empty string), this method will retry on subsequent
     * calls until a valid value is obtained.
     */
    static String getEntityGuid() {
        // Check for both null and empty string - empty string indicates the RPM service
        // hasn't connected yet, and we should retry on the next call
        if (cachedServiceGuid == null || cachedServiceGuid.isEmpty()) {
            synchronized (JdbcHelper.class) {
                if (cachedServiceGuid == null || cachedServiceGuid.isEmpty()) {
                    cachedServiceGuid = AgentBridge.getAgent().getEntityGuid(false);
                }
            }
        }

        return cachedServiceGuid;
    }

    /**
     * Resets the cached entity GUID. Only for testing purposes.
     */
    static void resetEntityGuidCache() {
        synchronized (JdbcHelper.class) {
            cachedServiceGuid = null;
        }
    }
}