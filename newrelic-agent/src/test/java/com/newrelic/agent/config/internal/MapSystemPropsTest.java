package com.newrelic.agent.config.internal;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MapSystemPropsTest {
    @Test
    public void constructor_populatesProps() {
        Map<String, String> props = new HashMap<>();
        props.put("k1", "v1");
        props.put("k2", "v2");
        props.put("k3", "v3");
        MapSystemProps mapSystemProps = new MapSystemProps(props);

        Assert.assertEquals("v1", mapSystemProps.getSystemProperty("k1"));
        Assert.assertEquals("v2", mapSystemProps.getSystemProperty("k2"));
        Assert.assertEquals("v3", mapSystemProps.getSystemProperty("k3"));
        Assert.assertEquals(props.size(), mapSystemProps.getAllSystemProperties().size());
    }
}
