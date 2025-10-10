/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.DistributedTracePayload;
import com.newrelic.api.agent.TransportType;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.model.PriorityAware;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.Tracer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class BaseDistributedTraceTest {

    static MockServiceManager serviceManager;
    static IAgentLogger noOpLogger;

    @BeforeClass
    public static void beforeClass() {
        final Map<String, Object> settings = new HashMap<>();
        settings.put(AgentConfigImpl.APP_NAME, "Unit Test");
        settings.put(AgentConfigImpl.HOST, "no-collector.example.com");
        Map<String, Object> dtConfig = new HashMap<>();
        dtConfig.put("enabled", true);
        settings.put("distributed_tracing", dtConfig);
        ConfigService configService = ConfigServiceFactory.createConfigServiceUsingSettings(settings);
        serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);
        noOpLogger = Mockito.mock(IAgentLogger.class);
    }

    @Before
    public void before() {
        Transaction.clearTransaction();
    }

    void createDistributedTraceService(final String accountId, final String trustKey,
            final String applicationId, final int majorVersion, final int minorVersion) {
        DistributedTraceService distributedTraceService = new DistributedTraceService() {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public int getMajorSupportedCatVersion() {
                return majorVersion;
            }

            @Override
            public int getMinorSupportedCatVersion() {
                return minorVersion;
            }

            @Override
            public String getAccountId() {
                return accountId;
            }

            @Override
            public String getApplicationId() {
                return applicationId;
            }

            @Override
            public Map<String, Object> getIntrinsics(DistributedTracePayloadImpl inboundPayload, String guid,
                    String traceId, TransportType transportType,
                    long parentTransportDuration, long largestTransportDuration,
                    String parentId, String parentSpanId, float priority) {
                return null;
            }

            @Override
            public String getTrustKey() {
                return trustKey;
            }

            @Override
            public DistributedTracePayload createDistributedTracePayload(Tracer tracer) {
                return null;
            }

            @Override
            public float calculatePriorityRemoteParent(boolean remoteParentSampled, Float inboundPriority) {
                return 0.0f;
            }

            @Override
            public float calculatePriorityRoot(){
                return 0.0f;
            }
        };
        serviceManager.setDistributedTraceService(distributedTraceService);
    }
}
