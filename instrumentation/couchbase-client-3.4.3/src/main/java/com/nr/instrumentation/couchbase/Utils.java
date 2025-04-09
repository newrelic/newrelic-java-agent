package com.nr.instrumentation.couchbase;

import java.util.Optional;

import com.couchbase.client.core.io.CollectionIdentifier;
import com.couchbase.client.java.AsyncCollection_Instrumentation;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.database.DefaultDatabaseStatementParser;
import com.newrelic.agent.database.ParsedDatabaseStatement;
import com.newrelic.agent.service.ServiceFactory;

public class Utils {

    public static boolean initialized = false;

    private static DefaultDatabaseStatementParser parser = null;

    private static CouchbaseVendor vendor = new CouchbaseVendor();
    
    private static final String CONFIGPREFIX = "Couchbase.include.";
    
    
    private static boolean includeBucket = true;
    private static boolean includeScope = false;
    private static boolean includeCollection = true;

    public static void init() {
        DatabaseService dbService = ServiceFactory.getDatabaseService();
        if(dbService != null) {
            parser = (DefaultDatabaseStatementParser) dbService.getDatabaseStatementParser();
            initialized = true;
        }
        
        AgentConfig config = ServiceFactory.getConfigService().getLocalAgentConfig();
        Boolean b = config.getProperty(CONFIGPREFIX+"bucket", Boolean.TRUE);
        if(b != null && b != includeBucket) {
            includeBucket = b;
        }
        
        b = config.getProperty(CONFIGPREFIX+"scope", Boolean.FALSE);
        if(b != null && b != includeScope) {
            includeScope = b;
        }
        
        b = config.getProperty(CONFIGPREFIX+"collection", Boolean.TRUE);
        if(b != null && b != includeCollection) {
            includeCollection = b;
        }
        
    }

    public static ParsedDatabaseStatement parseSQL(String sql) {
        if(!initialized) {
            init();
        }

        if(initialized) {
            ParsedDatabaseStatement statement = parser.getParsedDatabaseStatement(vendor, sql, null);

            return statement;
        }

        return null;
    }

    public static String getName(CollectionIdentifier identifier) {
        StringBuffer sb = new StringBuffer();
        String scope;
        if (includeBucket) {
            String bucket = identifier.bucket();
            if (bucket != null && !bucket.isEmpty()) {
                sb.append(bucket);
            }
            scope = null;
        }

        if (includeScope) {
            Optional<String> scp = identifier.scope();
            if (scp.isPresent()) {
                scope = scp.get();
                if (scope != null && !scope.isEmpty()) {
                    if (sb.length() != 0) {
                        sb.append('-');
                    }
                    sb.append(scope);
                }
            } 
        }
        
        if (includeCollection) {
            String collection = null;
            Optional<String> coll = identifier.collection();
            if (coll.isPresent()) {
                collection = coll.get();
                if (collection != null && !collection.isEmpty()) {
                    if (sb.length() != 0) {
                        sb.append('-');
                    }
                    sb.append(collection);
                }
            } 
        }
        String name = sb.toString();
        return !name.isEmpty() ? name : "Unknown";
    }

    public static String getName(AsyncCollection_Instrumentation coll) {
        StringBuffer sb = new StringBuffer();
        if (includeBucket) {
            String bucket = coll.bucketName();
            if (bucket != null && !bucket.isEmpty()) {

                sb.append(bucket);
            } 
        }
        
        if (includeScope) {
            String scope = coll.scopeName();
            if (scope != null && !scope.isEmpty()) {
                if (sb.length() != 0) {
                    sb.append('-');
                }
                sb.append(scope);
            } 
        }
        
        if (includeCollection) {
            String collection = coll.name();
            if (collection != null && !collection.isEmpty()) {
                if (sb.length() != 0) {
                    sb.append('-');
                }
                sb.append(collection);
            } 
        }
        
        String name = sb.toString();
        return !name.isEmpty() ? name : "Unknown";
    }
}
