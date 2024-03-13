package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;

import java.util.HashMap;
import java.util.Map;

@Weave(type = MatchType.ExactClass)
public class AutoConfiguredOpenTelemetrySdk {

    public static AutoConfiguredOpenTelemetrySdkBuilder builder() {
        final AutoConfiguredOpenTelemetrySdkBuilder builder = Weaver.callOriginal();
        if (System.getProperty("otel.exporter.otlp.endpoint") == null &&
                System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT") == null) {
            agent.getLogger().log(Level.INFO, "Auto-initializing OpenTelemetry SDK");
            final String entityGuid = AgentBridge.getAgent().getEntityGuid(true);
            final Agent agent = NewRelic.getAgent();
            final String host = agent.getConfig().getValue("host");
            final String endpoint = "https://" + host + ":443";
            final String licenseKey = agent.getConfig().getValue("license_key");
            final Map<String, String> properties = new HashMap<>();
            properties.put("otel.exporter.otlp.headers", "api-key=" + licenseKey);
            properties.put("otel.exporter.otlp.endpoint", endpoint);
            properties.put("otel.exporter.otlp.protocol", "http/protobuf");
            properties.put("otel.exporter.otlp.headers", "api-key=" + licenseKey);
            properties.put("otel.span.attribute.value.length.limit", "4095");
            properties.put("otel.exporter.otlp.compression", "gzip");

            final Object appName = agent.getConfig().getValue("app_name");
            properties.put("otel.service.name", appName.toString());

            String resourceAttributes = System.getProperty("otel.resource.attributes");
            if (resourceAttributes == null) {
                resourceAttributes = System.getenv("OTEL_RESOURCE_ATTRIBUTES");
            }
            resourceAttributes = (resourceAttributes == null ? "" : resourceAttributes + ",")
                    + "entity.guid=" + entityGuid;
            properties.put("otel.resource.attributes", resourceAttributes);
            builder.setConfig(DefaultConfigProperties.create(properties));
        }
        return builder;
    }
}
