/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent;

import com.newrelic.agent.config.AgentConfigImpl;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class InitConfigEnvUpdate {
    public static final String LICENSE_KEY_OPTION = "l";
    public static final String APP_NAME_OPTION = "a";
    public static final String CONFIG_FILE_LOCATION_OPTION = "c";
    public static final String PORT_OPTION = "p";
    private static final String AGENT_CLASS = "com.newrelic.agent.Agent";
    //private static final String TARGET_ENV_FILE = "/etc/environment";
    private static final String TARGET_ENV_FILE = "/Users/jduffy/tmp/etc/environment";
    private static final String JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS";
    private static final String CONFIG_FILE_ENV_KEY = "NEW_RELIC_CONFIG_FILE";
    private static final String AGENT_CONFIG_FILE = "newrelic.yml";
    private static final String JAVA_AGENT_OPTION = "-javaagent:";
    private static final String NEWRELIC_JAR = "newrelic.jar";

    /*
     * Exit codes:
     * 100 - Unable to derive New Relic agent jar location
     * 101 - Target environment config file not found
     * 102 - Target environment config file is not readable
     * 103 - Target environment config file is not writable
     * 104 - An IOException occurred reading or writing the target environment file
     * 105 - The -javaagent JVM parameter exists with a reference to the NR agent jar file
     * 106 - The supplied config file path doesn't exist or is not writable
     * 107 - Config file location or license key value is missing
     */
    public static void updateEnvInitialConfigOptions(CommandLine cmdLine) {
        String configFileLocation = cmdLine.getOptionValue(CONFIG_FILE_LOCATION_OPTION);
        String licenseKey = cmdLine.getOptionValue(LICENSE_KEY_OPTION);
        String appName = cmdLine.getOptionValue(APP_NAME_OPTION);
        int port = portNumberFromCommandLineOption(cmdLine.getOptionValue(PORT_OPTION));
        int exitCode;

        if (StringUtils.isNotEmpty(configFileLocation) && StringUtils.isNotEmpty(licenseKey)) {
            configFileLocation = configFileLocation.endsWith("/") ? configFileLocation : configFileLocation + "/";

            logMessage("Config file location: " + configFileLocation);
            logMessage("License key supplied");
            logMessage("App name: " + (StringUtils.isEmpty(appName) ? "n/a" : appName));
            logMessage("Port: " + (port == 0 ? "n/a" : port));

            exitCode = writeInitialConfigFile(configFileLocation, licenseKey, appName, port);
            if (exitCode == 0) {
                exitCode = updateEnvFileWithToolOptions(configFileLocation);
            }
        } else {
            exitCode = 107;
        }

        logMessage("Exiting with code: " + exitCode);
        System.exit(exitCode);
    }

    private static int writeInitialConfigFile(String path, String licenseKey, String appName, int port) {
        Path configFilePath = Paths.get(path);
        if (configFilePath.toFile().exists() && Files.isWritable(configFilePath)) {
            Path ymlFile = Paths.get(path, AGENT_CONFIG_FILE);
            String ymlContents = "# Generated via Java agent init-config parameter\n" +
                    "common: &default_settings\n  " +
                    "license_key: '" + licenseKey +
                    "'\n  app_name: " + (appName == null ? "" : appName) + "\n\n" +
                    "  http_integration_server:\n" +
                    "    enabled: true\n" +
                    (port == 0 ? "" : "    port: " + port) + "\n\n";
            logMessage("Wrote initial agent config file to " + ymlFile);

            try {
                Files.write(ymlFile, ymlContents.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                logMessage("Config file path either does not exist or is not writable");
                return 106;
            }
        }

        return 0;
    }
    private static int updateEnvFileWithToolOptions(String configFilePath) {
        Path path = Paths.get(TARGET_ENV_FILE);
        String agentJarLocation = getAgentJarLocation();
        int resultCode;

        if (agentJarLocation == null) {
            resultCode = 100;
        } else {
            resultCode = validateEnvFile(path);
            if (resultCode == 0) {
                try {
                    String envFileContents = readEnvFileToString(path);

                    if (!doesEnvFileAlreadyContainNewRelicJavaAgentRef(envFileContents)) {
                        String contents = readEnvFileToString(path) + "\n" +
                                JAVA_TOOL_OPTIONS + "=" +
                                (doesEnvFileAlreadyContainJavaToolOpts(envFileContents) ? "$" + JAVA_TOOL_OPTIONS + " " : "") +
                                JAVA_AGENT_OPTION + agentJarLocation + "\n" +
                                CONFIG_FILE_ENV_KEY + "=" + configFilePath + AGENT_CONFIG_FILE;

                        System.out.println(contents);
                        writeUpdatedEnvFile(path, contents);
                    } else {
                        resultCode = 105;
                    }
                } catch (IOException e) {
                    resultCode = 104;
                }
            }
        }

        return resultCode;
    }

    private static String getAgentJarLocation() {
        try {
            URL url = Class.forName(AGENT_CLASS).getProtectionDomain().getCodeSource().getLocation();
            return Paths.get(url.toURI()).toString();
        } catch (ClassNotFoundException | URISyntaxException notFoundException) {
            return null;
        }
    }

    private static int validateEnvFile(Path path) {
        int exitCode = 0;

        if (!Files.exists(path)) {
            exitCode = 101;
        } else if (!Files.isReadable(path)) {
            exitCode = 102;
        } else if (!Files.isWritable(path)) {
            exitCode = 103;
        }

        return exitCode;
    }

    private static boolean doesEnvFileAlreadyContainJavaToolOpts(String contents) throws IOException {
        return contents.contains(JAVA_TOOL_OPTIONS);
    }

    private static boolean doesEnvFileAlreadyContainNewRelicJavaAgentRef(String contents) throws IOException {
        return contents.contains(JAVA_AGENT_OPTION) && contents.contains(NEWRELIC_JAR);
    }

    private static String readEnvFileToString(Path path) throws IOException {
        StringBuilder contents = new StringBuilder();

        List<String> fileLines = Files.readAllLines(path);
        for (String line : fileLines) {
            contents.append(line).append("\n");
        }

        return contents.toString();
    }

    private static void writeUpdatedEnvFile(Path path, String contents) throws IOException {
        Files.write(path, contents.getBytes(StandardCharsets.UTF_8));
    }

    private static int portNumberFromCommandLineOption(String value) {
        int port = 0;
        if (StringUtils.isNotBlank(value)) {
            try {
                port = Integer.parseInt(value);
            } catch (NumberFormatException nfe) {
                logMessage("Invalid port value supplied: " + value);
            }
        }

        return port;
    }

    private static void logMessage(String msg) {
        System.out.println(msg);
    }
}
