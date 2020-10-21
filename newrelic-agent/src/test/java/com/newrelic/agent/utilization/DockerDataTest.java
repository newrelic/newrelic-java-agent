/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockerDataTest {

    private final DockerData dockerData = new DockerData();

    @Test
    public void testGetDockerIdNotLinux() {
        Assert.assertNull(dockerData.getDockerContainerId(false));
    }

    @Test
    public void testCheckLineAndGetIdValid() {
        StringBuilder sb = new StringBuilder();
        String line = "4:cpu:/docker/f37a7e4d17017e7bf774656b19ca4360c6cdc4951c86700a464101d0d9ce97ee";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb));
        Assert.assertEquals("f37a7e4d17017e7bf774656b19ca4360c6cdc4951c86700a464101d0d9ce97ee", sb.toString());

        sb = new StringBuilder();
        line = "3:cpu:/lxc/cb8c113e5f3cf8332f5231f8154adc429ea910f7c29995372de4f571c55d3159";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb));
        Assert.assertEquals("cb8c113e5f3cf8332f5231f8154adc429ea910f7c29995372de4f571c55d3159", sb.toString());

        sb = new StringBuilder();
        line = "3:cpu,memory:/lxc/cb8c113e5f3cf8332f5231f8154adc429ea910f7c29995372de4f571c55d3159";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb));
        Assert.assertEquals("cb8c113e5f3cf8332f5231f8154adc429ea910f7c29995372de4f571c55d3159", sb.toString());

        sb = new StringBuilder();
        line = "3:cpuacct,cpu:/system.slice/docker-67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f.scope";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb));
        Assert.assertEquals("67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f", sb.toString());

        sb = new StringBuilder();
        line = "3:cpu:/system.slice/docker-67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f.scope";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb));
        Assert.assertEquals("67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f", sb.toString());

        sb = new StringBuilder();
        line = "2:cpu:/docker/47cbd16b77c50cbf71401c069cd2189f0e659af17d5a2daca3bddf59d8a870b2";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb));
        Assert.assertEquals("47cbd16b77c50cbf71401c069cd2189f0e659af17d5a2daca3bddf59d8a870b2", sb.toString());

        sb = new StringBuilder();
        line = "2:cpu:/47cbd16b77c50cbf71401c069cd2189f0e659af17d5a2daca3bddf59d8a870b2";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb));
        Assert.assertEquals("47cbd16b77c50cbf71401c069cd2189f0e659af17d5a2daca3bddf59d8a870b2", sb.toString());

        sb = new StringBuilder();
        line = "2:cpu:/kubepods.slice/kubepods-burstable.slice/kubepods-burstable-pod917d7891_0d63_11ea_873f_005056993b36.slice/docker-92b21022a0cecd0212c5097aa63ed8248ed832902dbdbb654e6df95e05573646.scope";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb));
        Assert.assertEquals("92b21022a0cecd0212c5097aa63ed8248ed832902dbdbb654e6df95e05573646", sb.toString());

        sb = new StringBuilder();
        line = "1:cpu:/system.slice/crio-67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f.scope";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb));
        Assert.assertEquals("67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f", sb.toString());
    }

    @Test
    public void testCheckLineAndGetIdInValid() {
        StringBuilder sb;
        String line;

        sb = new StringBuilder();
        // it should be cpu not cpuacct
        line = "3:cpuacct:/lxc/cb8c113e5f3cf8332f5231f8154adc429ea910f7c29995372de4f571c55d3159";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        sb = new StringBuilder();
        // it should be cpu not cpuacct
        line = "3:cpuacct:/cb8c113e5f3cf8332f5231f8154adc429ea910f7c29995372de4f571c55d3159";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        sb = new StringBuilder();
        // no .scope
        line = "3:cpuacct,cpu:/system.slice/docker-67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        sb = new StringBuilder();
        line = "2:cpuset:/docker/47cbd16b77c50cbf71401c069cd2189f0e659af17d5a2daca3bddf59d8a870b2";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        sb = new StringBuilder();
        line = "2:cpuset:/47cbd16b77c50cbf71401c069cd2189f0e659af17d5a2daca3bddf59d8a870b2";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        sb = new StringBuilder();
        line = "1:hugetlb,perf_event,blkio,freezer,devices,memory,cpuacct,cpu,cpuset:/lxc/b6d196c1-50f2-4949-abdb-5d4909864487";
        // id not grabbed becase of slashes (-)
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        sb = new StringBuilder();
        line = "1:hugetlb,perf_event,blkio,freezer,devices,memory,cpuacct,cpu,cpuset:/b6d196c1-50f2-4949-abdb-5d4909864487";
        // id not grabbed becase of slashes (-)
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        sb = new StringBuilder();
        line = "4:cpu:/lxc/p1";
        // id not grabbed because p not a valid id character
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        sb = new StringBuilder();
        line = "4:cpu:/p1";
        // id not grabbed because p not a valid id character
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        line = "4:cpu:";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        line = "4:cpu:";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        line = "4:cpu:/";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        line = "5:";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        line = ":";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        line = "::";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        line = "2:,:/";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));

        line = "2;,;/";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb));
    }

    @Test
    public void testGetDockerIdFromFiles() throws Exception {
        Map<String, String> tests = testToAnswers(AgentHelper.getFile("com/newrelic/agent/cross_agent_tests/docker_container_id/cases.json"));
        List<File> files = AgentHelper.getFiles("com/newrelic/agent/cross_agent_tests/docker_container_id/");
        if (files == null || files.isEmpty()) {
            Assert.fail("There were no files read in for testing.");
        }
        int testCount = 0;
        for (File current : files) {
            if (current.getName().toLowerCase().endsWith((".txt"))) {
                String name = current.getName();
                Assert.assertTrue("The test file name " + name + " was not found in the list of tests", tests.containsKey(name));
                String answer = tests.get(name);
                processFile(current, answer);
                testCount++;
            }
        }

        Assert.assertEquals("The number of tests files does not match the number of tests", testCount, tests.size());
    }

    @Test
    public void testValidDockerValue() {
        String validId = "aabcdefabcdefabcdefabcdefbcdefabcdef123456890123456890123456890f";
        Assert.assertTrue(validId.length() == 64);
        Assert.assertFalse(dockerData.isInvalidDockerValue(validId));
    }

    @Test
    public void testInvalidDockerValue() {
        String invalidId = "AABCDEFABCDEFABCDEFABCDEFBCDEFABCDEF123456890123456890123456890F";
        Assert.assertTrue(invalidId.length() == 64);
        Assert.assertTrue(dockerData.isInvalidDockerValue(invalidId));
    }

    @Test
    public void testInvalidDockerValueCharacters() {
        String invalidValue = "138339393939393939393939393939393939389\uD83D\uDCBB abcd999012345aaaaaaaaa";
        Assert.assertTrue(invalidValue.length() == 64);
        Assert.assertTrue(dockerData.isInvalidDockerValue(invalidValue));
    }

    @Test
    public void testInvalidDockerValueLength() {
        String invalidValue = "112345abcdef12345abcdef12345abcdef12345abcdef12345abcdef12345abcdef12345abcdef2345abcdef";
        Assert.assertTrue(invalidValue.length() > 64);
        Assert.assertTrue(dockerData.isInvalidDockerValue(invalidValue));
    }

    @Test
    public void testDockerError() throws Exception {
        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
        MockServiceManager mockServiceManager = new MockServiceManager();
        StatsService spy = Mockito.spy(new StatsServiceImpl());
        mockServiceManager.setStatsService(spy);
        ServiceFactory.setServiceManager(mockServiceManager);
        // Invalid docker id. Missing characters.
        String invalidDockerId = "47cbd16b77c50cbf71401c069cd2189f0e659af17d5a2daca3bddf59d8a870";
        StringReader reader = new StringReader("2:cpu:/docker/" + invalidDockerId);
        Assert.assertNull(dockerData.readFile(reader));
    }

    @Test
    public void testDockerGCPError() throws Exception {
        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
        MockServiceManager mockServiceManager = new MockServiceManager();
        StatsService spy = Mockito.spy(new StatsServiceImpl());
        mockServiceManager.setStatsService(spy);
        ServiceFactory.setServiceManager(mockServiceManager);
        // Invalid docker id. Missing characters.
        String invalidDockerId = "47cbd16b77c50cbf71401c069cd2189f0e659af17d5a2daca3bddf59d8a870";
        StringReader reader = new StringReader("2:cpu:/" + invalidDockerId);
        Assert.assertNull(dockerData.readFile(reader));
    }

    @Test
    public void testNoDockerError() throws Exception {
        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
        MockServiceManager mockServiceManager = new MockServiceManager();
        StatsService spy = Mockito.spy(new StatsServiceImpl());
        mockServiceManager.setStatsService(spy);
        ServiceFactory.setServiceManager(mockServiceManager);
        String validDockerID = "47cbd16b77c50cbf71401c069cd2189f0e659af17d5a2daca3bddf59d8a870b2";
        StringReader reader = new StringReader("2:cpu:/docker/" + validDockerID);
        Assert.assertEquals(validDockerID, dockerData.readFile(reader));
    }

    @Test
    public void testInvalidDockerValueNull() {
        Assert.assertTrue(dockerData.isInvalidDockerValue(null));
    }

    private void processFile(File file, String answer) {
        System.out.println("Current test file: " + file.getAbsolutePath());
        String actual = dockerData.getDockerIdFromFile(file);
        Assert.assertEquals("The file " + file.getAbsolutePath() + " should have found " + answer + " but found "
                + actual, answer, actual);
    }

    private Map<String, String> testToAnswers(File solutionFile) throws Exception {
        Map<String, String> tests = new HashMap<>();
        JSONArray inputJson = readJsonAndGetTests(solutionFile);
        for (Object currentTest : inputJson) {
            JSONObject current = (JSONObject) currentTest;
            String filename = getStringValue("filename", current);
            String id = getStringValue("containerId", current);
            Assert.assertFalse(tests.containsKey(filename));
            tests.put(filename, id);
        }
        return tests;
    }

    private String getStringValue(String key, JSONObject input) {
        Assert.assertTrue(input.containsKey(key));
        String val = (String) input.get(key);
        return val;
    }

    private JSONArray readJsonAndGetTests(File file) throws Exception {
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

}
