/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.Agent;
import com.newrelic.agent.config.internal.SystemEnvironmentFacade;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Grabs the docker container id.
 * For newer Linux systems running cgroup v2, this is taken from /proc/self/mountinfo. The line should look like:
 *
 *   594 576 254:1 /docker/containers/f37a7e4d17017e7bf994656b19ca4360c6cdc4951c86700a464101d0d9ce97ef/hosts /etc/hosts rw,relatime - ext4 /dev/vda1 rw,discard
 *
 * For older Linux systems running cgroup v1, this is taken from /proc/self/cgroup. The line should look like:
 *
 *   +4:cpu:/docker/3ccfa00432798ff38f85839de1e396f771b4acbe9f4ddea0a761c39b9790a782
 *
 *   We should grab the "cpu" line. The long id number is the number we want.
 *
 * For AWS ECS (fargate and non-fargate) we check the metadata returned from the URL defined in either the
 * v3 or v4 metadata URL. These checks are only made if the metadata URL(s) are present in the target env variables.
 * The docker id returned in the metadata JSON response is a 32-digit hex followed by a 10-digit number in the "DockerId" key.
 *
 * In either case, this is the full docker id, not the short id that appears when you run a "docker ps".
 */
public class DockerData {

    private static final String FILE_WITH_CONTAINER_ID_V1 = "/proc/self/cgroup";
    private static final String FILE_WITH_CONTAINER_ID_V2 = "/proc/self/mountinfo";
    private static final String CPU = "cpu";

    private static final String AWS_ECS_METADATA_UNVERSIONED_ENV_VAR = "ECS_CONTAINER_METADATA_URI";
    private static final String AWS_ECS_METADATA_V4_ENV_VAR = "ECS_CONTAINER_METADATA_URI_V4";
    private static final String FARGATE_DOCKER_ID_KEY = "DockerId";

    private static final Pattern VALID_CONTAINER_ID = Pattern.compile("^[0-9a-f]{64}$");
    private static final Pattern DOCKER_CONTAINER_STRING_V1 = Pattern.compile("^.*[^0-9a-f]+([0-9a-f]{64,}).*");
    private static final Pattern DOCKER_CONTAINER_STRING_V2 = Pattern.compile(".*/docker/containers/([0-9a-f]{64,}).*");

    public String getDockerContainerIdForEcsFargate(boolean isLinux) {
        if (isLinux) {
            String result;

            // Try v4 ESC Fargate metadata call, then fallback to the un-versioned call
            String fargateUrl = null;
            try {
                fargateUrl = System.getenv(AWS_ECS_METADATA_V4_ENV_VAR);
                if (fargateUrl != null) {
                    Agent.LOG.log(Level.INFO, "Attempting to fetch ECS Fargate container id from URL (v4): {0}", fargateUrl);
                    result = retrieveDockerIdFromFargateMetadata(new AwsFargateMetadataFetcher(fargateUrl));
                    if (result != null) {
                        return result;
                    }
                }

                fargateUrl = System.getenv(AWS_ECS_METADATA_UNVERSIONED_ENV_VAR);
                if (fargateUrl != null) {
                    Agent.LOG.log(Level.INFO, "Attempting to fetch ECS Fargate container id from URL (unversioned): {0}", fargateUrl);
                    return retrieveDockerIdFromFargateMetadata(new AwsFargateMetadataFetcher(fargateUrl));
                }
            } catch (MalformedURLException e) {
                Agent.LOG.log(Level.FINEST, "Invalid AWS Fargate metadata URL: {0}", fargateUrl);
            }
        }

        return null;
    }

    public String getDockerContainerIdFromCGroups(boolean isLinux) {
        if (isLinux) {
            String result;
            //try to get the container id from the v2 location
            File containerIdFileV2 = new File(FILE_WITH_CONTAINER_ID_V2);
            result = getDockerIdFromFile(containerIdFileV2, CGroup.V2);
            if (result != null) {
                return result;
            }

            //try to get container id from the v1 location
            File containerIdFileV1 = new File(FILE_WITH_CONTAINER_ID_V1);
            result = getDockerIdFromFile(containerIdFileV1, CGroup.V1);
            if (result != null) {
                return result;
            }
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

   private  boolean checkLineAndGetResultV1(String line, StringBuilder resultGoesHere) {
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
    private boolean checkLineAndGetResultV2(String line, StringBuilder resultGoesHere) {
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

    /*
    * Get the match for the capture group of a single-capture-group regex expression.
     */
    private boolean checkAndGetMatch(Pattern p, StringBuilder result, String segment) {
        Matcher m = p.matcher(segment);
        if (m.matches() && m.groupCount() == 1) {
            result.append(m.group(1));
            return true;
        }
        return false;
    }

    @VisibleForTesting
    String retrieveDockerIdFromFargateMetadata(AwsFargateMetadataFetcher awsFargateMetadataFetcher) {
        String dockerId = null;
        StringBuilder jsonBlob = new StringBuilder();

        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(awsFargateMetadataFetcher.openStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBlob.append(line);
                }
            }

            JSONObject jsonObject = (JSONObject) new JSONParser().parse(jsonBlob.toString());
            dockerId = (String) jsonObject.get(FARGATE_DOCKER_ID_KEY);
            Agent.LOG.log(Level.INFO, "ECS Fargate container id: {0} ", dockerId);
        } catch (IOException e) {
            Agent.LOG.log(Level.WARNING, "Error opening input stream retrieving AWS Fargate metadata");
        } catch (ParseException e) {
            Agent.LOG.log(Level.WARNING, "Error parsing JSON blob for AWS Fargate metadata");
        }

        return dockerId;
    }

}
