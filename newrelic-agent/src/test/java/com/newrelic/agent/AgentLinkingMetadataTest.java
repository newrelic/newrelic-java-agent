package com.newrelic.agent;

import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManagerImpl;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

        // Outside of Kubernetes no k8s.* attributes should be present
        assertFalse(linkingMetadata.containsKey(AgentLinkingMetadata.K8S_CLUSTER_NAME));
        assertFalse(linkingMetadata.containsKey(AgentLinkingMetadata.K8S_NAMESPACE_NAME));
        assertFalse(linkingMetadata.containsKey(AgentLinkingMetadata.K8S_POD_NAME));
        assertFalse(linkingMetadata.containsKey(AgentLinkingMetadata.K8S_NODE_NAME));
        assertFalse(linkingMetadata.containsKey(AgentLinkingMetadata.K8S_DEPLOYMENT_NAME));
        assertFalse(linkingMetadata.containsKey(AgentLinkingMetadata.K8S_REPLICASET_NAME));
        assertFalse(linkingMetadata.containsKey(AgentLinkingMetadata.K8S_CONTAINER_NAME));
        assertFalse(linkingMetadata.containsKey(AgentLinkingMetadata.K8S_CONTAINER_IMAGE_NAME));
    }

    @Test
    public void buildKubernetesMetadata_includesAllPresentAttributes() {
        Map<String, String> env = new HashMap<>();
        env.put("NEW_RELIC_METADATA_KUBERNETES_CLUSTER_NAME", "my-cluster");
        env.put("NEW_RELIC_METADATA_KUBERNETES_NAMESPACE_NAME", "my-namespace");
        env.put("NEW_RELIC_METADATA_KUBERNETES_POD_NAME", "my-pod");
        env.put("NEW_RELIC_METADATA_KUBERNETES_NODE_NAME", "my-node");
        env.put("NEW_RELIC_METADATA_KUBERNETES_DEPLOYMENT_NAME", "my-deployment");
        env.put("NEW_RELIC_METADATA_KUBERNETES_REPLICASET_NAME", "my-replicaset");
        env.put("NEW_RELIC_METADATA_KUBERNETES_CONTAINER_NAME", "my-container");
        env.put("NEW_RELIC_METADATA_KUBERNETES_CONTAINER_IMAGE_NAME", "my-image");
        // Unrelated env vars should be ignored
        env.put("SOME_OTHER_VAR", "ignored");

        Map<String, String> k8sMetadata = AgentLinkingMetadata.buildKubernetesMetadata(env);

        assertEquals(8, k8sMetadata.size());
        assertEquals("my-cluster", k8sMetadata.get(AgentLinkingMetadata.K8S_CLUSTER_NAME));
        assertEquals("my-namespace", k8sMetadata.get(AgentLinkingMetadata.K8S_NAMESPACE_NAME));
        assertEquals("my-pod", k8sMetadata.get(AgentLinkingMetadata.K8S_POD_NAME));
        assertEquals("my-node", k8sMetadata.get(AgentLinkingMetadata.K8S_NODE_NAME));
        assertEquals("my-deployment", k8sMetadata.get(AgentLinkingMetadata.K8S_DEPLOYMENT_NAME));
        assertEquals("my-replicaset", k8sMetadata.get(AgentLinkingMetadata.K8S_REPLICASET_NAME));
        assertEquals("my-container", k8sMetadata.get(AgentLinkingMetadata.K8S_CONTAINER_NAME));
        assertEquals("my-image", k8sMetadata.get(AgentLinkingMetadata.K8S_CONTAINER_IMAGE_NAME));
    }

    @Test
    public void buildKubernetesMetadata_returnsEmptyMapWhenNoEnvVarsSet() {
        Map<String, String> k8sMetadata = AgentLinkingMetadata.buildKubernetesMetadata(new HashMap<String, String>());

        assertTrue("k8s metadata should be empty outside of Kubernetes", k8sMetadata.isEmpty());
    }

    @Test
    public void buildKubernetesMetadata_ignoresBlankValues() {
        Map<String, String> env = new HashMap<>();
        env.put("NEW_RELIC_METADATA_KUBERNETES_CLUSTER_NAME", "");
        env.put("NEW_RELIC_METADATA_KUBERNETES_NAMESPACE_NAME", "my-namespace");

        Map<String, String> k8sMetadata = AgentLinkingMetadata.buildKubernetesMetadata(env);

        assertEquals(1, k8sMetadata.size());
        assertFalse(k8sMetadata.containsKey(AgentLinkingMetadata.K8S_CLUSTER_NAME));
        assertEquals("my-namespace", k8sMetadata.get(AgentLinkingMetadata.K8S_NAMESPACE_NAME));
    }
}
