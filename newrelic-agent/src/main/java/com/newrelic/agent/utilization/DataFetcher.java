/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.google.common.io.Files;
import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.config.SystemPropertyProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.CharMatcher.ascii;

/**
 * Utilities for fetching utilization data.
 */
public class DataFetcher {

    protected static final Pattern LINUX_MEMORY_PATTERN = Pattern.compile("MemTotal: \\s+(\\d+)\\skB");
    protected static final Pattern LINUX_PROCESSOR_PATTERN = Pattern.compile("processor\\s*:\\s*([0-9]+)");

    /**
     * @return total physical memory in mebibytes, 0 if could not get total physical memory from OS.
     */
    public static Callable<Long> getTotalRamInMibCallable() {
        return new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                String os = ManagementFactory.getOperatingSystemMXBean().getName().toLowerCase();

                if (os.contains("linux")) {
                    // Read MemTotal from /proc/meminfo
                    // kB
                    String match = findLastMatchInFile(new File("/proc/meminfo"), LINUX_MEMORY_PATTERN);
                    if (match != null) {
                        long ramInkB = parseLongRam(match);
                        // kB * 1024 (bytes) / 1024^2 (bytes in mebibytes)
                        return ramInkB / 1024;
                    }
                } else if (os.contains("bsd")) {
                    // command: sysctl -n hw.realmem
                    // bytes
                    String output = executeCommand("sysctl -n hw.realmem");
                    long ramInBytes = parseLongRam(output);
                    return ramInBytes / (1024 * 1024);
                } else if (os.contains("mac")) {
                    // sysctl -n hw.memsize
                    // bytes
                    String output = executeCommand("sysctl -n hw.memsize");
                    long ramInBytes = parseLongRam(output);
                    return ramInBytes / (1024 * 1024);
                } else if (os.contains("windows")) {
                    // command output example (units is bytes):
                    // TotalPhysicalMemory
                    // 123456789
                    String output = executeCommand("wmic ComputerSystem get TotalPhysicalMemory").replaceFirst("TotalPhysicalMemory", "").trim();
                    long ramInBytes = parseLongRam(output);
                    return ramInBytes / (1024 * 1024);
                } else {
                    Agent.LOG.log(Level.FINER, "Could not get total physical memory for OS {0}", os);
                }

                return 0L;
            }
        };
    }

    /**
     * @return total logical processors on the OS.
     * Falls back to JVM if the count cannot be obtained from the OS.
     */
    public static int getLogicalProcessorCount() {
        String os = ManagementFactory.getOperatingSystemMXBean().getName().toLowerCase();

        if (os.contains("linux")) {
            final int linuxLogicalProcessors = getLinuxLogicalProcessors(new File("/proc/cpuinfo"));
            if (linuxLogicalProcessors > 0) {
                return linuxLogicalProcessors;
            }
        }
        // fallback: the number of processors the JVM can see. This is usually correct anyways.
        return ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    }

    static int getLinuxLogicalProcessors(File procCpuInfo) {
        String match = findLastMatchInFile(procCpuInfo, LINUX_PROCESSOR_PATTERN);
        if (null != match) {
            try {
                return Integer.parseInt(match) + 1; //processor num starts at zero
            } catch (NumberFormatException nfe) {
                Agent.LOG.log(Level.FINE, "Unable to parse linux processors. Found {0}", match);
            }
        }
        return 0;
    }

    /**
     * @return boot id, up to a limit of 128 characters on Linux, null for a non-Linux OS
     */
    public static String getBootId() {
        String os = ManagementFactory.getOperatingSystemMXBean().getName().toLowerCase();
        // boot_id MUST be collected in Linux environments, otherwise it MUST be excluded from the hash.
        if (os.contains("linux")) {
            try {
                String bootId = Files.asCharSource(new File("/proc/sys/kernel/random/boot_id"), StandardCharsets.UTF_8).read().trim();

                /* The agent MUST validate that /proc/sys/kernel/random/boot_id contains only ASCII characters. If the
                 * boot id contains non-ASCII characters, the agent MUST NOT send the boot_id key.
                 *
                 * The boot ID is expected to be 36 ASCII characters long. If the value is in an unexpected format, the
                 * key SHOULD be sent regardless of its form up to a limit of 128 ASCII characters. Longer values should
                 * be truncated to the first 128 ASCII characters.
                 *
                 * If the boot ID is not a 36-character string or is not ASCII, a warning should be logged and the
                 * Supportability/utilization/boot_id/error metric SHOULD be incremented. If the value is not found on a
                 * Linux system, the key MUST NOT be sent, a warning SHOULD be logged, and the
                 * Supportability/utilization/boot_id/error metric SHOULD be incremented.
                 */
                if (!ascii().matchesAllOf(bootId)) {
                    Agent.LOG.log(Level.FINE, "Non-ASCII characters in boot_id {0} for OS {1}", bootId, os);
                    recordBootIdError();
                    return null;
                } else if (bootId.length() < 36) {
                    Agent.LOG.log(Level.FINE, "Non-standard boot_id {0} for OS {1}", bootId, os);
                    recordBootIdError();
                } else if (bootId.length() > 128) {
                    Agent.LOG.log(Level.FINE, "Truncating boot_id {0} for OS {1}", bootId, os);
                    recordBootIdError();
                    bootId = bootId.substring(0, 128);
                }
                return bootId;
            } catch (IOException e) {
                recordBootIdError();
                Agent.LOG.log(Level.FINEST, e, "Exception occurred when reading boot_id file.");
            }
        }
        return null;
    }

    private static void recordBootIdError() {
        new CloudUtility().recordError(MetricNames.SUPPORTABILITY_BOOT_ID_ERROR);
    }

    /**
     * Returns the last matched backref of the pattern.
     * Assumes the pattern has one back reference.
     *
     * pp for tests
     */
    static String findLastMatchInFile(File file, Pattern lookFor) {
        if (file.exists() && file.canRead()) {
            FileInputStream fileInputStream;
            InputStreamReader inputStreamReader;
            BufferedReader reader = null;

            try {
                fileInputStream = new FileInputStream(file);
                inputStreamReader = new InputStreamReader(fileInputStream);
                reader = new BufferedReader(inputStreamReader);

                Matcher matcher = lookFor.matcher("");
                String line;
                String lastMatch = null;
                while ((line = reader.readLine()) != null) {
                    matcher.reset(line);
                    if (matcher.find()) {
                        lastMatch = matcher.group(1);
                    }
                }
                return lastMatch;
            } catch (FileNotFoundException e) {
                // FileInputStream failed
            } catch (IOException e) {
                // readLine failed
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                }
            }
        }

        Agent.LOG.log(Level.FINER, "Could not read file {0}", file.getName());
        return null;
    }

    protected static long parseLongRam(String number) {
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException e) {
            Agent.LOG.log(Level.FINE, "Unable to parse total memory available. Found {0}", number);
        }
        return 0;
    }

    static KubernetesData getKubernetesData(SystemPropertyProvider systemPropertyProvider) {
        try {
            return KubernetesData.extractKubernetesValues(systemPropertyProvider);
        } catch (Exception e) {
            Agent.LOG.log(Level.FINE, e, "Unable to extract kubernetes metadata.");
        }

        new CloudUtility().recordError(MetricNames.SUPPORTABILITY_KUBERNETES_ERROR);
        return null;
    }

    /**
     * Executes command in a subprocess.
     *
     * @return returns subprocess output, or an empty String if the subprocess execution failed
     */
    private static String executeCommand(String command) {
        StringBuffer output = new StringBuffer();

        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            BufferedReader procOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = procOutput.readLine()) != null) {
                output.append(line);
            }
            process.waitFor();
        } catch (IOException e) {
            // exec or readLine failed.
            Agent.LOG.log(Level.FINEST, e, "An exception occurred running subprocess cmd: {0}", command);
        } catch (InterruptedException e) {
            Agent.LOG.log(Level.FINER, "Subprocess cmd interrupted: {0}", command);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return output.toString();
    }

}
