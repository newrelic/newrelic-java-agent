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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DockerDataTest {

    private final DockerData dockerData = new DockerData();

    @Test
    public void testGetDockerIdNotLinux() {
        Assert.assertNull(dockerData.getDockerContainerIdFromCGroups(false));
    }
    @Test
    public void testCheckLineAndGetIdValidV2() {
        StringBuilder sb = new StringBuilder();
        String line = "481 473 254:1 /docker/containers/f37a7e4d17017e7bf774656b19ca4360c6cdc4951c86700a464101d0d9ce97ee/resolv.conf /etc/resolv.conf rw,relatime - ext4 /dev/vda1 rw";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb, CGroup.V2));
        Assert.assertEquals("f37a7e4d17017e7bf774656b19ca4360c6cdc4951c86700a464101d0d9ce97ee", sb.toString());

        sb = new StringBuilder();
        line = "886 322 5:997 /docker/containers/67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f/hosts /etc/hosts rw,relatime - opts ro";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb, CGroup.V2));
        Assert.assertEquals("67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f", sb.toString());

        sb = new StringBuilder();
        line = "886 322 5:997 /docker/containers/d340a98bd7761414d6b3b8fabd2917c74d85155af1477b584bcc4adf4b94eaf1 / rw - opts ro";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb, CGroup.V2));
        Assert.assertEquals("d340a98bd7761414d6b3b8fabd2917c74d85155af1477b584bcc4adf4b94eaf1", sb.toString());
    }

    @Test
    public void testCheckLineAndGetIdInValidV2() {
        StringBuilder sb = new StringBuilder();
        String line = "481 473 254:1 /fs /etc/resolv.conf rw,relatime - ext4 /dev/vda1 rw";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V2));

        //id-like string in the wrong place
        sb = new StringBuilder();
        line = "211 921 5:646 / / rw master - opt opt rw,dontReadPath=/var/lib/67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V2));

        //id too short
        sb = new StringBuilder();
        line = "481 473 254:1 /docker/containers/12345/hosts ro - proc proc rw";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V2));

        //missing prefix
        sb = new StringBuilder();
        line = "213 888 9:000 a2ffe0eb7ac22657a2a023ad628e9df837c38a03b1ebc904d3f6d644eb1a1a81 / rw - sys sys ro";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V2));

        //linux prefix
        sb = new StringBuilder();
        line = "886 322 5:997 /lxc/67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f9900/hosts / ro - opts";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V2));

        //id includes invalid character z
        sb = new StringBuilder();
        line = "886 322 5:997 /docker/containers/67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fz3c2f84a2f/hosts /etc/hosts rw,relatime - opts ro";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V2));

        //id includes dashes
        sb = new StringBuilder();
        line = "886 322 5:997 /docker/containers/67f98-c9e6188f9c-1818672a15dbe46237b6ee-7e77f834d40d41c5fb3c2f84a2f/hosts /etc/hosts rw,relatime - opts ro";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V2));
    }

    @Test
    public void testGetDockerIdFromFilesV2() throws Exception {
        Map<String, String> tests = testToAnswers(AgentHelper.getFile("com/newrelic/agent/cross_agent_tests/docker_container_id_v2/cases.json"));
        List<File> files = AgentHelper.getFiles("com/newrelic/agent/cross_agent_tests/docker_container_id_v2");
        if (files == null || files.isEmpty()) {
            Assert.fail("There were no files read in for testing.");
        }
        int testCount = 0;
        for (File current : files) {
            if (current.getName().toLowerCase().endsWith((".txt"))) {
                String name = current.getName();
                Assert.assertTrue("The test file name " + name + " was not found in the list of tests", tests.containsKey(name));
                String answer = tests.get(name);
                processFile(current, answer, CGroup.V2);
                testCount++;
            }
        }

        Assert.assertEquals("The number of tests files does not match the number of tests", testCount, tests.size());
    }
    @Test
    public void testCheckLineAndGetIdValidV1() {
        StringBuilder sb = new StringBuilder();
        String line = "4:cpu:/docker/f37a7e4d17017e7bf774656b19ca4360c6cdc4951c86700a464101d0d9ce97ee";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));
        Assert.assertEquals("f37a7e4d17017e7bf774656b19ca4360c6cdc4951c86700a464101d0d9ce97ee", sb.toString());

        sb = new StringBuilder();
        line = "3:cpu:/lxc/cb8c113e5f3cf8332f5231f8154adc429ea910f7c29995372de4f571c55d3159";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));
        Assert.assertEquals("cb8c113e5f3cf8332f5231f8154adc429ea910f7c29995372de4f571c55d3159", sb.toString());

        sb = new StringBuilder();
        line = "3:cpu,memory:/lxc/cb8c113e5f3cf8332f5231f8154adc429ea910f7c29995372de4f571c55d3159";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));
        Assert.assertEquals("cb8c113e5f3cf8332f5231f8154adc429ea910f7c29995372de4f571c55d3159", sb.toString());

        sb = new StringBuilder();
        line = "3:cpuacct,cpu:/system.slice/docker-67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f.scope";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));
        Assert.assertEquals("67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f", sb.toString());

        sb = new StringBuilder();
        line = "3:cpu:/system.slice/docker-67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f.scope";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));
        Assert.assertEquals("67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f", sb.toString());

        sb = new StringBuilder();
        line = "2:cpu:/docker/47cbd16b77c50cbf71401c069cd2189f0e659af17d5a2daca3bddf59d8a870b2";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));
        Assert.assertEquals("47cbd16b77c50cbf71401c069cd2189f0e659af17d5a2daca3bddf59d8a870b2", sb.toString());

        sb = new StringBuilder();
        line = "2:cpu:/47cbd16b77c50cbf71401c069cd2189f0e659af17d5a2daca3bddf59d8a870b2";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));
        Assert.assertEquals("47cbd16b77c50cbf71401c069cd2189f0e659af17d5a2daca3bddf59d8a870b2", sb.toString());

        sb = new StringBuilder();
        line = "2:cpu:/kubepods.slice/kubepods-burstable.slice/kubepods-burstable-pod917d7891_0d63_11ea_873f_005056993b36.slice/docker-92b21022a0cecd0212c5097aa63ed8248ed832902dbdbb654e6df95e05573646.scope";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));
        Assert.assertEquals("92b21022a0cecd0212c5097aa63ed8248ed832902dbdbb654e6df95e05573646", sb.toString());

        sb = new StringBuilder();
        line = "1:cpu:/system.slice/crio-67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f.scope";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));
        Assert.assertEquals("67f98c9e6188f9c1818672a15dbe46237b6ee7e77f834d40d41c5fb3c2f84a2f", sb.toString());

        //Azure docker container example
        sb = new StringBuilder();
        line = "2:cpu:/containers/f142db3a4219409af324d5481e297545aa33425fb7dc837e68cee93c36062ca8";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));
        Assert.assertEquals("f142db3a4219409af324d5481e297545aa33425fb7dc837e68cee93c36062ca8", sb.toString());

        //Azure docker container example
        sb = new StringBuilder();
        line = "11:cpu,cpuacct:/kubepods.slice/kubepods-burstable.slice/kubepods-burstable-pod4b42abd9_149e_4db2_82f9_d52edd985efe.slice/cri-containerd-d340a98bd7761414d6b3b8fabd2917c74d85155af1477b584bcc4adf4b94eaf1.scope";
        Assert.assertTrue(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));
        Assert.assertEquals("d340a98bd7761414d6b3b8fabd2917c74d85155af1477b584bcc4adf4b94eaf1", sb.toString());
    }

    @Test
    public void testCheckLineAndGetIdInValidV1() {
        StringBuilder sb;
        String line;

        sb = new StringBuilder();
        // it should be cpu not cpuacct
        line = "3:cpuacct:/lxc/cb8c113e5f3cf8332f5231f8154adc429ea910f7c29995372de4f571c55d3159";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));

        sb = new StringBuilder();
        // it should be cpu not cpuacct
        line = "3:cpuacct:/cb8c113e5f3cf8332f5231f8154adc429ea910f7c29995372de4f571c55d3159";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));

        sb = new StringBuilder();
        line = "2:cpuset:/docker/47cbd16b77c50cbf71401c069cd2189f0e659af17d5a2daca3bddf59d8a870b2";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));

        sb = new StringBuilder();
        line = "2:cpuset:/47cbd16b77c50cbf71401c069cd2189f0e659af17d5a2daca3bddf59d8a870b2";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));

        sb = new StringBuilder();
        line = "1:hugetlb,perf_event,blkio,freezer,devices,memory,cpuacct,cpu,cpuset:/lxc/b6d196c1-50f2-4949-abdb-5d4909864487";
        // id not grabbed becase of slashes (-)
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));

        sb = new StringBuilder();
        line = "1:hugetlb,perf_event,blkio,freezer,devices,memory,cpuacct,cpu,cpuset:/b6d196c1-50f2-4949-abdb-5d4909864487";
        // id not grabbed becase of slashes (-)
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));

        sb = new StringBuilder();
        line = "4:cpu:/lxc/p1";
        // id not grabbed because p not a valid id character
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));

        sb = new StringBuilder();
        line = "4:cpu:/p1";
        // id not grabbed because p not a valid id character
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));

        line = "4:cpu:";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));

        line = "4:cpu:";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));

        line = "4:cpu:/";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));

        line = "5:";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));

        line = ":";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));

        line = "::";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));

        line = "2:,:/";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));

        line = "2;,;/";
        Assert.assertFalse(dockerData.checkLineAndGetResult(line, sb, CGroup.V1));
    }

    @Test
    public void testGetDockerIdFromFilesV1() throws Exception {
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
                processFile(current, answer, CGroup.V1);
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
        Assert.assertNull(dockerData.readFile(reader, CGroup.V1));
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
        Assert.assertNull(dockerData.readFile(reader, CGroup.V1));
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
        Assert.assertEquals(validDockerID, dockerData.readFile(reader, CGroup.V1));
    }

    @Test
    public void testInvalidDockerValueNull() {
        Assert.assertTrue(dockerData.isInvalidDockerValue(null));
    }

    @Test
    public void retrieveDockerIdFromFargateMetadata_withValidUrl_returnsDockerIdUrl() throws IOException {
        InputStream byteArrayStream = new ByteArrayInputStream(FARGATE_JSON.getBytes());
        AwsFargateMetadataFetcher mockFetcher = mock(AwsFargateMetadataFetcher.class);
        when(mockFetcher.openStream()).thenReturn(byteArrayStream);

        DockerData dockerData = new DockerData();
        Assert.assertEquals("1e1698469422439ea356071e581e8545-2769485393", dockerData.retrieveDockerIdFromFargateMetadataUrl(mockFetcher));
    }

    @Test
    public void retrieveDockerIdFromFargateMetadata_Url_withInvalidJson_returnsNull() throws IOException {
        InputStream byteArrayStream = new ByteArrayInputStream("foofoo".getBytes());
        AwsFargateMetadataFetcher mockFetcher = mock(AwsFargateMetadataFetcher.class);
        when(mockFetcher.openStream()).thenReturn(byteArrayStream);

        DockerData dockerData = new DockerData();
        Assert.assertNull(dockerData.retrieveDockerIdFromFargateMetadataUrl(mockFetcher));
    }

    @Test
    public void retrieveDockerIdFromFargateMetadata_withValidFile_returnsDockerId() {
        URL resourceUrl = getClass().getResource("/ecs_fargate_metadata.json");
        File file = new File(resourceUrl.getFile());

        DockerData dockerData = new DockerData();
        Assert.assertEquals("b593651c4d6b44a6b2b583f45c957e15-3356213583", dockerData.retrieveDockerIdFromFargateMetadataFile(file.getAbsolutePath()));
    }

    @Test
    public void retrieveDockerIdFromFargateMetadata_withIncompleteJson_returnsNull() {
        URL resourceUrl = getClass().getResource("/invalid_ecs_fargate_metadata.json");
        File file = new File(resourceUrl.getFile());

        DockerData dockerData = new DockerData();
        Assert.assertNull(dockerData.retrieveDockerIdFromFargateMetadataFile(file.getAbsolutePath()));
    }

    @Test
    public void retrieveDockerIdFromFargateMetadata_withMissingFile_returnsNull() throws IOException, URISyntaxException {
        DockerData dockerData = new DockerData();
        Assert.assertNull(dockerData.retrieveDockerIdFromFargateMetadataFile("/foo.json"));
    }

    @Test
    public void retrieveDockerIdFromFargateMetadata_Url_withInputStreamException_returnsNull() throws IOException {
        AwsFargateMetadataFetcher mockFetcher = mock(AwsFargateMetadataFetcher.class);
        when(mockFetcher.openStream()).thenThrow(new IOException("oops"));

        DockerData dockerData = new DockerData();
        Assert.assertNull(dockerData.retrieveDockerIdFromFargateMetadataUrl(mockFetcher));
    }

    @Test
    public void getDockerContainerId_withNoDockerIdSource_returnsNull() {
        Assert.assertNull(dockerData.getDockerContainerIdFromCGroups(true));
    }

    private void processFile(File file, String answer, CGroup cgroup) {
        System.out.println("Current test file: " + file.getAbsolutePath());
        String actual = dockerData.getDockerIdFromFile(file, cgroup);
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

    private static final String FARGATE_JSON =
            "{" +
                    "\"DockerId\": \"1e1698469422439ea356071e581e8545-2769485393\"," +
                    "\"Name\": \"fargateapp\"," +
                    "\"DockerName\": \"fargateapp\"," +
                    "\"Image\": \"123456789012.dkr.ecr.us-west-2.amazonaws.com/fargatetest:latest\"," +
                    "\"ImageID\": \"sha256:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcd\"," +
                    "\"Labels\": {" +
                    "\"com.amazonaws.ecs.cluster\": \"arn:aws:ecs:us-west-2:123456789012:cluster/testcluster\"," +
                    "\"com.amazonaws.ecs.container-name\": \"fargateapp\"," +
                    "\"com.amazonaws.ecs.task-arn\": \"arn:aws:ecs:us-west-2:123456789012:task/testcluster/1e1698469422439ea356071e581e8545\"," +
                    "\"com.amazonaws.ecs.task-definition-family\": \"fargatetestapp\"," +
                    "\"com.amazonaws.ecs.task-definition-version\": \"7\"" +
                    "}," +
                    "\"DesiredStatus\": \"RUNNING\"," +
                    "\"KnownStatus\": \"RUNNING\"," +
                    "\"Limits\": {" +
                    "\"CPU\": 2" +
                    "}," +
                    "\"CreatedAt\": \"2024-04-25T17:38:31.073208914Z\"," +
                    "\"StartedAt\": \"2024-04-25T17:38:31.073208914Z\"," +
                    "\"Type\": \"NORMAL\"," +
                    "\"Networks\": [" +
                    "{" +
                    "\"NetworkMode\": \"awsvpc\"," +
                    "\"IPv4Addresses\": [" +
                    "\"10.10.10.10\"" +
                    "]" +
                    "}" +
                    "]" +
                    "}";
}
