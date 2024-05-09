/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.xml.XmlInstrumentOptions;
import com.newrelic.agent.xml.XmlInstrumentValidator;
import com.newrelic.weave.verification.WeavePackageVerifier;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class AgentCommandLineParser {
    private static final String DEPLOYMENT_COMMAND = "deployment";
    private static final String VERIFY_INSTRUMENTATION = "verifyInstrumentation";
    /**
     * Used to create the custom instrumentation file.
     */
    private static final String INSTRUMENT_COMMAND = "instrument";
    private static final String INIT_CONFIG = "init-config";
    private static final Map<String, Options> commandOptionsMap;
    private static final Map<String, String> commandDescriptions;

    static {
        commandOptionsMap = new HashMap<>();
        commandOptionsMap.put(DEPLOYMENT_COMMAND, getDeploymentOptions());
        commandOptionsMap.put(INSTRUMENT_COMMAND, getInstrumentOptions());
        commandOptionsMap.put(VERIFY_INSTRUMENTATION, getVerifyInstrumentationOptions());
        commandOptionsMap.put(INIT_CONFIG, getInitConfigEnvUpdateOptions());

        commandDescriptions = new HashMap<>();
        commandDescriptions.put(DEPLOYMENT_COMMAND, "[OPTIONS] [description]  Records a deployment");
        commandDescriptions.put(INSTRUMENT_COMMAND, "[OPTIONS]                Validates a custom instrumentation xml configuration file.");
    }

    public void parseCommand(String[] args) {
        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse(getCommandLineOptions(), args);

            @SuppressWarnings("unchecked")
            List<String> argList = cmd.getArgList();
            String command = argList.size() > 0 ? argList.get(0) : null;

            if (cmd.hasOption('h')) {
                printHelp(command);
                return;
            }
            if (command != null) {
                Options commandOptions = commandOptionsMap.get(command);
                if (commandOptions == null) {
                    printHelp();
                    System.err.println("\nInvalid command - " + command);
                    System.exit(1);
                }
                cmd = parser.parse(commandOptions, args);
            }

            if (DEPLOYMENT_COMMAND.equals(command)) {
                deploymentCommand(cmd);
            } else if (INSTRUMENT_COMMAND.equals(command)) {
                instrumentCommand(cmd);
            } else if (VERIFY_INSTRUMENTATION.equals(command)) {
                verifyInstrumentation(cmd);
            }else if (INIT_CONFIG.equals(command)) {
                initConfigEnvUpdateCommand(cmd);
            } else if (cmd.hasOption('v') || cmd.hasOption("version")) {
                System.out.println(Agent.getVersion());
            } else {
                printHelp();
                System.exit(1);
            }
        } catch (ParseException e) {
            System.err.println("Error parsing arguments");
            printHelp();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error executing command");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Updates the xml instrumentation file.
     *
     * @param cmd The commands from the command line.
     * @throws Exception Thrown if a problem while updating the xml configuration file.
     */
    private void instrumentCommand(CommandLine cmd) throws Exception {
        XmlInstrumentValidator.validateInstrumentation(cmd);
    }

    private void deploymentCommand(CommandLine cmd) throws Exception {
        Deployments.recordDeployment(cmd);
    }

    private void initConfigEnvUpdateCommand(CommandLine cmd) throws Exception {
        InitConfigEnvUpdate.updateEnvInitialConfigOptions(cmd);
    }

    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        System.out.println(MessageFormat.format("New Relic Agent Version {0}", Agent.getVersion()));
        formatter.printHelp("java -jar newrelic.jar", "", getBasicOptions(), getCommandLineFooter());
    }

    private void printHelp(String command) {
        if (command == null) {
            printHelp();
            return;
        }
        HelpFormatter formatter = new HelpFormatter();
        System.out.println(MessageFormat.format("New Relic Agent Version {0}", Agent.getVersion()));
        String footer = "\n  " + command + " " + commandDescriptions.get(command);
        formatter.printHelp("java -jar newrelic.jar " + command, "", commandOptionsMap.get(command), footer);
    }

    private void verifyInstrumentation(CommandLine cmd) {
        List<String> args = cmd.getArgList().subList(1, cmd.getArgList().size());
        WeavePackageVerifier.main(args.toArray(new String[args.size()]));
    }

    private String getCommandLineFooter() {
        int maxCommandLength = getMaxCommandLength();
        String minSpaces = "    ";

        StringBuilder builder = new StringBuilder("\nCommands:");
        for (Entry<String, String> entry : commandDescriptions.entrySet()) {
            String extraSpaces = new String(new char[maxCommandLength - entry.getKey().length()]).replace('\0', ' ');
            builder.append("\n  ").append(entry.getKey()).append(extraSpaces).append(minSpaces).append(entry.getValue());
        }
        return builder.toString();
    }

    private int getMaxCommandLength() {
        int max = 0;
        for (String command : commandDescriptions.keySet()) {
            max = Math.max(max, command.length());
        }
        return max;
    }

    static Options getCommandLineOptions() {
        Collection<Options> values = new ArrayList<>(Collections.singletonList(getBasicOptions()));
        values.addAll(commandOptionsMap.values());
        return combineOptions(values);
    }

    @SuppressWarnings("unchecked")
    private static Options combineOptions(Collection<Options> optionsList) {
        Options newOptions = new Options();
        for (Options options : optionsList) {
            for (Option option : (Collection<Option>) options.getOptions()) {
                newOptions.addOption(option);
            }
        }
        return newOptions;
    }

    private static Options getBasicOptions() {
        Options options = new Options();
        options.addOption("v", false, "Prints the agent version");
        options.addOption("version", false, "Prints the agent version");
        options.addOption("h", false, "Prints help");
        return options;
    }

    private static Options getDeploymentOptions() {
        Options options = new Options();
        options.addOption(Deployments.APP_NAME_OPTION, true, "Set the application name. Default is app_name setting in newrelic.yml");
        options.addOption(Deployments.ENVIRONMENT_OPTION, true, "Set the environment (staging, production, test, development)");
        options.addOption(Deployments.USER_OPTION, true, "Specify the user deploying");
        options.addOption(Deployments.REVISION_OPTION, true, "Specify the revision being deployed");
        options.addOption(Deployments.CHANGE_LOG_OPTION, false, "Reads the change log for a deployment from standard input");
        return options;
    }

    /**
     * Returns the instrumentation options.
     *
     * @return All options for instrumentation.
     */
    private static Options getInstrumentOptions() {
        Options options = new Options();
        XmlInstrumentOptions[] instrumentOps = XmlInstrumentOptions.values();
        for (XmlInstrumentOptions op : instrumentOps) {
            options.addOption(op.getFlagName(), op.isArgRequired(), op.getDescription());
        }
        return options;
    }

    private static Options getVerifyInstrumentationOptions() {
        return new Options();
    }

    private static Options getInitConfigEnvUpdateOptions() {
        Options options = new Options();

        options.addOption(InitConfigEnvUpdate.CONFIG_FILE_LOCATION_OPTION, true, "Set the newrelic.config.file property for the Java agent");
        options.addOption(InitConfigEnvUpdate.LICENSE_KEY_OPTION, true, "Set the license_key property for the Java agent");
        options.addOption(InitConfigEnvUpdate.APP_NAME_OPTION, true, "Set the app_name property for the Java agent");
        options.addOption(InitConfigEnvUpdate.PORT_OPTION, true, "Set the port number the integration server will listen on");

        return options;
    }
}
