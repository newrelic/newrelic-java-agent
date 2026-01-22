package com.newrelic.agent.tracing;

import com.newrelic.agent.IRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.attributes.CrossAgentInput;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionGuidFactory;
import com.newrelic.agent.tracing.samplers.AdaptiveSampler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.newrelic.agent.tracing.DistributedTraceServiceImpl.SamplerCase.REMOTE_PARENT_NOT_SAMPLED;
import static com.newrelic.agent.tracing.DistributedTraceServiceImpl.SamplerCase.REMOTE_PARENT_SAMPLED;
import static com.newrelic.agent.tracing.DistributedTraceServiceImpl.SamplerCase.ROOT;
import static org.mockito.ArgumentMatchers.any;

@RunWith(Parameterized.class)
public class HarvestSamplingRatesCrossAgentTest {
    private DistributedTraceServiceImpl distributedTraceService;
    private MockServiceManager serviceManager;
    private MockRPMServiceManager rpmServiceManager;

    int fullGranSampled = 0;
    int partialGranSampled = 0;
    int totalSampled = 0;

    @Parameterized.Parameter(0)
    public String testName;

    @Parameterized.Parameter(1)
    public JSONObject testData;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() throws Exception {
        JSONArray tests = CrossAgentInput.readJsonAndGetTests("com/newrelic/agent/cross_agent_tests/samplers/harvest_sampling_rates.json");
        List<Object[]> result = new LinkedList<>();
        for (Object test : tests) {
            JSONObject testObject = (JSONObject) test;
            String name = (String) testObject.get("test_name");
            result.add(new Object[]{name, testObject});
        }
        return result;
    }

