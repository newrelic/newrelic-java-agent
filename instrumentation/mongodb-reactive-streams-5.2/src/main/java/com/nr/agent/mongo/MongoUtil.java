/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.mongo;

import com.mongodb.ServerAddress;
import com.mongodb.client.model.WriteModel;
import com.mongodb.connection.ClusterDescription;
import com.mongodb.connection.ServerDescription;
import com.mongodb.internal.async.SingleResultCallback;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MongoUtil {

    private static ConcurrentHashMap<String, String> mongoDatabaseToHostMap = new ConcurrentHashMap<>(3);

    public static final String OP_FIND = "find";
    public static final String OP_AGGREGATE = "aggregate";
    public static final String OP_CREATE_INDEX = "createIndex";
    public static final String OP_CREATE_INDEXES = "createIndexes";

    public static final String OP_RENAME_COLLECTION = "renameCollection";
    public static final String OP_FIND_AND_UPDATE = "findAndUpdate";
    public static final String OP_FIND_ONE_AND_UPDATE = "findOneAndUpdate";

    public static final String OP_FIND_AND_REPLACE = "findAndReplace";
    public static final String OP_FIND_ONE_AND_REPLACE = "findOneAndReplace";

    public static final String OP_FIND_AND_DELETE = "findAndDelete";
    public static final String OP_FIND_ONE_AND_DELETE = "findOneAndDelete";

    public static final String OP_DROP_INDEX = "dropIndex";
    public static final String OP_DROP_INDEXES = "dropIndexes";
    public static final String OP_DROP_COLLECTION = "drop";
    public static final String OP_DROP_DATABASE = "dropDatabase";
    public static final String OP_DISTINCT = "distinct";
    public static final String OP_COUNT = "count";
    public static final String OP_MAP_REDUCE = "mapReduce";
    public static final String OP_REPLACE_ONE = "replaceOne";
    public static final String OP_LIST_INDEXES = "listIndexes";
    public static final String OP_BULK_WRITE = "bulkWrite";
    public static final String OP_INSERT_ONE = "insertOne";
    public static final String OP_INSERT_MANY = "insertMany";
    public static final String OP_UPDATE_MANY = "updateMany";
    public static final String OP_UPDATE_ONE = "updateOne";

    public static final String OP_DELETE_ONE = "deleteOne";
    public static final String OP_DELETE_MANY = "deleteMany";
    public static final String CUSTOM = "Custom";

    public static final String OP_DEFAULT = "other";

    public static final String UNKNOWN = "unknown";
    public static final String CLUSTER = "Cluster";
    public static final int DEFAULT_PORT = 27017;

    public static void addDatabaseAndHostToMap(final String databaseName, final String host) {
        NewRelic.getAgent().getLogger().log(Level.FINE, "Adding mongo DB with with host to map: {0} --> {1}", databaseName, host);
        if (databaseName != null && host != null) {
            if (mongoDatabaseToHostMap.containsKey(databaseName)) {
                mongoDatabaseToHostMap.replace(databaseName, host);
            } else {
                mongoDatabaseToHostMap.put(databaseName, host);
            }
        }
    }

    public static String getHostBasedOnDatabaseName(String databaseName) {
        return mongoDatabaseToHostMap.getOrDefault(databaseName, UNKNOWN);
    }


    public static String determineHostDisplayValueFromCluster(ClusterDescription clusterDescription) {
        String hostDescription = UNKNOWN;
        List<ServerDescription> serverDescriptions = clusterDescription.getServerDescriptions();

        if (serverDescriptions != null) {
            ServerAddress serverAddress = serverDescriptions.get(0).getAddress();
            hostDescription = serverDescriptions.size() == 1 ?  serverAddress.getHost() + ":" + serverAddress.getPort() : CLUSTER;
        }

        return hostDescription;
    }

    public static <T> SingleResultCallback<T> instrumentSingleResultCallback(SingleResultCallback<T> callback, String collectionName,
            String operationName, String databaseName, String host) {
        if (callback instanceof NRCallbackWrapper) {
            return callback;
        }

        String hostname = null;
        int port = DEFAULT_PORT;
        String [] hostAndPort = host.split(":");
        try {
            hostname = hostAndPort[0];
            if (hostAndPort.length == 2) {
                port = Integer.parseInt(hostAndPort[1]);
            }
        } catch (NumberFormatException ignored) {
            //Guard rail for parse exception
        }

        NRCallbackWrapper<T> wrapper = new NRCallbackWrapper<T>(callback);
        wrapper.params = DatastoreParameters
                .product(DatastoreVendor.MongoDB.name())
                .collection(collectionName)
                .operation(operationName)
                .instance(hostname, port)
                .databaseName(databaseName)
                .build();

        wrapper.token = NewRelic.getAgent().getTransaction().getToken();
        wrapper.segment = NewRelic.getAgent().getTransaction().startSegment(operationName);
        return wrapper;
    }

    public static String determineBulkWriteOperation(WriteModel writeModel) {
        String operationName = OP_DEFAULT;

        if (writeModel != null) {
            String [] tokens = writeModel.getClass().toString().split("\\.");
            String classNameOnly = tokens[tokens.length - 1];
            switch (classNameOnly) {
                case "DeleteManyModel":
                    operationName = OP_DELETE_MANY;
                    break;
                case "DeleteOneModel":
                    operationName = OP_DELETE_ONE;
                    break;
                case "InsertOneModel":
                    operationName = OP_INSERT_ONE;
                    break;
                case "ReplaceOneModel":
                    operationName = OP_REPLACE_ONE;
                    break;
                case "UpdateManyModel":
                    operationName = OP_UPDATE_MANY;
                    break;
                case "UpdateOneModel":
                    operationName = OP_UPDATE_ONE;
                    break;
            }
        }

        return operationName;
    }
}
