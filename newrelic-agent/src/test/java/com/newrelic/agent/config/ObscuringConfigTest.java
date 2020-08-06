package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class ObscuringConfigTest {
    @Test
    public void deobscuresAll() {
        Map<String, Object> obscuredMap = ImmutableMap.of(
                "obscuring_key", "abc123",
                "top_level", new ObscuredYamlPropertyWrapper("FwMPRFcAU1Y="),
                "in_list", Arrays.asList("a", "b", new ObscuredYamlPropertyWrapper("Ag==")),
                "in_map", Collections.singletonMap("state", new ObscuredYamlPropertyWrapper("LhAGVl1d"))
        );

        ObscuringConfig target = new ObscuringConfig(obscuredMap, "not_applicable");
        Map<String, Object> deobscuredProperties = target.getDeobscuredProperties();
        assertEquals("value324", deobscuredProperties.get("top_level"));
        assertEquals(Arrays.asList("a", "b", "c"), deobscuredProperties.get("in_list"));
        assertEquals(Collections.singletonMap("state", "Oregon"), deobscuredProperties.get("in_map"));
    }

}