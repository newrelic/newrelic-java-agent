package com.newrelic.agent.cli;

import com.newrelic.weave.verification.WeavePackageVerifier;
import org.apache.commons.cli.CommandLine;

class VerifyInstrumentationCliCommand extends AbstractCliCommand {
    @Override
    public String getLongDescription() {
        return "Verifies a weave package";
    }

    @Override
    public void performCommand(CommandLine parsedOptions) {
        WeavePackageVerifier.main(parsedOptions.getArgs());
    }
}
