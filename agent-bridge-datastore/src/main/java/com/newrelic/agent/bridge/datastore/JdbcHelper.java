/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.time.Duration;
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

    /**
     * Any values in the urlToFactory or urlToDatabaseName Maps older than this value will
     * be evicted from the cache when the {@link ExpiringValueMap} timer fires. This
     * will also be used as the timer interval value.
     */
    private static final long CACHE_EXPIRATION_AGE_MILLI = Duration.ofHours(2).toMillis();

    private static final ExpiringValueMap.ExpiringValueLogicFunction EXPIRE_FUNC =
            (timeCreated, timeLastAccessed) -> timeLastAccessed < (System.currentTimeMillis() - CACHE_EXPIRATION_AGE_MILLI);

    // This will contain every vendor type that we detected on the client system
    private static final Map<String, DatabaseVendor> typeToVendorLookup = new ConcurrentHashMap<>(10);
    private static final Map<Class<?>, DatabaseVendor> classToVendorLookup = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
    private static final Map<Statement, String> statementToSql = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
    private static final Map<Connection, String> connectionToIdentifier = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
    private static final Map<Connection, String> connectionToURL = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
    public static final String UNKNOWN = "unknown";

    // Config to toggle the use of the ExpiringValueCache Map implementation or vanilla ConcurrentHashMap implementation
    // for the urlToFactory and urlToDatabaseName maps. The default is to use the ExpiringValueCache. Set a config
    // property of use_jdbchelper_vanilla_map=true to revert to the ConcurrentHashMap implementation.
    private static final Map<String, ConnectionFactory> urlToFactory;
    private static final Map<String, String> urlToDatabaseName;
    static {
        boolean useVanillaMap = NewRelic.getAgent().getConfig().getValue("use_jdbchelper_vanilla_map", false);
        if (useVanillaMap) {
            urlToFactory = new ConcurrentHashMap<>(10);
            urlToDatabaseName = new ConcurrentHashMap<>(10);
        } else {
            urlToFactory =
                    new ExpiringValueMap<>("urlToFactoryCache", CACHE_EXPIRATION_AGE_MILLI, EXPIRE_FUNC);
            urlToDatabaseName =
                    new ExpiringValueMap<>("urlToDatabaseNameCache", CACHE_EXPIRATION_AGE_MILLI, EXPIRE_FUNC);
        }
        NewRelic.getAgent().getLogger().log(Level.FINEST, "JdbcHelper using map implementation: {0} " +
                "for urlToFactory and urlToDatabaseName maps", useVanillaMap ? "ConcurrentHashMap" : "ExpiringValueMap");
    }

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
}
