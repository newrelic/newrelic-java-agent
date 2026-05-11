/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.dynamodb_dax;

import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.dax.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

public class DAXClientConfigCacheTest {

    @Test
    public void testStoreAndRetrieveConfiguration() {
        Object client = new Object();
        Configuration config = createConfiguration();

        DAXClientConfigCache.storeConfiguration(client, config);

        Configuration retrieved = DAXClientConfigCache.getConfiguration(client);
        assertNotNull(retrieved);
        assertEquals(config, retrieved);
    }

    @Test
    public void testGetConfiguration_unknownClient() {
        Object unknownClient = new Object();

        Configuration retrieved = DAXClientConfigCache.getConfiguration(unknownClient);

        assertNull(retrieved);
    }

    @Test
    public void testGetConfiguration_nullClient() {
        Configuration retrieved = DAXClientConfigCache.getConfiguration(null);
        assertNull(retrieved);
    }

    @Test
    public void testStoreConfiguration_nullClient() {
        Configuration config = createConfiguration();

        DAXClientConfigCache.storeConfiguration(null, config);
        // no exception thrown
    }

    @Test
    public void testStoreConfiguration_nullConfiguration() {
        Object client = new Object();

        DAXClientConfigCache.storeConfiguration(client, null);

        assertNull(DAXClientConfigCache.getConfiguration(client));
    }

    @Test
    public void testStoreConfiguration_bothNull() {
        DAXClientConfigCache.storeConfiguration(null, null);
        // no exception thrown
    }

    @Test
    public void testMultipleClients() {
        Object client1 = new Object();
        Object client2 = new Object();
        Configuration config1 = createConfiguration("dax://cluster1.amazonaws.com");
        Configuration config2 = createConfiguration("dax://cluster2.amazonaws.com");

        DAXClientConfigCache.storeConfiguration(client1, config1);
        DAXClientConfigCache.storeConfiguration(client2, config2);

        assertEquals(config1, DAXClientConfigCache.getConfiguration(client1));
        assertEquals(config2, DAXClientConfigCache.getConfiguration(client2));
    }

    @Test
    public void testOverwriteConfiguration() {
        Object client = new Object();
        Configuration config1 = createConfiguration("dax://cluster1.amazonaws.com");
        Configuration config2 = createConfiguration("dax://cluster2.amazonaws.com");

        DAXClientConfigCache.storeConfiguration(client, config1);
        DAXClientConfigCache.storeConfiguration(client, config2);

        assertEquals(config2, DAXClientConfigCache.getConfiguration(client));
    }

    private Configuration createConfiguration() {
        return createConfiguration("dax://my-cluster.l6fzcv.dax-clusters.us-east-2.amazonaws.com");
    }

    private Configuration createConfiguration(String url) {
        return Configuration.builder()
                .region(Region.US_EAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("accessKey", "secretKey")))
                .url(url)
                .build();
    }
}
