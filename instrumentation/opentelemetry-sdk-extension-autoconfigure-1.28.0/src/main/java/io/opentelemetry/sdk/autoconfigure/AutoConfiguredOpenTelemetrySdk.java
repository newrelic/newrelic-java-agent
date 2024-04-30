package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.logging.Level;

@Weave(type = MatchType.ExactClass)
public class AutoConfiguredOpenTelemetrySdk {

    public static AutoConfiguredOpenTelemetrySdkBuilder builder() {
        final AutoConfiguredOpenTelemetrySdkBuilder builder = Weaver.callOriginal();
        final Boolean autoConfigure = NewRelic.getAgent().getConfig().getValue("opentelemetry.sdk.autoconfigure.enabled");
        if (autoConfigure == null || autoConfigure) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "Appending OpenTelemetry SDK customizers");
            builder.addPropertiesCustomizer(properties -> OpenTelemetrySDKCustomizer.applyProperties(properties, NewRelic.getAgent()));
            builder.addResourceCustomizer((resource, props) -> OpenTelemetrySDKCustomizer.applyResources(resource,
                    AgentBridge.getAgent(), NewRelic.getAgent().getLogger()));

            // span support
            builder.addTracerProviderCustomizer(OpenTelemetrySDKCustomizer::applyTraceProviderCustomizer);
            builder.addSpanExporterCustomizer(SpanExportSuppressor::new);
        }
        return builder;
    }
}
