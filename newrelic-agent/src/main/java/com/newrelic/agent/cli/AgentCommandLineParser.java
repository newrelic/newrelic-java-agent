/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cli;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.Agent;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

public class AgentCommandLineParser {
    private static final String DEPLOYMENT_COMMAND = "deployment";
    private static final String VERIFY_INSTRUMENTATION = "verifyInstrumentation";
    /**
     * Used to create the custom instrumentation file.
     */
    private static final String INSTRUMENT_COMMAND = "instrument";
    private static final String OBSCURE_COMMAND = "obscure";
    private static final String VERSION_COMMAND = "version";
    private static final Map<String, AbstractCliCommand> commands = ImmutableMap.of(
            OBSCURE_COMMAND, new ObscuringCliCommand(),
            VERIFY_INSTRUMENTATION, new VerifyInstrumentationCliCommand(),
            INSTRUMENT_COMMAND, new InstrumentationCliCommand(),
            DEPLOYMENT_COMMAND, new DeploymentCliCommand(),
            VERSION_COMMAND, new VersionCliCommand()
    );

    /**
     * Parses the command-line arguments and executes the expected behavior.
     * @param args command-line arguments
     * @return expected system exit code
     */
    public int parseCommand(String[] args) {
        if (args == null || args.length == 0) {
            printHelp();
            return 1;
        }

        String firstArgument = args[0];
        AbstractCliCommand command = parseFirstArg(firstArgument);

        if (command == null) {
            printHelp();
            return 1;
        }

        DefaultParser parser = new DefaultParser();
        String[] remainingArgs = args.length == 1
            ? new String[0]
            : Arrays.copyOfRange(args, 1, args.length);

        final CommandLine commandLine;
        try {
            commandLine = parser.parse(
                    combineOptions(command.getOptions(), getHelpOption()),
                    remainingArgs);
        } catch (Throwable t) {
            printHelp(firstArgument, command, t.getMessage());
            return 1;
        }

        if (commandLine == null) {
            printHelp();
            return 1;
        }

        if (commandLine.hasOption("help")) {
            printHelp(firstArgument, command, null);
            return 1;
        }

        try {
            command.performCommand(commandLine);
        } catch (Throwable t) {
            printHelp(firstArgument, command, t.getMessage());
            return 1;
        }

        return 0;
    }

    /**
     * The first argument can be a command. commons-cli is not really set up for this,
     * so we manually parse it. The only options if a command are not provided are --help and --version,
     * which are trivial to check for.
     * @param arg The first provided argument on the command-line.
     * @return The argument if a valid command and null otherwise.
     */
    private AbstractCliCommand parseFirstArg(String arg) {
        if (arg == null || arg.isEmpty()) {
            System.err.println("ERROR: expected a command for the first argument.");
            return null;
        }

        if (Arrays.asList("-h", "-help", "--help", "help").contains(arg)) {
            return null;
        }

        if (commands.containsKey(arg)) {
            return commands.get(arg);
        }

        if (Arrays.asList("-v", "-version", "--version").contains(arg)) {
            return commands.get(VERSION_COMMAND);
        }

        System.err.println("ERROR: expected a command for the first argument.");
        return null;
    }

    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        System.out.println(MessageFormat.format("New Relic Agent Version {0}", Agent.getVersion()));
        formatter.printHelp("java -jar newrelic.jar <command> [command options...]", "", getHelpOption(), getCommandLineFooter());
    }

    private void printHelp(String commandName, AbstractCliCommand command, String error) {
        if (commandName == null) {
            printHelp();
            return;
        }

        if (error != null && !error.isEmpty()) {
            System.err.println("Error: " + error);
        }

        System.out.println(MessageFormat.format("New Relic Agent Version {0}", Agent.getVersion()));

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(
                "java -jar newrelic.jar " + commandName,
                command.getLongDescription(),
                command.getOptions(),
                "",
                true);
    }

    private String getCommandLineFooter() {
        int maxCommandLength = getMaxCommandLength();
        String minSpaces = "    ";

        StringBuilder builder = new StringBuilder("\nCommands:");
        for (Entry<String, AbstractCliCommand> entry : commands.entrySet()) {
            String extraSpaces = new String(new char[maxCommandLength - entry.getKey().length()]).replace('\0', ' ');
            builder.append("\n  ").append(entry.getKey()).append(extraSpaces).append(minSpaces).append(entry.getValue().getLongDescription());
        }
        return builder.toString();
    }

    private int getMaxCommandLength() {
        int max = 0;
        for (String command : commands.keySet()) {
            max = Math.max(max, command.length());
        }
        return max;
    }

    private Options combineOptions(Options... optionsList) {
        Options newOptions = new Options();
        for (Options options : optionsList) {
            for (Option option : options.getOptions()) {
                newOptions.addOption(option);
            }
        }
        return newOptions;
    }

    private Options getHelpOption() {
        Options options = new Options();
        options.addOption("h", "help", false, "Prints help");
        return options;
    }

}
