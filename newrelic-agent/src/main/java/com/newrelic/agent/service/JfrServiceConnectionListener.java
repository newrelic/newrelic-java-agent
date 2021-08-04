package com.newrelic.agent.service;

import com.newrelic.agent.AgentConnectionEstablishedListener;
import com.newrelic.agent.jfr.JfrService;

import java.util.Map;

public class JfrServiceConnectionListener implements AgentConnectionEstablishedListener {

    private final JfrService jfrService;

    public JfrServiceConnectionListener(JfrService jfrService) {
        this.jfrService = jfrService;
    }

    @Override
    public void onEstablished(String appName, String agentRunToken, Map<String, String> requestMetadata) {
        try {
            //The entity guid is now available via RPM Service, so start JFR.
            jfrService.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
