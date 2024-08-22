/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.DebugFlag;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.MessageFormat;

/**
 * Utility class to find the New Relic configuration file.
 */
public class ConfigFileHelper {

    public static final String NEW_RELIC_YAML_FILE = "newrelic.yml";

    private static final String CONFIG_FILE_ENVIRONMENT_VARIABLE = "NEWRELIC_FILE";
    private static final String CONFIG_FILE_PROPERTY = "newrelic.config.file";
    private static final String NEW_RELIC_HOME_DIRECTORY_PROPERTY = "newrelic.home";
    private static final String NEW_RELIC_HOME_DIRECTORY_ENVIRONMENT_VARIABLE = "NEWRELIC_HOME";
    private static final String[] SEARCH_DIRECTORIES = { ".", "conf", "config", "etc" };

    private static enum ConfigFileLocationSpecifier {
        ENV_VAR("environment variable"),
        SYS_PROP("system property");

        private final String friendlyName;

        ConfigFileLocationSpecifier(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        public String getFriendlyName() {
            return this.friendlyName;
        }
    }

    /**
     * Find the New Relic configuration file.
     *
     * @return the configuration file or null
     */
    public static File findConfigFile() {
        File configFile = findFromEnvVariable();
        if (configFile != null) {
            return configFile;
        }

        configFile = findFromProperty();
        if (configFile != null) {
            return configFile;
        }

        File parentDir = getNewRelicDirectory();
        if (parentDir != null) {
            if (DebugFlag.DEBUG) {
                System.err.println(MessageFormat.format("New Relic home directory: {0}", parentDir));
            }

            configFile = findConfigFile(parentDir);
            if (configFile != null) {
                return configFile;
            }
        }

        return findConfigFileInWorkingDirectory();
    }

    /**
     * Find the New Relic home directory.
     *
     * @return the home directory or null
     */
    public static File getNewRelicDirectory() {
        File newRelicDir = findHomeDirectory();
        if (newRelicDir == null) {
            newRelicDir = AgentJarHelper.getAgentJarDirectory();
        }
        return newRelicDir;

    }

    /**
     * Find the configuration file from a environment variable.
     *
     * @return the configuration file or null
     */
    private static File findFromEnvVariable() {
        return getFileFromFilePath(System.getenv(CONFIG_FILE_ENVIRONMENT_VARIABLE), ConfigFileLocationSpecifier.ENV_VAR);
    }

    /**
     * Find the configuration file from a System property.
     *
     * @return the configuration file or null
     */
    private static File findFromProperty() {
        return getFileFromFilePath(System.getProperty(CONFIG_FILE_PROPERTY), ConfigFileLocationSpecifier.SYS_PROP);
    }

    @Nullable
    private static File getFileFromFilePath(String filePath, ConfigFileLocationSpecifier configFileLocationSpecifier) {
        if (filePath != null) {
            File configFile = new File(filePath);
            if (configFile.exists()) {
                return configFile;
            }
            System.err.println(MessageFormat.format(
                    "The configuration file {0} specified with the {1} [{2}] does not exist",
                    configFile.getAbsolutePath(), configFileLocationSpecifier.getFriendlyName(), filePath));
        }
        return null;
    }


    /**
     * Find the New Relic home directory.
     *
     * @return the New Relic home directory or null
     */
    private static File findHomeDirectory() {
        File homeDir = findHomeDirectoryFromProperty();
        if (homeDir != null) {
            return homeDir;
        }

        homeDir = findHomeDirectoryFromEnvironmentVariable();
        if (homeDir != null) {
            return homeDir;
        }

        return null;
    }

    /**
     * Find the New Relic home directory from a System property.
     *
     * @return the New Relic home directory or null
     */
    private static File findHomeDirectoryFromProperty() {
        String filePath = System.getProperty(NEW_RELIC_HOME_DIRECTORY_PROPERTY);
        if (filePath != null) {
            File homeDir = new File(filePath);
            if (homeDir.exists()) {
                return homeDir;
            }
            System.err.println(MessageFormat.format("The directory {0} specified with the {1} property does not exist",
                    homeDir.getAbsolutePath(), NEW_RELIC_HOME_DIRECTORY_PROPERTY));
        }
        return null;
    }

    /**
     * Find the New Relic home directory from a System environment variable.
     *
     * @return the New Relic home directory or null
     */
    private static File findHomeDirectoryFromEnvironmentVariable() {
        String filePath = System.getenv(NEW_RELIC_HOME_DIRECTORY_ENVIRONMENT_VARIABLE);
        if (filePath != null) {
            File homeDir = new File(filePath);
            if (homeDir.exists()) {
                return homeDir;
            }
            System.err.println(MessageFormat.format(
                    "The directory {0} specified with the {1} environment variable does not exist",
                    homeDir.getAbsolutePath(), NEW_RELIC_HOME_DIRECTORY_ENVIRONMENT_VARIABLE));
        }
        return null;
    }

    /**
     * Search for the configuration file in the given directory and specific sub-directories.
     *
     * @param parentDirectory the directory to begin searching
     * @return the configuration file or null
     */
    private static File findConfigFile(File parentDirectory) {
        for (String searchDir : SEARCH_DIRECTORIES) {
            File configDir = new File(parentDirectory, searchDir);
            if (DebugFlag.DEBUG) {
                System.err.println(MessageFormat.format("Searching for New Relic configuration in directory {0}", configDir));
            }
            if (configDir.exists()) {
                File configFile = new File(configDir, NEW_RELIC_YAML_FILE);
                if (configFile.exists()) {
                    return configFile;
                }
            }
        }
        return null;
    }

    /**
     * Find the configuration file in the working directory.
     *
     * @return the configuration file or null
     */
    private static File findConfigFileInWorkingDirectory() {
        File configFile = new File(NEW_RELIC_YAML_FILE);
        if (configFile.exists()) {
            return configFile;
        }
        return null;
    }

}
