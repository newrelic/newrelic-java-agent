package com.newrelic.agent.config;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApplicationLoggingLabelsConfigTest {

    private static final String PARENT_ROOT = "newrelic.config.application_logging.forwarding.labels.";
    private static final String LABELS = "labels";

    @Test
    public void testApplicationLoggingLabelsConfigEnabled() {
        Map<String, Object> props = new HashMap<>();
        props.put("enabled", true);

        ApplicationLoggingLabelsConfig config = new ApplicationLoggingLabelsConfig(props, PARENT_ROOT + LABELS);
        assertTrue(config.getEnabled());
    }

    @Test
    public void testLabelCOnfigDisabledByDefault() {
        Map<String, Object> props = new HashMap<>();
        ApplicationLoggingLabelsConfig config = new ApplicationLoggingLabelsConfig(props, PARENT_ROOT + LABELS);
        assertFalse(config.getEnabled());
    }

}
