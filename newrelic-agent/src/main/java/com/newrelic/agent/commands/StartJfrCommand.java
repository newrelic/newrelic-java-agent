package com.newrelic.agent.commands;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.jfr.JfrService;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

public class StartJfrCommand extends AbstractCommand {

    public static final String COMMAND_NAME = "enable_jfr";

    private JfrService jfrService = null;

    public StartJfrCommand(JfrService jfrService) {
        super(COMMAND_NAME);
        this.jfrService = jfrService;
    }

    @Override
    public Map process(final IRPMService rpmService, Map arguments) throws CommandException {
        if (!jfrService.isRunning()) {
            Agent.LOG.log(Level.INFO, "Processing EnableJfrCommand");
            try {
                jfrService.toggleOn();
            } catch (Exception e) {
                Agent.LOG.log(Level.SEVERE, "Exception occurred attempting to start JFRService", e);
            }
        } else {
            Agent.LOG.info("EnableJfrCommand - JFR Service is already running; ignoring command");
        }

        return Collections.EMPTY_MAP;
    }
}