    @Before
    public void before() throws Exception {
        fullGranSampled = 0;
        partialGranSampled = 0;
        totalSampled = 0;

        AdaptiveSampler.resetForTesting();

        Map<String, Object> config = new HashMap<>();
        config.put(AgentConfigImpl.APP_NAME, "Test");
        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(config),
                Collections.<String, Object>emptyMap());
        serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.setConfigService(configService);
        rpmServiceManager = new MockRPMServiceManager();
        serviceManager.setRPMServiceManager(rpmServiceManager);
        ServiceFactory.getServiceManager().start();
    }

    @Test
    public void testHarvestSamplingRates(){
        JSONObject testConfig = (JSONObject) testData.get("config");
        Map<String, Object> dtConfig = buildConfigFromJSON(testConfig);
        Map<String, Object> mainConfig = new HashMap<>();
        mainConfig.put("distributed_tracing", dtConfig);

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(mainConfig);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
        distributedTraceService = new DistributedTraceServiceImpl();
        serviceManager.setDistributedTraceService(distributedTraceService);

        IRPMService rpmService = rpmServiceManager.getOrCreateRPMService("Test");
        distributedTraceService.connected(rpmService, agentConfig);

        runSamplers();
        assertSampledCounts();
    }

    private void assertSampledCounts(){
        int expectedTotalSampled = ((Long) testData.get("expected_sampled")).intValue();
        int expectedFullSampled = ((Long) testData.get("expected_sampled_full")).intValue();
        int expectedPartialSampled = ((Long) testData.get("expected_sampled_partial")).intValue();

        //some tests include an optional "variance" parameter to include wiggle room in the expected totals.
        double variance = testData.containsKey("variance") ? (Double) testData.get("variance") : 0.0;

        Assert.assertTrue(Math.abs(expectedTotalSampled - totalSampled) <= expectedTotalSampled * variance);
        Assert.assertTrue(Math.abs(expectedFullSampled - fullGranSampled) <= expectedFullSampled * variance);
        Assert.assertTrue(Math.abs(expectedPartialSampled - partialGranSampled) <= expectedPartialSampled * variance);
    }

    private void runSamplers() {
        int rootTraffic = testData.containsKey("root") ? ((Long) testData.get("root")).intValue() : 0;
        int parentSampledNoMatchingIdTraffic = testData.containsKey("parent_sampled_no_matching_acct_id") ? ((Long) testData.get("parent_sampled_no_matching_acct_id")).intValue() : 0;
        int parentSampledMatchingIdSampledTrueTraffic = testData.containsKey("parent_sampled_matching_acct_id_sampled_true") ? ((Long) testData.get("parent_sampled_matching_acct_id_sampled_true")).intValue() : 0;
        int parentNotSampledNoMatchingIdTraffic = testData.containsKey("parent_not_sampled_no_matching_acct_id") ? ((Long) testData.get("parent_not_sampled_no_matching_acct_id")).intValue() : 0;
        int parentNotSampledMatchingIdSampledTrueTraffic = testData.containsKey("parent_not_sampled_matching_acct_id_sampled_true") ? ((Long) testData.get("parent_not_sampled_matching_acct_id_sampled_true")).intValue() : 0;

        while (rootTraffic + parentSampledNoMatchingIdTraffic + parentSampledMatchingIdSampledTrueTraffic + parentNotSampledNoMatchingIdTraffic + parentNotSampledMatchingIdSampledTrueTraffic > 0) {
            //try to sample each of the five traffic types, one-at-a-time.
            //this is preferable to doing all roots first, then all parentSampleNoMatchingIdTraffic, etc, so that we don't have a gotcha treating the first (or last) txns specially.
            if (rootTraffic > 0){
                incrementSampled(distributedTraceService.calculatePriority(simulateRootTransaction(), ROOT));
                rootTraffic--;
            }
            if (parentSampledNoMatchingIdTraffic > 0){
                incrementSampled(distributedTraceService.calculatePriority(simulateTransactionWithParent(false, null), REMOTE_PARENT_SAMPLED));
                parentSampledNoMatchingIdTraffic--;
            }
            if (parentSampledMatchingIdSampledTrueTraffic > 0){
                incrementSampled(distributedTraceService.calculatePriority(simulateTransactionWithParent(true, true), REMOTE_PARENT_SAMPLED));
                parentSampledMatchingIdSampledTrueTraffic--;
            }
            if (parentNotSampledNoMatchingIdTraffic > 0){
                incrementSampled(distributedTraceService.calculatePriority(simulateTransactionWithParent(false, null), REMOTE_PARENT_NOT_SAMPLED));
                parentNotSampledNoMatchingIdTraffic--;
            }
            if (parentNotSampledMatchingIdSampledTrueTraffic > 0){
                incrementSampled(distributedTraceService.calculatePriority(simulateTransactionWithParent(true, true), REMOTE_PARENT_NOT_SAMPLED));
                parentNotSampledMatchingIdSampledTrueTraffic--;
            }
        }
    }

    private void incrementSampled(float priority){
        if (priority >= 1.0f){
            totalSampled++;
            if (priority >= 2.0f){
                fullGranSampled++;
            } else {
                partialGranSampled++;
            }
        }
    }

    private Transaction simulateRootTransaction(){
        Transaction tx = Mockito.mock(Transaction.class);
        Mockito.when(tx.getPriorityFromInboundSamplingDecision(any())).thenReturn(null);
        Mockito.when(tx.getOrCreateTraceId()).thenReturn(TransactionGuidFactory.generate16CharGuid() + TransactionGuidFactory.generate16CharGuid());
        return tx;
    }

    private Transaction simulateTransactionWithParent(Boolean hadMatchingAcctId, Boolean traceContextSampledFlag){
        Transaction tx = Mockito.mock(Transaction.class);
        Mockito.when(tx.getOrCreateTraceId()).thenReturn(TransactionGuidFactory.generate16CharGuid() + TransactionGuidFactory.generate16CharGuid());
        if (!hadMatchingAcctId){
            Mockito.when(tx.getPriorityFromInboundSamplingDecision(any())).thenReturn(null);
        } else {
            float basePriority = DistributedTraceServiceImpl.nextTruncatedFloat();
            Mockito.when(tx.getPriorityFromInboundSamplingDecision(Granularity.FULL)).thenReturn(basePriority + (traceContextSampledFlag ? Granularity.FULL.priorityIncrement() : 0));
            Mockito.when(tx.getPriorityFromInboundSamplingDecision(Granularity.PARTIAL)).thenReturn(basePriority + (traceContextSampledFlag ? Granularity.PARTIAL.priorityIncrement() : 0));
        }
        return tx;
    }


    private Map<String, Object> buildConfigFromJSON(JSONObject testSpec) {
        Map<String, Object> config = new HashMap<>();
        if (testSpec.containsKey("sampler")) {
            config.put("sampler", parseJSONAsMap((JSONObject) testSpec.get("sampler")));
        }
        return config;
    }

    private Map<String, Object> parseJSONAsMap(JSONObject json) {
        Map<String, Object> result = new HashMap<>();
        for (Object key : json.keySet()) {
            result.put((String) key, parseValue(json.get(key)));
        }
        return result;
    }

    private Object parseValue(Object value) {
        //warning, this only handles JSON objects and primitives!! If you get parsing error, update this to be able to handle json arrays.
        if (value instanceof JSONObject) {
            return parseJSONAsMap((JSONObject) value);
        } else {
            return value;
        }
    }
}
