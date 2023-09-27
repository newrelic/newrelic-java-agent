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
 *   We should grab the "cpu" line. The long id number is the number we want.
 *   This is the full docker id, not the short id that appears when you run a "docker ps".
 */
public class DockerData {

    private static final String FILE_WITH_CONTAINER_ID_V1 = "/proc/self/cgroup";
    private static final String FILE_WITH_CONTAINER_ID_V2 = "/proc/self/mountinfo";
    private static final String CPU = "cpu";

    private static final Pattern VALID_CONTAINER_ID = Pattern.compile("^[0-9a-f]{64}$");
    private static final Pattern DOCKER_CONTAINER_STRING_V1 = Pattern.compile("^.*[^0-9a-f]+([0-9a-f]{64,}).*");
    private static final Pattern DOCKER_CONTAINER_STRING_V2 = Pattern.compile(".*/docker/containers/([0-9a-f]{64,}).*");

    public String getDockerContainerId(boolean isLinux) {
        if (isLinux) {
            //try to get the container id from the v2 location
            File containerIdFileV2 = new File(FILE_WITH_CONTAINER_ID_V2);
            String idResultV2 = getDockerIdFromFile(containerIdFileV2, CGroup.V2);
            if (idResultV2 != null) {
                return idResultV2;
            }
            //try to get container id from the v1 location
            File containerIdFileV1 = new File(FILE_WITH_CONTAINER_ID_V1);
            return getDockerIdFromFile(containerIdFileV1, CGroup.V1);
        }
        return null;
    }

    String getDockerIdFromFile(File mountInfoFile, CGroup cgroup) {
        if (mountInfoFile.exists() && mountInfoFile.canRead()) {
            try {
                return readFile(new FileReader(mountInfoFile), cgroup);
            } catch (FileNotFoundException e) {
            }
        }
        return null;
    }

    String readFile(Reader reader, CGroup cgroup) {
        try (BufferedReader bReader = new BufferedReader(reader)) {
            String line;
            StringBuilder resultGoesHere = new StringBuilder();
            while ((line = bReader.readLine()) != null) {
                if (checkLineAndGetResult(line, resultGoesHere, cgroup)) {
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

    boolean checkLineAndGetResult(String line, StringBuilder resultGoesHere, CGroup cgroup) {
        if (cgroup == CGroup.V1) {
            return checkLineAndGetResultV1(line, resultGoesHere);
        } else if (cgroup == CGroup.V2) {
            return checkLineAndGetResultV2(line, resultGoesHere);
        }
        return false;
    }

    boolean checkLineAndGetResultV1(String line, StringBuilder resultGoesHere) {
        String[] parts = line.split(":");
        if (parts.length == 3 && validCpuLine(parts[1])) {
            String mayContainId = parts[2];
            if (checkAndGetMatch(DOCKER_CONTAINER_STRING_V1, resultGoesHere, mayContainId)) {
                return true;
            } else if (!mayContainId.equals("/")) {
                Agent.LOG.log(Level.FINE, "Docker Data: Ignoring unrecognized cgroup ID format: {0}", mayContainId);
            }
        }
        return false;
    }
    boolean checkLineAndGetResultV2(String line, StringBuilder resultGoesHere) {
        String[] parts = line.split(" ");
        if (parts.length >= 4 ) {
            String mayContainId = parts[3];
            if (checkAndGetMatch(DOCKER_CONTAINER_STRING_V2, resultGoesHere, mayContainId)) {
                return true;
            }
        }
        return false;
    }

    boolean isInvalidDockerValue(String value) {
        /*
         * Value should be exactly 64 characters.
         *
         * Value is expected to include only [0-9a-f]
         */
        return (value == null) || (!VALID_CONTAINER_ID.matcher(value).matches());
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

    //Get the match for the capture group of a single-capture-group regex expression.
    private boolean checkAndGetMatch(Pattern p, StringBuilder result, String segment) {
        Matcher m = p.matcher(segment);
        if (m.matches() && m.groupCount() == 1) {
            result.append(m.group(1));
            return true;
        }
        return false;
    }

    //CGROUPS v1 logic

//    String getDockerIdFromFile(File cpuInfoFile) {
//        if (cpuInfoFile.exists() && cpuInfoFile.canRead()) {
//            try {
//                return readFile(new FileReader(cpuInfoFile));
//            } catch (FileNotFoundException e) {
//            }
//        }
//        return null;
//    }

    /*
     * protected for testing.
     *
     * Returns the docker id as a string, or null if this value could not be read, or failed validation.
//     */
//    String readFile(Reader reader) {
//        try (BufferedReader bReader = new BufferedReader(reader)) {
//
//            String line;
//            StringBuilder resultGoesHere = new StringBuilder();
//            while ((line = bReader.readLine()) != null) {
//                if (checkLineAndGetResult(line, resultGoesHere)) {
//                    String value = resultGoesHere.toString().trim();
//                    if (isInvalidDockerValue(value)) {
//                        Agent.LOG.log(Level.WARNING, MessageFormat.format("Failed to validate Docker value {0}", value));
//                        return null;
//                    }
//                    return value;
//                }
//            }
//        } catch (Throwable e) {
//            Agent.LOG.log(Level.FINEST, e, "Exception occurred when reading docker file.");
//        }
//        return null;
//    }
    //    String cgroupV2GetDockerIdFromFile(File mountInfoFile) {
//        if (mountInfoFile.exists() && mountInfoFile.canRead()) {
//            try {
//                return cgroupV2ReadFile(new FileReader(mountInfoFile));
//            } catch (FileNotFoundException e) {
//            }
//        }
//        return null;
//    }
//
//    String cgroupV2ReadFile(Reader reader) {
//        try (BufferedReader bReader = new BufferedReader(reader)) {
//            String line;
//            StringBuilder resultGoesHere = new StringBuilder();
//            while ((line = bReader.readLine()) != null) {
//                //check the first line with the docker/containers/ tag.
//                if (cgroupV2CheckLineAndGetResult(line, resultGoesHere)) {
//                    String value = resultGoesHere.toString().trim();
//                    if (isInvalidDockerValue(value)) {
//                        Agent.LOG.log(Level.WARNING, MessageFormat.format("Failed to validate Docker value {0}", value));
//                        return null;
//                    }
//                    return value;
//                }
//            }
//        } catch (Throwable e) {
//            Agent.LOG.log(Level.FINEST, e, "Exception occurred when reading docker file.");
//        }
//        return null;
//    }


}
