package com.nr.agent.mongo;

import com.mongodb.ServerAddress;
import com.mongodb.connection.ServerConnectionState;
import com.mongodb.connection.ServerDescription;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class MongoUtilTest {

    @Test
    public void addDatabaseAndHostToMap_withValidKeyValue_addsSuccessfully() {
        assertEquals(MongoUtil.UNKNOWN_HOST, MongoUtil.getHostBasedOnDatabaseName("dbname"));
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
    public void concatHostsFromServerDescriptionList_createsCorrectHostString() {
        List<ServerDescription> serverDescriptions = new ArrayList<>();

        serverDescriptions.add(ServerDescription.builder().state(ServerConnectionState.CONNECTING).address(new ServerAddress("localhost:8888")).build());
        serverDescriptions.add(ServerDescription.builder().state(ServerConnectionState.CONNECTING).address(new ServerAddress("localhost2:9999")).build());

        assertEquals("localhost:8888;localhost2:9999", MongoUtil.concatHostsFromServerDescriptionList(serverDescriptions));
    }
}
