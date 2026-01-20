/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * The input parameters required to report a datastore on the {@link TracedMethod}. A fluent builder is provided to
 * allow for easy usage and management of this API.
 *
 * @since 3.36.0
 */
public class DatastoreParameters implements ExternalParameters {

    /**
     * The name of the vendor or driver.
     */
    private final String product;

    /**
     * The name of the collection or table.
     */
    private final String collection;

    /**
     * The name of the datastore operation. This should be the primitive operation type accepted by the datastore itself
     * or the name of the API method in the client library.
     */
    private final String operation;

    /**
     * The host where the datastore is located.
     */
    private final String host;

    /**
     * The port for the connection to the datastore.
     */
    private final Integer port;

    /**
     * The path or identifier of this instance.
     */
    private final String pathOrId;

    /**
     * The database name.
     */
    private final String databaseName;

    /**
     * The cloud provider's identifier for this resource. Eg. in AWS, this should be an ARN.
     */
    private final String cloudResourceId;

    private DatastoreParameters(String product, String collection, String operation, String host, Integer port,
            String pathOrId, String databaseName, String cloudResourceId) {
        this.product = product;
        this.collection = collection;
        this.operation = operation;
        this.host = host;
        this.port = port;
        this.pathOrId = pathOrId;
        this.databaseName = databaseName;
        this.cloudResourceId = cloudResourceId;
    }

    protected DatastoreParameters(DatastoreParameters datastoreParameters) {
        this.product = datastoreParameters.product;
        this.collection = datastoreParameters.collection;
        this.operation = datastoreParameters.operation;
        this.host = datastoreParameters.host;
        this.port = datastoreParameters.port;
        this.pathOrId = datastoreParameters.pathOrId;
        this.databaseName = datastoreParameters.databaseName;
        this.cloudResourceId = datastoreParameters.cloudResourceId;
    }

    protected static class Builder implements CollectionParameter, OperationParameter, InstanceParameter,
            DatabaseParameter, SlowQueryParameter, SlowQueryWithInputParameter, Build {
        private String product;
        private String collection;
        private String operation;
        private String host = null;
        private Integer port = null;
        private String pathOrId = null;
        private String databaseName = null;
        private String cloudResourceId = null;

        /**
         * Used for {@link SlowQueryDatastoreParameters}. The builder method below gives us type safety here.
         */
        private Object rawQuery = null;
        private QueryConverter<Object> queryConverter = null;
        private String normalizedSqlHash = null;

        /**
         * Used for {@link SlowQueryWithInputDatastoreParameters}. The builder method below gives us type safety here.
         */
        private String inputQueryLabel = null;
        private Object rawInputQuery = null;
        private QueryConverter<Object> rawInputQueryConverter = null;

        public Builder(String product) {
            this.product = product;
        }

        @Override
        public OperationParameter collection(String collection) {
            this.collection = collection;
            return this;
        }

        @Override
        public InstanceParameter operation(String operation) {
            this.operation = operation;
            return this;
        }

        @Override
        public DatabaseParameter instance(String host, Integer port) {
            this.host = host;
            this.port = port;
            return this;
        }

        @Override
        public DatabaseParameter instance(String host, String pathOrId) {
            this.host = host;
            this.pathOrId = pathOrId;
            return this;
        }

        @Override
        public DatabaseParameter noInstance() {
            return this;
        }

        @Override
        public SlowQueryParameter databaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        @Override
        public Build cloudResourceId(String cloudResourceId) {
            this.cloudResourceId = cloudResourceId;
            return this;
        }

        @Override
        public DatastoreParameters build() {
            if (inputQueryLabel != null && rawInputQuery != null && rawInputQueryConverter != null) {
                return new SlowQueryWithInputDatastoreParameters<>(
                        (SlowQueryDatastoreParameters<Object>) buildWithSlowQuery(), inputQueryLabel, rawInputQuery,
                        rawInputQueryConverter);
            } else if (rawQuery != null && queryConverter != null) {
                return new SlowQueryDatastoreParameters<>(buildRegular(), rawQuery, queryConverter, normalizedSqlHash);
            } else {
                return buildRegular();
            }
        }

        @Override
        public SlowQueryParameter noDatabaseName() {
            return this;
        }

        @Override
        public <T> SlowQueryWithInputParameter slowQuery(T rawQuery, QueryConverter<T> queryConverter, String normalizedSqlHash) {
            this.rawQuery = rawQuery;
            this.queryConverter = (QueryConverter<Object>) queryConverter;
            this.normalizedSqlHash = normalizedSqlHash;
            return this;
        }

        @Override
        public <T> SlowQueryWithInputParameter slowQuery(T rawQuery, QueryConverter<T> queryConverter) {
            slowQuery(rawQuery, queryConverter, null);
            return this;
        }

        @Override
        public SlowQueryWithInputParameter noSlowQuery() {
            return this;
        }

        private DatastoreParameters buildRegular() {
            return new DatastoreParameters(product, collection, operation, host, port, pathOrId, databaseName, cloudResourceId);
        }

        private SlowQueryDatastoreParameters<?> buildWithSlowQuery() {
            return new SlowQueryDatastoreParameters<>(buildRegular(), rawQuery, queryConverter, normalizedSqlHash);
        }

        @Override
        public <T> Build slowQueryWithInput(String inputQueryLabel, T rawInputQuery,
                QueryConverter<T> rawInputQueryConverter) {
            this.inputQueryLabel = inputQueryLabel;
            this.rawInputQuery = rawInputQuery;
            this.rawInputQueryConverter = (QueryConverter<Object>) rawInputQueryConverter;
            return this;
        }
    }

