package com.newrelic.agent.commands;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.config.JfrConfig;
import com.newrelic.agent.jfr.JfrService;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

public class EnableJfrCommand extends AbstractCommand {

    public static final String COMMAND_NAME = "enable_jfr";

    private JfrService jfrService = null;

    private JfrConfig jfrConfig = null;

    public EnableJfrCommand(JfrService jfrService, JfrConfig jfrConfig) {
        super(COMMAND_NAME);
        this.jfrService = jfrService;
        this.jfrConfig = jfrConfig;
    }

    @Override
    public Map process(final IRPMService rpmService, Map arguments) throws CommandException {
        if (!jfrConfig.isEnabled()) {
            Agent.LOG.log(Level.INFO, "Processing EnableJfrCommand");
            try {
                jfrConfig.setEnabled(true);
                jfrService.start();
            } catch (Exception e) {
                Agent.LOG.log(Level.SEVERE, "Exception occurred attempting to start JFRService", e);
            }
        } else {
            Agent.LOG.info("EnableJfrCommand - JFR Service is already enabled; ignoring command");
        }

        return Collections.EMPTY_MAP;
    }
}
