package com.newrelic.agent.config.internal;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MapEnvironmentFacadeTest {
    @Test
    public void constructor_assignsInternalMap() {
        Map<String, String> props = new HashMap<>();
        props.put("k1", "v1");
        props.put("k2", "v2");
        props.put("k3", "v3");
        MapEnvironmentFacade mapEnvironmentFacade = new MapEnvironmentFacade(props);

        Assert.assertEquals("v1", mapEnvironmentFacade.getenv("k1"));
        Assert.assertEquals("v2", mapEnvironmentFacade.getenv("k2"));
        Assert.assertEquals("v3", mapEnvironmentFacade.getenv("k3"));
        Assert.assertEquals(props.size(), mapEnvironmentFacade.getAllEnvProperties().size());
    }
}
