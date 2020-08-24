package com.newrelic.agent.jmx;

import java.util.Map;

public interface LinkingMetadataMBean {

    Map<String, String> readLinkingMetadata();

}
