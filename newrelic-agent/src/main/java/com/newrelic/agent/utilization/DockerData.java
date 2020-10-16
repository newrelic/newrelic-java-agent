/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.newrelic.agent.Agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Grabs the docker container id from the file /proc/self/cgroup. The lines look something like below:
 *
 *   +4:cpu:/docker/3ccfa00432798ff38f85839de1e396f771b4acbe9f4ddea0a761c39b9790a782
 *
 *   We should grab the "cpu" line. It should only be used if /docker is present. The long id number is the number we want.
 *   This is the full docker id, not the short id that appears when you run a "docker ps".
 */
public class DockerData {

    private static final String FILE_WITH_CONTAINER_ID = "/proc/self/cgroup";
    private static final String CPU = "cpu";

    private static final Pattern DOCKER_NATIVE_DRIVER_WOUT_SYSTEMD = Pattern.compile("^/.*/([0-9a-f]+)$");
    private static final Pattern DOCKER_GENERIC_DRIVER = Pattern.compile("^/([0-9a-f]+)$");
    private static final Pattern DOCKER_NATIVE_DRIVER_W_SYSTEMD = Pattern.compile("^/.*/\\w+-([0-9a-f]+)\\.scope$");

    public String getDockerContainerId(boolean isLinux) {
        if (isLinux) {
            File cpuInfoFile;
            cpuInfoFile = new File(FILE_WITH_CONTAINER_ID);
            return getDockerIdFromFile(cpuInfoFile);
        }
        return null;
    }

    String getDockerIdFromFile(File cpuInfoFile) {
        if (cpuInfoFile.exists() && cpuInfoFile.canRead()) {
            try {
                return readFile(new FileReader(cpuInfoFile));
            } catch (FileNotFoundException e) {
            }
        }
        return null;
    }

    /*
     * protected for testing.
     *
     * Returns the docker id as a string, or null if this value could not be read, or failed validation.
     */
    String readFile(Reader reader) {
        try (BufferedReader bReader = new BufferedReader(reader)) {

            String line;
            StringBuilder resultGoesHere = new StringBuilder();
            while ((line = bReader.readLine()) != null) {
                if (checkLineAndGetResult(line, resultGoesHere)) {
                    String value = resultGoesHere.toString().trim();
                    if (isInvalidDockerValue(value)) {
                        Agent.LOG.log(Level.WARNING, MessageFormat.format("Failed to validate Docker value {0}", value));
                        return null;
                    }
                    return value;
                }
            }
        } catch (Throwable e) {
            Agent.LOG.log(Level.FINEST, e, "Exception occurred when reading docker file.");
        }
        return null;
    }

    boolean isInvalidDockerValue(String value) {
        /*
         * Value should be exactly 64 characters.
         *
         * Value is expected to include only [0-9a-f]
         */

        if (value == null) {
            return true;
        }

        if (value.length() != 64) {
            return true;
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c >= '0' && c <= '9') {
                continue;
            }

            if (c >= 'a' && c <= 'f') {
                continue;
            }

            // Invalid character
            return true;
        }

        return false;
    }

    // protected for testing
    boolean checkLineAndGetResult(String line, StringBuilder resultGoesHere) {
        String[] parts = line.split(":");
        if (parts.length == 3 && validCpuLine(parts[1])) {
            String mayContainId = parts[2];
            if (checkAndGetMatch(DOCKER_NATIVE_DRIVER_W_SYSTEMD, resultGoesHere, mayContainId)) {
                return true;
            } else if (checkAndGetMatch(DOCKER_NATIVE_DRIVER_WOUT_SYSTEMD, resultGoesHere, mayContainId)) {
                return true;
            } else if (checkAndGetMatch(DOCKER_GENERIC_DRIVER, resultGoesHere, mayContainId)) {
                return true;
            } else if (!mayContainId.equals("/")) {
                Agent.LOG.log(Level.FINE, "Docker Data: Ignoring unrecognized cgroup ID format: {0}", mayContainId);
            }
        }
        return false;
    }

    private boolean validCpuLine(String segment) {
        if (segment != null) {
            String[] parts = segment.split(",");
            for (String current : parts) {
                if (current.equals(CPU)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkAndGetMatch(Pattern p, StringBuilder result, String segment) {
        Matcher m = p.matcher(segment);
        if (m.matches() && m.groupCount() == 1) {
            result.append(m.group(1));
            return true;
        }
        return false;
    }

}
