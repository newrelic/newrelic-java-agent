package com.newrelic.agent.commands;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.jfr.JfrService;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

public class StopJfrCommand extends AbstractCommand {

    public static final String COMMAND_NAME = "disable_jfr";

    private JfrService jfrService = null;

    public StopJfrCommand(JfrService jfrService) {
        super(COMMAND_NAME);
        this.jfrService = jfrService;
    }

    @Override
    public Map process(final IRPMService rpmService, Map arguments) throws CommandException {
        if (jfrService.isRunning()) {
            Agent.LOG.log(Level.INFO, "Processing DisableJfrCommand");
            try {
                jfrService.toggleOff();
            } catch (Exception e) {
                Agent.LOG.log(Level.SEVERE, "Exception occurred attempting to stop JFRService", e);
            }
        } else {
            Agent.LOG.info("DisableJfrCommand - JFR Service is already disabled; ignoring command");
        }

        return Collections.EMPTY_MAP;
    }
}
