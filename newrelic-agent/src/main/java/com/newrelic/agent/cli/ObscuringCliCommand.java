package com.newrelic.agent.cli;

import com.newrelic.agent.util.Obfuscator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

class ObscuringCliCommand extends AbstractCliCommand{
    @Override
    public String getLongDescription() {
        return "[OPTIONS] [plaintext] Obscures a value for the yaml file.";
    }

    @Override
    public Options getOptions() {

        Option encodingKeyOption = Option.builder("k")
                .longOpt("obscuring-key")
                .hasArg()
                .argName("obscuring-key")
                .desc("the obscuring key used for obscuring and un-obscuring values")
                .required()
                .build();

        return new Options().addOption(encodingKeyOption);
    }

    @Override
    public void performCommand(CommandLine parsedOptions) throws Exception {
        String obscuringKey = parsedOptions.getOptionValue("obscuring-key");
        if (parsedOptions.getArgs().length != 1) {
            throw new Exception("You must specify exactly one string value to be obscured.");
        }
        System.out.println(Obfuscator.obfuscateNameUsingKey(parsedOptions.getArgs()[0], obscuringKey));
    }
}
