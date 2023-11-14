package com.newrelic.agent.model;

import java.util.Map;

public class SyntheticsInfo {

    private final String initiator;
    private final String type;
    private final Map<String, String> attributeMap;

    public SyntheticsInfo(String initiator, String type, Map<String, String> attributeMap) {
        this.initiator = initiator;
        this.type = type;
        this.attributeMap = attributeMap;
    }

    public String getInitiator() {
        return initiator;
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getAttributeMap() {
        return attributeMap;
    }

}
