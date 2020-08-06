package com.newrelic.agent.cli;

import com.newrelic.agent.Agent;
import org.apache.commons.cli.CommandLine;

class VersionCliCommand extends AbstractCliCommand {
    @Override
    public String getLongDescription() {
        return "print the agent version in this jar";
    }

    @Override
    public void performCommand(CommandLine parsedOptions) {
        System.out.println(Agent.getVersion());
    }
}
