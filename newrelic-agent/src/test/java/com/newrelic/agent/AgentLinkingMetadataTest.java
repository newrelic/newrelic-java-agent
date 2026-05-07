package com.newrelic.agent;

import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManagerImpl;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentLinkingMetadataTest {

    @Test
    public void getLinkingMetadata() {
        // Given
        final String expectedTraceId = "traceId1234";
        final String expectedSpanId = "spanId5678";
        final String expectedEntityGuid = "entityGuid91011";
        final String expectedEntityName = "entityName91011";
        final String expectedEntityType = AgentLinkingMetadata.ENTITY_TYPE_DEFAULT;

        TraceMetadataImpl traceMetadataMock = mock(TraceMetadataImpl.class);
        ServiceManagerImpl serviceManagerMock = mock(ServiceManagerImpl.class);
        RPMServiceManagerImpl rpmServiceManagerMock = mock(RPMServiceManagerImpl.class);
        RPMService rpmServiceMock = mock(RPMService.class);
        ConfigServiceImpl configServiceMock = mock(ConfigServiceImpl.class);
        AgentConfigImpl agentConfigMock = mock(AgentConfigImpl.class);

        ServiceFactory.setServiceManager(serviceManagerMock);

        // When
        when(traceMetadataMock.getTraceId()).thenReturn(expectedTraceId);
        when(traceMetadataMock.getSpanId()).thenReturn(expectedSpanId);
        when(serviceManagerMock.getRPMServiceManager()).thenReturn(rpmServiceManagerMock);
        when(serviceManagerMock.getConfigService()).thenReturn(configServiceMock);
        when(rpmServiceManagerMock.getRPMService()).thenReturn(rpmServiceMock);
        when(configServiceMock.getDefaultAgentConfig()).thenReturn(agentConfigMock);
        when(agentConfigMock.getApplicationName()).thenReturn(expectedEntityName);
        when(rpmServiceMock.getEntityGuid()).thenReturn(expectedEntityGuid);

        // Then
        Map<String, String> linkingMetadata = AgentLinkingMetadata.getLinkingMetadata(traceMetadataMock, ServiceFactory.getConfigService(),
                ServiceFactory.getRPMService());

        assertFalse("linkingMetadata map shouldn't be empty", linkingMetadata.isEmpty());

        // Can't assert on a specific hostname value as it will resolve to the actual hostname of the machine running the test
        assertFalse("hostname shouldn't be empty", linkingMetadata.get(AgentLinkingMetadata.HOSTNAME).isEmpty());

        assertEquals(expectedEntityGuid, linkingMetadata.get(AgentLinkingMetadata.ENTITY_GUID));
        assertEquals(expectedEntityName, linkingMetadata.get(AgentLinkingMetadata.ENTITY_NAME));
        assertEquals(expectedEntityType, linkingMetadata.get(AgentLinkingMetadata.ENTITY_TYPE));

        assertEquals(expectedTraceId, linkingMetadata.get(AgentLinkingMetadata.TRACE_ID));
        assertEquals(expectedSpanId, linkingMetadata.get(AgentLinkingMetadata.SPAN_ID));
    }

    @Test
    public void getLinkingMetadataWithEmptyTraceAttributes() {
        // Given
        final String expectedTraceId = "";
        final String expectedSpanId = "";
        final String expectedEntityGuid = "entityGuid91011";
        final String expectedEntityName = "entityName91011";
        final String expectedEntityType = AgentLinkingMetadata.ENTITY_TYPE_DEFAULT;

        TraceMetadataImpl traceMetadataMock = mock(TraceMetadataImpl.class);
        ServiceManagerImpl serviceManagerMock = mock(ServiceManagerImpl.class);
        RPMServiceManagerImpl rpmServiceManagerMock = mock(RPMServiceManagerImpl.class);
        RPMService rpmServiceMock = mock(RPMService.class);
        ConfigServiceImpl configServiceMock = mock(ConfigServiceImpl.class);
        AgentConfigImpl agentConfigMock = mock(AgentConfigImpl.class);

        ServiceFactory.setServiceManager(serviceManagerMock);

        // When
        when(traceMetadataMock.getTraceId()).thenReturn(expectedTraceId);
        when(traceMetadataMock.getSpanId()).thenReturn(expectedSpanId);
        when(serviceManagerMock.getRPMServiceManager()).thenReturn(rpmServiceManagerMock);
        when(serviceManagerMock.getConfigService()).thenReturn(configServiceMock);
        when(rpmServiceManagerMock.getRPMService()).thenReturn(rpmServiceMock);
        when(configServiceMock.getDefaultAgentConfig()).thenReturn(agentConfigMock);
        when(agentConfigMock.getApplicationName()).thenReturn(expectedEntityName);
        when(rpmServiceMock.getEntityGuid()).thenReturn(expectedEntityGuid);
        when(rpmServiceMock.getApplicationName()).thenReturn(expectedEntityName);

        // Then
        Map<String, String> linkingMetadata = AgentLinkingMetadata.getLinkingMetadata(traceMetadataMock, ServiceFactory.getConfigService(),
                ServiceFactory.getRPMService());

        assertFalse("linkingMetadata map shouldn't be empty", linkingMetadata.isEmpty());

        // Can't assert on a specific hostname value as it will resolve to the actual hostname of the machine running the test
        assertFalse("hostname shouldn't be empty", linkingMetadata.get(AgentLinkingMetadata.HOSTNAME).isEmpty());

        assertEquals(expectedEntityGuid, linkingMetadata.get(AgentLinkingMetadata.ENTITY_GUID));
        assertEquals(expectedEntityName, linkingMetadata.get(AgentLinkingMetadata.ENTITY_NAME));
        assertEquals(expectedEntityType, linkingMetadata.get(AgentLinkingMetadata.ENTITY_TYPE));

        // trace.id and span.id would be empty values if getLinkingMetadata was called outside of a transaction.
        // With the getLinkingMetadata API the returned map includes keys with empty values
        assertEquals(expectedTraceId, linkingMetadata.get(AgentLinkingMetadata.TRACE_ID));
        assertEquals(expectedSpanId, linkingMetadata.get(AgentLinkingMetadata.SPAN_ID));
    }

    @Test
    public void getLogEventLinkingMetadata() {
        // Given
        final String expectedTraceId = "traceId1234";
        final String expectedSpanId = "spanId5678";
        final String expectedEntityGuid = "entityGuid91011";
        final String expectedEntityName = "entityName91011";

        TraceMetadataImpl traceMetadataMock = mock(TraceMetadataImpl.class);
        ServiceManagerImpl serviceManagerMock = mock(ServiceManagerImpl.class);
        RPMServiceManagerImpl rpmServiceManagerMock = mock(RPMServiceManagerImpl.class);
        RPMService rpmServiceMock = mock(RPMService.class);
        ConfigServiceImpl configServiceMock = mock(ConfigServiceImpl.class);
        AgentConfigImpl agentConfigMock = mock(AgentConfigImpl.class);

        ServiceFactory.setServiceManager(serviceManagerMock);

        // When
        when(traceMetadataMock.getTraceId()).thenReturn(expectedTraceId);
        when(traceMetadataMock.getSpanId()).thenReturn(expectedSpanId);
        when(serviceManagerMock.getRPMServiceManager()).thenReturn(rpmServiceManagerMock);
        when(serviceManagerMock.getConfigService()).thenReturn(configServiceMock);
        when(rpmServiceManagerMock.getRPMService()).thenReturn(rpmServiceMock);
        when(configServiceMock.getDefaultAgentConfig()).thenReturn(agentConfigMock);
        when(agentConfigMock.getApplicationName()).thenReturn(expectedEntityName);
        when(rpmServiceMock.getEntityGuid()).thenReturn(expectedEntityGuid);
        when(rpmServiceMock.getApplicationName()).thenReturn(expectedEntityName);

        // Then
        Map<String, String> linkingMetadata = AgentLinkingMetadata.getLogEventLinkingMetadata(traceMetadataMock, ServiceFactory.getConfigService(),
                ServiceFactory.getRPMService());

        assertFalse("linkingMetadata map shouldn't be empty", linkingMetadata.isEmpty());

        // Can't assert on a specific hostname value as it will resolve to the actual hostname of the machine running the test
        assertFalse("hostname shouldn't be empty", linkingMetadata.get(AgentLinkingMetadata.HOSTNAME).isEmpty());

        assertFalse("entity.type should not be included in LogEvent linking metadata", linkingMetadata.containsKey(AgentLinkingMetadata.ENTITY_TYPE));
        assertEquals(expectedEntityName, linkingMetadata.get(AgentLinkingMetadata.ENTITY_NAME));
        assertEquals(expectedEntityGuid, linkingMetadata.get(AgentLinkingMetadata.ENTITY_GUID));

        assertEquals(expectedTraceId, linkingMetadata.get(AgentLinkingMetadata.TRACE_ID));
        assertEquals(expectedSpanId, linkingMetadata.get(AgentLinkingMetadata.SPAN_ID));
    }

    @Test
    public void getLogEventLinkingMetadataWithEmptyTraceAttributes() {
        // Given
        final String expectedTraceId = "";
        final String expectedSpanId = "";
        final String expectedEntityGuid = "entityGuid91011";
        final String expectedEntityName = "entityName91011";

        TraceMetadataImpl traceMetadataMock = mock(TraceMetadataImpl.class);
        ServiceManagerImpl serviceManagerMock = mock(ServiceManagerImpl.class);
        RPMServiceManagerImpl rpmServiceManagerMock = mock(RPMServiceManagerImpl.class);
        RPMService rpmServiceMock = mock(RPMService.class);
        ConfigServiceImpl configServiceMock = mock(ConfigServiceImpl.class);
        AgentConfigImpl agentConfigMock = mock(AgentConfigImpl.class);

        ServiceFactory.setServiceManager(serviceManagerMock);

        // When
        when(traceMetadataMock.getTraceId()).thenReturn(expectedTraceId);
        when(traceMetadataMock.getSpanId()).thenReturn(expectedSpanId);
        when(serviceManagerMock.getRPMServiceManager()).thenReturn(rpmServiceManagerMock);
        when(serviceManagerMock.getConfigService()).thenReturn(configServiceMock);
        when(rpmServiceManagerMock.getRPMService()).thenReturn(rpmServiceMock);
        when(configServiceMock.getDefaultAgentConfig()).thenReturn(agentConfigMock);
        when(agentConfigMock.getApplicationName()).thenReturn(expectedEntityName);
        when(rpmServiceMock.getEntityGuid()).thenReturn(expectedEntityGuid);
        when(rpmServiceMock.getApplicationName()).thenReturn(expectedEntityName);

        // Then
        Map<String, String> linkingMetadata = AgentLinkingMetadata.getLogEventLinkingMetadata(traceMetadataMock, ServiceFactory.getConfigService(),
                ServiceFactory.getRPMService());

        assertFalse("linkingMetadata map shouldn't be empty", linkingMetadata.isEmpty());

        // Can't assert on a specific hostname value as it will resolve to the actual hostname of the machine running the test
        assertFalse("hostname shouldn't be empty", linkingMetadata.get(AgentLinkingMetadata.HOSTNAME).isEmpty());

        assertFalse("entity.type should not be included in LogEvent linking metadata", linkingMetadata.containsKey(AgentLinkingMetadata.ENTITY_TYPE));
        assertEquals(expectedEntityName, linkingMetadata.get(AgentLinkingMetadata.ENTITY_NAME));
        assertEquals(expectedEntityGuid, linkingMetadata.get(AgentLinkingMetadata.ENTITY_GUID));

        // trace.id and span.id would be empty values if getLogEventLinkingMetadata was called outside of a transaction, in which case they are omitted
        assertFalse("empty trace.id value should not be included in LogEvent linking metadata", linkingMetadata.containsKey(AgentLinkingMetadata.TRACE_ID));
        assertFalse("empty span.id value should not be included in LogEvent linking metadata", linkingMetadata.containsKey(AgentLinkingMetadata.SPAN_ID));
    }
}