    /**
     * Returns the name of the vendor.
     *
     * @return Vendor name
     * @since 3.36.0
     */
    public String getProduct() {
        return product;
    }

    /**
     * Returns the name of the collection or table.
     *
     * @return Table name
     * @since 3.36.0
     */
    public String getCollection() {
        return collection;
    }

    /**
     * Returns the datastore operation. This is the primitive operation type accepted by the datastore itself or the
     * name of the API method in the client library.
     *
     * @return Operation performed on the datastore
     * @since 3.36.0
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Returns the name of the host where the datastore is located.
     *
     * @return Host for the external call
     * @since 3.36.0
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port for the connection to the datastore.
     *
     * @return Port for the datastore
     * @since 3.36.0
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Return the path or id of the instance.
     *
     * @return Path or identifier of the datastore
     */
    public String getPathOrId() {
        return pathOrId;
    }

    /**
     * Return the name of the database where the query was executed.
     *
     * @return Database name
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * @return the cloud provider's identifier for the message queue. Eg. in AWS, this should be an ARN.
     */
    public String getCloudResourceId() {
        return cloudResourceId;
    }

    // Builder Interfaces

    /**
     * Set the name of the vendor.
     *
     * @param product the name of the product
     * @return the next builder interface
     */
    public static CollectionParameter product(String product) {
        return new DatastoreParameters.Builder(product);
    }

    public interface CollectionParameter {

        /**
         * Set the name of the collection or table.
         *
         * @param collection the collection or table
         * @return the next builder interface
         */
        OperationParameter collection(String collection);

    }

    public interface OperationParameter {

        /**
         * Set the datastore operation. This is the primitive operation type accepted by the datastore itself or the
         * name of the API method in the client library.
         *
         * @param operation the datastore operation
         * @return the next builder interface
         */
        InstanceParameter operation(String operation);

    }

    public interface InstanceParameter extends Build {

        /**
         * Set the host and port of the datastore connection used for this query.
         *
         * @param host The host where the datastore is located
         * @param port The port for the connection to the datastore
         * @return the next builder interface
         */
        DatabaseParameter instance(String host, Integer port);

        /**
         * Set the host and pathOrId of the datastore connection used for this query.
         *
         * @param host     The host where the datastore is located
         * @param pathOrId The path or identifier of this instance
         * @return the next builder interface
         */
        DatabaseParameter instance(String host, String pathOrId);

        /**
         * No instance information recorded.
         *
         * @return the next builder interface
         */
        DatabaseParameter noInstance();

    }

    public interface DatabaseParameter extends Build {

        /**
         * Set the name of the database where the query was executed
         *
         * @param databaseName the name of the database where the query was executed
         * @return the next builder interface
         */
        SlowQueryParameter databaseName(String databaseName);

        /**
         * No database information recorded.
         *
         * @return the next builder interface
         */
        SlowQueryParameter noDatabaseName();

    }

    public interface SlowQueryParameter extends Build {
        /**
         * Set a raw query and queryConverter to be used when reporting this call as a slow query
         *
         * @param rawQuery       The raw query object used for transforming into a raw and obfuscated query string
         * @param queryConverter A converter to transform the rawQuery into a raw and obfuscated query string
         * @param normalizedSqlHash the hash of the normalized SQL statement
         * @param <T>            The type of the query
         *
         * @return the next builder interface
         */
        <T> SlowQueryWithInputParameter slowQuery(T rawQuery, QueryConverter<T> queryConverter, String normalizedSqlHash);

        /**
         * Set a raw query and queryConverter to be used when reporting this call as a slow query
         *
         * @param rawQuery       The raw query object used for transforming into a raw and obfuscated query string
         * @param queryConverter A converter to transform the rawQuery into a raw and obfuscated query string
         * @param <T>            The type of the query
         * @return the next builder interface
         */
        <T> SlowQueryWithInputParameter slowQuery(T rawQuery, QueryConverter<T> queryConverter);

        /**
         * No slow query information recorded.
         *
         * @return the next builder interface
         */
        SlowQueryWithInputParameter noSlowQuery();

    }

    public interface SlowQueryWithInputParameter extends Build {

        /**
         * Set an input query and converter to be used when reporting this call as a slow query. The input query
         * could be anything from "Hibernate HQL" to a custom query language that gets converted into SQL.
         *
         * @param inputQueryLabel        The label used to display this input query in the UI.
         * @param rawInputQuery          The raw input query object used for transforming into a raw and obfuscated input query string
         * @param rawInputQueryConverter A converter to transform the rawInputQuery into a raw and obfuscated input query string
         * @param <T>                    The type of the input query
         * @return the next builder interface
         */
        <T> Build slowQueryWithInput(String inputQueryLabel, T rawInputQuery, QueryConverter<T> rawInputQueryConverter);

    }

    public interface Build {

        /**
         * Set the cloud provider's id for the database.
         * This method is optional and can be bypassed by calling build directly.
         * @return the build object so it can be built.
         */
        Build cloudResourceId(String cloudResourceId);

        /**
         * Build the final {@link DatastoreParameters} for the API call.
         *
         * @return the completed DatastoreParameters object
         */
        DatastoreParameters build();

    }

}