package com.nr.lettuce65.instrumentation;

import com.newrelic.api.agent.DatastoreParameters;
import io.lettuce.core.RedisURI;
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
        assertEquals("0", dbParams.getDatabaseName()); //default redis db is 0

        // And database info is populated correctly
        assertEquals("Redis", dbParams.getProduct());
        assertEquals(operation, dbParams.getOperation());
    }

    @Test
    public void testDatastoreParametersForUri_withDB() {
        // Given
        Integer expectedPort = 12345;
        RedisURI uri = RedisURI.create("redis://localhost:" + expectedPort + "/1");
        String operation = "SET";

        // When
        DatastoreParameters dbParams = RedisDatastoreParameters.from(uri, operation);

        // Then url values are correct
        assertEquals(expectedPort, dbParams.getPort());
        assertEquals("localhost", dbParams.getHost());
        assertEquals("1", dbParams.getDatabaseName()); //default redis db is 0

        // And database info is populated correctly
        assertEquals("Redis", dbParams.getProduct());
        assertEquals(operation, dbParams.getOperation());
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

        // And database info is STILL populated correctly
        assertEquals("Redis", dbParams.getProduct());
        assertEquals(operation, dbParams.getOperation());
    }
}