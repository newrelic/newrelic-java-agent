/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.google.common.base.Strings;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Mocks;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.SystemPropertyFactory;
import com.newrelic.agent.config.SystemPropertyProvider;
import com.newrelic.agent.config.UtilizationDataConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.utilization.AWS.AwsData;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilizationCrossAgentTests {
    private static final Pattern PROC_FILE_PATTERN = Pattern.compile(".+pack_.+core_([0-9]+)logical.txt");
    private static final Map<String, String> TEST_ENV_VARS = new HashMap<>();
    private static SystemPropertyProvider originalProvider;

    @BeforeClass
    public static void beforeClass() {
        originalProvider = SystemPropertyFactory.getSystemPropertyProvider();
    }

    @AfterClass
    public static void afterClass() {
        SystemPropertyFactory.setSystemPropertyProvider(originalProvider);
    }

    private void setEnvVar(String key, String val) {
        TEST_ENV_VARS.put(key, val);
        SystemPropertyProvider testProvider = Mocks.createSystemPropertyProvider(new HashMap<String, String>(), TEST_ENV_VARS);
        SystemPropertyFactory.setSystemPropertyProvider(testProvider);
    }

    /*
     * Create a mock service manager with a config service with the given utilization values
     */
    private static void setConfig(String billingHostname, Long totalRam, Integer logicalProcessors) {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> values = new HashMap<>();
        values.put("billing_hostname", billingHostname);
        values.put("total_ram_mib", totalRam);
        values.put("logical_processors", logicalProcessors);
        config.put(UtilizationDataConfig.PROPERTY_NAME, values);
        config.put("app_name", "CATs");
        ConfigService configService = new MockConfigService(AgentConfigImpl.createAgentConfig(config));
        MockServiceManager mockServiceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(mockServiceManager);
    }

    @Test
    public void testCrossAgentUtilizationMemory() {
        File file = AgentHelper.getFile("com/newrelic/agent/cross_agent_tests/proc_meminfo/meminfo_4096MB.txt");
        // 4194304 MiB = 4096 MB
        Assert.assertEquals(4096, (Integer.parseInt(DataFetcher.findLastMatchInFile(file, DataFetcher.LINUX_MEMORY_PATTERN)) / 1024));
    }

    @Test
    public void testCrossAgentUtilizationCPU() {
        List<File> mockProcFiles = AgentHelper.getFiles("com/newrelic/agent/cross_agent_tests/proc_cpuinfo/");
        int numCrossAgentTestsRan = 0;
        for (File mockProcFile : mockProcFiles) {
            if (mockProcFile.getName().equals("README.md")) {
                continue;
            }
            testProcFile(mockProcFile);
            numCrossAgentTestsRan++;
        }
        Assert.assertTrue("At least two cross-agent-tests must be run.", numCrossAgentTestsRan >= 2);
    }

    private void testProcFile(File procFile) {
        Matcher matcher = PROC_FILE_PATTERN.matcher(procFile.getName());
        int logicalProcessors = 0;
        if (matcher.find()) {
            // can parse expected number of logical processors
            logicalProcessors = Integer.parseInt(matcher.group(1));
        }
        Assert.assertEquals(logicalProcessors, DataFetcher.getLinuxLogicalProcessors(procFile));
    }

    @Test
    public void testUtilizationFiles() throws Exception {
        List<File> files = new ArrayList<>();
        files.addAll(AgentHelper.getFiles("com/newrelic/agent/cross_agent_tests/utilization/"));
        files.addAll(AgentHelper.getFiles("com/newrelic/agent/uncross_agent_tests/utilization/"));
        int numTestsRan = 0;
        for (File current : files) {
            if (current.getName().endsWith(".json")) {
                testUtilizationFile(current);
                numTestsRan++;
            }
        }
        Assert.assertTrue("At least one cross-agent-test must be run.", numTestsRan >= 1);
    }

    private void testUtilizationFile(File current) throws Exception {
        JSONArray tests = readJsonAndGetTests(current);
        for (Object currentTest : tests) {
            String vendorType;
            JSONObject expectedOutputJson = ((JSONObject) ((JSONObject) currentTest).get("expected_output_json"));
            if (expectedOutputJson.containsKey("vendors")) {
                vendorType = (String) ((JSONObject) expectedOutputJson.get("vendors")).keySet().toArray()[0];
            } else {
                vendorType = "";
            }
            runTest((JSONObject) currentTest, vendorType);
            SystemPropertyProvider testProvider = Mocks.createSystemPropertyProvider(new HashMap<String, String>(), new HashMap<String, String>());
            SystemPropertyFactory.setSystemPropertyProvider(testProvider);
            TEST_ENV_VARS.clear();
        }
    }

    private void runTest(JSONObject jsonTest, String type) throws ParseException {
        String testname = (String) jsonTest.get("testname");

        // input that the agent fetches from the host
        Long total_ram_mib = null == jsonTest.get("input_total_ram_mib") ? null : (Long) jsonTest.get("input_total_ram_mib");
        Integer logical_processors = null == jsonTest.get("input_logical_processors") ? null : Integer.valueOf(jsonTest.get("input_logical_processors") + "");
        String hostname = (String) jsonTest.get("input_hostname");
        String fullHostname = (String) jsonTest.get("input_full_hostname");
        ArrayList ipAddress = (ArrayList) jsonTest.get("input_ip_address");
        String boot_id = (String) jsonTest.get("input_boot_id");

        if (Strings.isNullOrEmpty(boot_id)) {
            boot_id = null;
        } else if (boot_id.length() > 128) {
            // mimic the truncation that DataFetcher does to boot_id
            boot_id = boot_id.substring(0, 128);
        }

        // no cross-agent tests for docker yet.
        String containerId = null;
        String ecsFargateDockerId = null;

        CloudData data = null;

        if (type.equals("aws")) {
            data = new AwsData((String) jsonTest.get("input_aws_id"), (String) jsonTest.get("input_aws_type"), (String) jsonTest.get("input_aws_zone"));
        } else if (type.equals("azure")) {
            data = new Azure.AzureData((String) jsonTest.get("input_azure_location"), (String) jsonTest.get("input_azure_name"),
                    (String) jsonTest.get("input_azure_id"), (String) jsonTest.get("input_azure_size"));
        } else if (type.equals("gcp")) {
            // mimic the parsing that GCP does to get the last segments of zone and machineType
            String zone = (String) jsonTest.get("input_gcp_zone");
            zone = zone.substring(zone.lastIndexOf('/') + 1);

            String machineType = (String) jsonTest.get("input_gcp_type");
            machineType = machineType.substring(machineType.lastIndexOf('/') + 1);

            data = new GCP.GcpData((String) jsonTest.get("input_gcp_id"), machineType, (String) jsonTest.get("input_gcp_name"), zone);
        } else if (type.equals("pcf")) {
            data = new PCF.PcfData((String) jsonTest.get("input_pcf_guid"), (String) jsonTest.get("input_pcf_ip"),
                    (String) jsonTest.get("input_pcf_mem_limit"));
        } else {
            data = AwsData.EMPTY_DATA;
        }

        Map<String, Object> expectedOutput = (Map<String, Object>) jsonTest.get("expected_output_json");

        { // set env variables
            Map<String, Object> envVars = (Map<String, Object>) jsonTest.get("input_environment_variables");
            if (null != envVars) {
                for (Map.Entry<String, Object> entry : envVars.entrySet()) {
                    setEnvVar(entry.getKey(), null == entry.getValue() ? null : String.valueOf(entry.getValue()));
                }
            }
        }
        SystemPropertyProvider systemPropertyProvider = SystemPropertyFactory.getSystemPropertyProvider();

        { // set up config service
            Map<String, Object> configuredInput = (Map<String, Object>) jsonTest.get("input_configured");
            if (null != configuredInput) {
                setConfig((String) configuredInput.get("billing_hostname"),
                        configuredInput.get("total_ram_mib") == null ? null : (Long) configuredInput.get("total_ram_mib"),
                        configuredInput.get("logical_processors") == null ? null : Integer.valueOf(String.valueOf(configuredInput.get("logical_processors"))));
            } else {
                setConfig(null, null, null);
            }
        }

        ArrayList<String> addresses = new ArrayList<>();
        if (ipAddress != null && ipAddress.size() > 0) {
            addresses.addAll(ipAddress);
        }

        UtilizationData utilizationData = new UtilizationData(hostname, fullHostname, addresses, logical_processors, containerId, ecsFargateDockerId,
                boot_id, data, total_ram_mib, UtilizationConfig.createFromConfigService(), KubernetesData.extractKubernetesValues(systemPropertyProvider));
        Assert.assertEquals("cross agent test '" + testname + "' failed.", expectedOutput, toJSONObject(utilizationData.map()));
    }

    // consider moving these into some type of CrossAgentUtils class
    private static JSONArray readJsonAndGetTests(File file) throws Exception {
        JSONParser parser = new JSONParser();
        FileReader fr = null;
        JSONArray theTests = null;
        try {
            fr = new FileReader(file);
            theTests = (JSONArray) parser.parse(fr);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                }
            }
        }
        return theTests;
    }

    private static JSONObject toJSONObject(Map map) throws ParseException {
        JSONParser parser = new JSONParser();
        String jsonString = JSONObject.toJSONString(map);
        System.out.println(jsonString);
        return (JSONObject) parser.parse(jsonString);
    }

}
