package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import java.util.UUID;
import java.util.function.BiFunction;
import java.util.logging.Level;

public final class ResourceCustomer implements BiFunction<Resource, ConfigProperties, Resource> {
    @Override
    public Resource apply(Resource resource, ConfigProperties configProperties) {
        NewRelic.getAgent().getLogger().log(Level.FINE, "Appending OpenTelemetry resources");
        final ResourceBuilder builder = new ResourceBuilder().putAll(resource);
        final AttributeKey<String> instanceIdKey = AttributeKey.stringKey("service.instance.id");
        final String instanceId = resource.getAttribute(instanceIdKey);
        if (instanceId == null) {
            builder.put(instanceIdKey, UUID.randomUUID().toString());
        }

        final String entityGuid = AgentBridge.getAgent().getEntityGuid(true);
        if (entityGuid != null) {
            builder.put("entity.guid", entityGuid);
        }
        return builder.build();
    }
}
