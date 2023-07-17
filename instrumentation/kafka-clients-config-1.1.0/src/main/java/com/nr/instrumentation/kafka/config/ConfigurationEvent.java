package com.nr.instrumentation.kafka.config;

import java.util.Map;

/**
 * Cached representation of a custom event to be issued as a description of a kafka
 * client config
 */
class ConfigurationEvent {

    public static final String ATTR_CONSTRUCTIONS = "constructions";

    private final String eventType;
    private final Map<String, Object> attributes;
    private int numRegistrations = 1;

    public ConfigurationEvent(final String eventType, final Map<String, Object> attributes) {
        this.eventType = eventType;
        this.attributes = attributes;
        updateConstructionCount();
    }

    private void updateConstructionCount() {
        this.attributes.put(ATTR_CONSTRUCTIONS, numRegistrations);
    }

    public String getEventType() {
        return eventType;
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public int getNumRegistrations() {
        return numRegistrations;
    }

    public void replacing(final ConfigurationEvent replacedEvent) {
        numRegistrations += replacedEvent.numRegistrations;
        updateConstructionCount();
    }
}
