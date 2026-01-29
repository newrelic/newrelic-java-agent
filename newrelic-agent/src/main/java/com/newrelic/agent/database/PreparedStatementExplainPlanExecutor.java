/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.newrelic.agent.bridge.datastore.DatabaseVendor;
import com.newrelic.agent.bridge.datastore.RecordSql;
import com.newrelic.agent.tracers.SqlTracerExplainInfo;

public class PreparedStatementExplainPlanExecutor extends DefaultExplainPlanExecutor {

    private final Object[] sqlParameters;
    private final DatabaseVendor vendor;

    public PreparedStatementExplainPlanExecutor(SqlTracerExplainInfo tracer, String originalSqlStatement,
            Object[] sqlParameters, RecordSql recordSql, DatabaseVendor vendor) {
        super(tracer, originalSqlStatement, recordSql);
        this.sqlParameters = sqlParameters;
        this.vendor = vendor;
    }

    @Override
    protected Statement createStatement(Connection connection, String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    @Override
    protected ResultSet executeStatement(Statement statement, String sql) throws SQLException {
        PreparedStatement preparedStatement = (PreparedStatement) statement;
        Connection connection = preparedStatement.getConnection();
        setSqlParameters(preparedStatement, connection);
        return preparedStatement.executeQuery();
    }

    private void setSqlParameters(PreparedStatement preparedStatement, Connection connection) {
        if (sqlParameters == null) {
            return;
        }

        try {
            for (int i = 0; i < sqlParameters.length; i++) {
                Object param = sqlParameters[i];
                if (param == null) {
                    break;
                }

                // Handle array parameters by converting Java arrays to SQL Array objects
                if (param.getClass().isArray()) {
                    java.sql.Array sqlArray = convertToSqlArray(connection, param);
                    if (sqlArray != null) {
                        preparedStatement.setArray(i + 1, sqlArray);
                    } else {
                        preparedStatement.setObject(i + 1, param);
                    }
                } else {
                    preparedStatement.setObject(i + 1, param);
                }
            }
        } catch (Throwable ignored) {
            // ignore
        }
    }

    /**
     * Converts a Java array to a SQL Array for use with array-based SQL operations.
     *
     * @param connection The database connection (for the createArrayOf() method)
     * @param arrayParam The Java array parameter
     *
     * @return A SQL Array, or null if conversion fails
     */
    private java.sql.Array convertToSqlArray(Connection connection, Object arrayParam) {
        try {
            String typeName = getSqlTypeName(arrayParam);
            if (typeName != null) {
                Object[] elements = convertToObjectArray(arrayParam);
                return connection.createArrayOf(typeName, elements);
            }
        } catch (Throwable t) {
            // If conversion fails, return null and let caller use setObject
        }

        return null;
    }

    /**
     * Determines the SQL type name for a Java array.
     *
     * Note: The type names returned are primarily compatible with PostgreSQL, which has
     * excellent native array support. Other databases have varying levels of support:
     * - PostgreSQL: Full support for these array types
     * - H2: Supports arrays with these type names
     * - MySQL: Does NOT support SQL array types
     * - SQL Server: Does NOT support SQL array types
     * - Oracle: Array support (VARRAY) with Oracle-specific type names (handled below)
     *
     * The code handles incompatibility gracefully by returning null for unsupported types,
     * which causes the caller to fall back to using setObject() instead of setArray().
     *
     * @param arrayParam The Java array parameter to determine the type for
     *
     * @return the type name or null if the type cannot be determined
     */
    private String getSqlTypeName(Object arrayParam) {
        Class<?> componentType = arrayParam.getClass().getComponentType();
        if (componentType == null) {
            return null;
        }

        // Check if this is Oracle - use Oracle-specific type names
        boolean isOracle = vendor != null && "Oracle".equalsIgnoreCase(vendor.getName());

        if (componentType == Integer.class || componentType == int.class) {
            return isOracle ? "NUMBER" : "integer";
        } else if (componentType == Long.class || componentType == long.class) {
            return isOracle ? "NUMBER" : "bigint";
        } else if (componentType == String.class) {
            return isOracle ? "VARCHAR2" : "varchar";
        } else if (componentType == Double.class || componentType == double.class) {
            return isOracle ? "BINARY_DOUBLE" : "double precision";
        } else if (componentType == Float.class || componentType == float.class) {
            return isOracle ? "BINARY_FLOAT" : "real";
        } else if (componentType == Boolean.class || componentType == boolean.class) {
            return "boolean";
        } else if (componentType == Short.class || componentType == short.class) {
            return isOracle ? "NUMBER" : "smallint";
        } else if (componentType == Byte.class || componentType == byte.class) {
            return isOracle ? "NUMBER" : "smallint";
        }
        // For other types, return null and fall back to setObject
        return null;
    }

    /**
     * Converts a primitive or object array to an Object array.
     */
    private Object[] convertToObjectArray(Object arrayParam) {
        Class<?> componentType = arrayParam.getClass().getComponentType();

        if (!componentType.isPrimitive()) {
            return (Object[]) arrayParam;
        }

        // Handle primitive arrays
        if (componentType == int.class) {
            int[] primitiveArray = (int[]) arrayParam;
            Integer[] objectArray = new Integer[primitiveArray.length];
            for (int i = 0; i < primitiveArray.length; i++) {
                objectArray[i] = primitiveArray[i];
            }
            return objectArray;
        } else if (componentType == long.class) {
            long[] primitiveArray = (long[]) arrayParam;
            Long[] objectArray = new Long[primitiveArray.length];
            for (int i = 0; i < primitiveArray.length; i++) {
                objectArray[i] = primitiveArray[i];
            }
            return objectArray;
        } else if (componentType == double.class) {
            double[] primitiveArray = (double[]) arrayParam;
            Double[] objectArray = new Double[primitiveArray.length];
            for (int i = 0; i < primitiveArray.length; i++) {
                objectArray[i] = primitiveArray[i];
            }
            return objectArray;
        } else if (componentType == float.class) {
            float[] primitiveArray = (float[]) arrayParam;
            Float[] objectArray = new Float[primitiveArray.length];
            for (int i = 0; i < primitiveArray.length; i++) {
                objectArray[i] = primitiveArray[i];
            }
            return objectArray;
        } else if (componentType == boolean.class) {
            boolean[] primitiveArray = (boolean[]) arrayParam;
            Boolean[] objectArray = new Boolean[primitiveArray.length];
            for (int i = 0; i < primitiveArray.length; i++) {
                objectArray[i] = primitiveArray[i];
            }
            return objectArray;
        } else if (componentType == short.class) {
            short[] primitiveArray = (short[]) arrayParam;
            Short[] objectArray = new Short[primitiveArray.length];
            for (int i = 0; i < primitiveArray.length; i++) {
                objectArray[i] = primitiveArray[i];
            }
            return objectArray;
        } else if (componentType == byte.class) {
            byte[] primitiveArray = (byte[]) arrayParam;
            Byte[] objectArray = new Byte[primitiveArray.length];
            for (int i = 0; i < primitiveArray.length; i++) {
                objectArray[i] = primitiveArray[i];
            }
            return objectArray;
        }

        return new Object[0];
    }
}