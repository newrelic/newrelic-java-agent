package com.newrelic.agent.commands;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.config.JfrConfig;
import com.newrelic.agent.jfr.JfrService;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

public class StopJfrCommand extends AbstractCommand {

    public static final String COMMAND_NAME = "disable_jfr";

    private JfrService jfrService = null;
    private JfrConfig jfrConfig = null;

    public StopJfrCommand(JfrService jfrService, JfrConfig jfrConfig) {
        super(COMMAND_NAME);
        this.jfrService = jfrService;
        this.jfrConfig = jfrConfig;
    }

    @Override
    public Map process(final IRPMService rpmService, Map arguments) throws CommandException {
        Agent.LOG.log(Level.INFO, "Processing StopJfrCommand");
        try {
            jfrConfig.setEnabled(false);
            jfrService.toggleOff();
        } catch (Exception e) {
            Agent.LOG.log(Level.SEVERE, "Exception occurred attempting to stop JFRService", e);
        }

        return Collections.EMPTY_MAP;
    }
}
