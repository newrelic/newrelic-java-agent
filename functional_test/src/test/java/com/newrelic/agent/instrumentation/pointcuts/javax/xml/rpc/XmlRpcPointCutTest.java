package com.newrelic.agent.instrumentation.pointcuts.javax.xml.rpc;

import com.fake.MockCall;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.SpanEventsService;
import com.newrelic.api.agent.Trace;
import org.junit.Test;
import test.newrelic.EnvironmentHolderSettingsGenerator;
import test.newrelic.test.agent.EnvironmentHolder;

import java.rmi.RemoteException;
import java.util.List;

import static org.junit.Assert.*;

public class XmlRpcPointCutTest {

    private static final String CONFIG_FILE = "configs/span_events_test.yml";
    private static final ClassLoader CLASS_LOADER = XmlRpcPointCutTest.class.getClassLoader();

    @Test
    public void externalTest() throws Exception {
        EnvironmentHolderSettingsGenerator envHolderSettings = new EnvironmentHolderSettingsGenerator(CONFIG_FILE, "all_enabled_test", CLASS_LOADER);
        EnvironmentHolder holder = new EnvironmentHolder(envHolderSettings);
        holder.setupEnvironment();

        try {
            doCall();

            SpanEventsService spanEventsService = ServiceFactory.getServiceManager().getSpanEventsService();
            String appName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
            SamplingPriorityQueue<SpanEvent> spanEventsPool = spanEventsService.getOrCreateDistributedSamplingReservoir(appName);
            assertNotNull(spanEventsPool);

            List<SpanEvent> spanEvents = spanEventsPool.asList();
            assertNotNull(spanEvents);
            assertEquals(2, spanEvents.size());

            boolean java = false;
            boolean external = false;
            for (SpanEvent span : spanEvents) {
                String name = (String) span.getIntrinsics().get("name");
                if (name.equals("External/newrelic.com/XmlRpc/invoke")) {
                    external = true;
                    assertEquals("XmlRpc", span.getIntrinsics().get("component"));
                    assertEquals("http", span.getIntrinsics().get("category"));
                    assertEquals("http://newrelic.com", span.getAgentAttributes().get("http.url"));
                } else if (name.equals("Java/com.newrelic.agent.instrumentation.pointcuts.javax.xml.rpc.XmlRpcPointCutTest/doCall")) {
                    java = true;
                    assertEquals("generic", span.getIntrinsics().get("category"));
                }
            }

            assertTrue("Unexpected span events found", java && external);
        } finally {
            holder.close();
        }
    }

    @Trace(dispatcher = true)
    public void doCall() {
        MockCall call = new MockCall();
        try {
            call.invoke(new Object[] { 1 });
        } catch (RemoteException e) {
        }
    }

}
