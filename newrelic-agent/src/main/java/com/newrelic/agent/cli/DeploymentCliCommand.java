package com.newrelic.agent.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

class DeploymentCliCommand extends AbstractCliCommand {
    @Override
    public String getLongDescription() {
        return "[OPTIONS] [description]  Records a deployment";
    }

    @Override
    public Options getOptions() {
        Options options = new Options();
        options.addOption(Deployments.APP_NAME_OPTION, Deployments.APP_NAME_OPTION, true,
                "Set the application name. Default is app_name setting in newrelic.yml");
        options.addOption(Deployments.ENVIRONMENT_OPTION, Deployments.ENVIRONMENT_OPTION, true, "Set the environment (staging, production, test, development)");
        options.addOption(Deployments.USER_OPTION, Deployments.USER_OPTION, true, "Specify the user deploying");
        options.addOption(Deployments.REVISION_OPTION, Deployments.REVISION_OPTION, true, "Specify the revision being deployed");
        options.addOption(Deployments.CHANGE_LOG_OPTION, Deployments.CHANGE_LOG_OPTION, false, "Reads the change log for a deployment from standard input");
        return options;
    }

    @Override
    public void performCommand(CommandLine parsedOptions) throws Exception {
        Deployments.recordDeployment(parsedOptions);
    }
}
