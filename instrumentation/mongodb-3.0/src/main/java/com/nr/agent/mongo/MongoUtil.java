/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.mongo;

public class MongoUtil {

    public static final String OP_FIND = "find";
    public static final String OP_INSERT = "insert";
    public static final String OP_UPDATE = "update";
    public static final String OP_AGGREGATE = "aggregate";
    public static final String OP_REMOVE = "remove";
    public static final String OP_PARALLEL_SCAN = "parallelCollectionScan";
    public static final String OP_CREATE_INDEX = "createIndex";

    public static final String OP_RENAME_COLLECTION = "renameCollection";
    public static final String OP_FIND_AND_UPDATE = "findAndUpdate";
    public static final String OP_FIND_AND_REPLACE = "findAndReplace";
    public static final String OP_FIND_AND_DELETE = "findAndDelete";
    public static final String OP_DROP_INDEX = "dropIndex";
    public static final String OP_DROP_COLLECTION = "drop";
    public static final String OP_DISTINCT = "distinct";
    public static final String OP_COUNT = "count";
    public static final String OP_MAP_REDUCE = "mapReduce";
    public static final String OP_REPLACE = "replace";
    public static final String OP_LIST_INDEX = "listIndex";
    public static final String OP_BULK_WRITE = "bulkWrite";
    public static final String OP_INSERT_MANY = "insertMany";
    public static final String OP_UPDATE_MANY = "updateMany";
    public static final String OP_GET_MORE = "getMore";

    // "delete" commands are different from DBCollection.remove
    public static final String OP_DELETE = "delete";

    /**
     * What to use when you can't get the operation.
     */
    public static final String DEFAULT_OPERATION = "other";

    /**
     * What to use when you can't get the collection name.
     */
    public static final String DEFAULT_COLLECTION = "other";

    public static final String OP_DEFAULT = "other";
}
