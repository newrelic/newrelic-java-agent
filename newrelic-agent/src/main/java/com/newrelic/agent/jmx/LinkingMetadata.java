package com.newrelic.agent.jmx;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;

import java.util.Map;
import java.util.logging.Level;

public class LinkingMetadata implements LinkingMetadataMBean {

    @Override
    public Map<String, String> readLinkingMetadata() {
        Agent agent = NewRelic.getAgent();
        Logger logger = agent.getLogger();
        logger.log(Level.INFO, "JMX LinkingMetadata: Fetching linking metadata from the agent...");
        return agent.getLinkingMetadata();
    }
}
