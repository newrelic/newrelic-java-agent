package com.newrelic.agent.samplers;

import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;

public class ProcStatCPUSamplerzzzTest {
    @Before
    public void setup() {
        MockServiceManager serviceManager = new MockServiceManager();
        serviceManager.setConfigService(new MockConfigService(AgentConfigFactory.createAgentConfig(
                Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(), null)));
        ServiceFactory.setServiceManager(serviceManager);
    }

    @Test
    public void getProcessCpuTime_withValidFile_returnsCorrectTime() throws Exception {
        File file = createTmpCpuUsageFile("cpu 1279636934 73759586 192327563 12184330186 543227057 56603 68503253 0 0 0 0 0 12345 987654");
        ProcStatCPUSampler procStatCPUSampler = new ProcStatCPUSampler(file);

        Assert.assertEquals(9999.0, procStatCPUSampler.getProcessCpuTime(), .1);

    }

    @Test
    public void getProcessCpuTime_withInvalidFile_returnsZero() throws Exception {
        File file = createTmpCpuUsageFile("cpu 1279636934 ");
        ProcStatCPUSampler procStatCPUSampler = new ProcStatCPUSampler(file);

        Assert.assertEquals(0.0, procStatCPUSampler.getProcessCpuTime(), .1);

        File file2 = createTmpCpuUsageFile("cpu 1279636934 73759586 192327563 12184330186 543227057 56603 68503253 0 0 0 0 0 aaaa bbbb");
        procStatCPUSampler = new ProcStatCPUSampler(file2);

        Assert.assertEquals(0.0, procStatCPUSampler.getProcessCpuTime(), .1);
    }

    private File createTmpCpuUsageFile(String contents) {
        File file = null;
        try {
            file = File.createTempFile("cpu_time", null);
            file.deleteOnExit();
            FileWriter writer = new FileWriter(file);
            writer.write(contents);
            writer.close();
        } catch (IOException ignored) {
        }

        return file;
    }

}
