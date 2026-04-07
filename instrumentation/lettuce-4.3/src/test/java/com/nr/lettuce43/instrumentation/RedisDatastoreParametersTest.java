package com.nr.lettuce43.instrumentation;

import com.lambdaworks.redis.RedisURI;
import com.newrelic.api.agent.DatastoreParameters;
import junit.framework.TestCase;
import org.junit.jupiter.api.Test;

public class RedisDatastoreParametersTest extends TestCase {

    @Test
    public void testDatastoreParametersForUri() {
        // Given
        Integer expectedPort = 12345;
        RedisURI uri = RedisURI.create("redis://localhost:" + expectedPort);
        String operation = "GET";

        // When
        DatastoreParameters dbParams = RedisDatastoreParameters.from(uri, operation);

        // Then url values are correct
        assertEquals(expectedPort, dbParams.getPort());
        assertEquals("localhost", dbParams.getHost());

        // And database info is populated correctly
        assertEquals("Redis", dbParams.getProduct());
        assertEquals(operation, dbParams.getOperation());
        assertEquals("0", dbParams.getDatabaseName());
    }

    @Test
    public void testDatastoreParametersForUriWithDB() {
        // Given
        Integer expectedPort = 12345;
        RedisURI uri = RedisURI.create("redis://localhost:" + expectedPort + "/3");
        String operation = "GET";

        // When
        DatastoreParameters dbParams = RedisDatastoreParameters.from(uri, operation);

        // Then url values are correct
        assertEquals(expectedPort, dbParams.getPort());
        assertEquals("localhost", dbParams.getHost());

        // And database info is populated correctly
        assertEquals("Redis", dbParams.getProduct());
        assertEquals(operation, dbParams.getOperation());
        assertEquals("3", dbParams.getDatabaseName());
    }

    @Test
    public void testDatastoreParametersForClustering_noUrl() {
        // Given
        String operation = "GET";

        // When
        DatastoreParameters dbParams = RedisDatastoreParameters.from(null, operation);

        // Then there are no url values
        assertNull(dbParams.getPort());
        assertNull(dbParams.getHost());
        assertNull(dbParams.getDatabaseName());

        // And database info is STILL populated correctly
        assertEquals("Redis", dbParams.getProduct());
        assertEquals(operation, dbParams.getOperation());
    }
}