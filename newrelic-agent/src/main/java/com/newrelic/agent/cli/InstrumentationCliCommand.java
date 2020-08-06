package com.newrelic.agent.cli;

import com.newrelic.agent.xml.XmlInstrumentOptions;
import com.newrelic.agent.xml.XmlInstrumentValidator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class InstrumentationCliCommand extends AbstractCliCommand {
    @Override
    public String getLongDescription() {
        return "[OPTIONS] Validates a custom instrumentation xml configuration file.";
    }

    @Override
    public Options getOptions() {
        Options options = new Options();
        XmlInstrumentOptions[] instrumentOps = XmlInstrumentOptions.values();
        for (XmlInstrumentOptions op : instrumentOps) {
            options.addOption(op.getFlagName(), op.getFlagName(), op.isArgRequired(), op.getDescription());
        }
        return options;
    }

    @Override
    public void performCommand(CommandLine parsedOptions) {
        XmlInstrumentValidator.validateInstrumentation(parsedOptions);
    }
}
