/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package mongo;

import com.mongodb.ServerAddress;
import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterDescription;
import com.mongodb.connection.ClusterType;
import com.mongodb.connection.ServerConnectionState;
import com.mongodb.connection.ServerDescription;
import com.mongodb.internal.async.SingleResultCallback;
import com.nr.agent.mongo.MongoUtil;
import com.nr.agent.mongo.NRCallbackWrapper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MongoUtilTest {

    @Test
    public void addDatabaseAndHostToMap_withValidKeyValue_addsSuccessfully() {
        assertEquals(MongoUtil.UNKNOWN, MongoUtil.getHostBasedOnDatabaseName("dbname"));
        MongoUtil.addDatabaseAndHostToMap("dbname", "myhost");
        assertEquals("myhost", MongoUtil.getHostBasedOnDatabaseName("dbname"));
    }

    @Test
    public void addDatabaseAndHostToMap_withNewValue_updatesSuccessfully() {
        MongoUtil.addDatabaseAndHostToMap("dbname", "myhost");
        MongoUtil.addDatabaseAndHostToMap("dbname", "newValue");
        assertEquals("newValue", MongoUtil.getHostBasedOnDatabaseName("dbname"));
    }

    @Test
    public void determineHostDisplayValueFromCluster_withMultipleHosts_returnsCluster() {
        List<ServerDescription> serverDescriptions = new ArrayList<>();

        serverDescriptions.add(ServerDescription.builder().state(ServerConnectionState.CONNECTING).address(new ServerAddress("localhost:8888")).build());
        serverDescriptions.add(ServerDescription.builder().state(ServerConnectionState.CONNECTING).address(new ServerAddress("localhost2:9999")).build());
        ClusterDescription clusterDescription = new ClusterDescription(ClusterConnectionMode.MULTIPLE, ClusterType.SHARDED, serverDescriptions);

        assertEquals(MongoUtil.CLUSTER, MongoUtil.determineHostDisplayValueFromCluster(clusterDescription));
    }

    @Test
    public void determineHostDisplayValueFromCluster_withSingleHost_returnsHost() {
        List<ServerDescription> serverDescriptions = new ArrayList<>();

        serverDescriptions.add(ServerDescription.builder().state(ServerConnectionState.CONNECTING).address(new ServerAddress("localhost:8888")).build());
        ClusterDescription clusterDescription = new ClusterDescription(ClusterConnectionMode.MULTIPLE, ClusterType.SHARDED, serverDescriptions);

        assertEquals("localhost:8888", MongoUtil.determineHostDisplayValueFromCluster(clusterDescription));
    }

    @Test
    public void instrumentSingleResultCallback_properlyWrapsTarget() {
        SingleResultCallback<Void> callback = (result, t) -> {
            //no op
        };

        SingleResultCallback<Void> wrapped = MongoUtil.instrumentSingleResultCallback(callback, "collection", "insert", "dbname", "localhost:8888");
        assertTrue(wrapped instanceof NRCallbackWrapper);
        NRCallbackWrapper<Void> nrCallbackWrapper = (NRCallbackWrapper<Void>) wrapped;
        assertEquals("collection", nrCallbackWrapper.params.getCollection());
        assertEquals("dbname", nrCallbackWrapper.params.getDatabaseName());
        assertEquals("insert", nrCallbackWrapper.params.getOperation());
        assertEquals("localhost", nrCallbackWrapper.params.getHost());
        assertEquals(8888, (int) nrCallbackWrapper.params.getPort());
    }
}
