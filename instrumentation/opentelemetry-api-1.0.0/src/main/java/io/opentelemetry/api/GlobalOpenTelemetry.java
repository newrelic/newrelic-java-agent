package io.opentelemetry.api;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.io.ByteArrayOutputStream;
import java.util.logging.Level;

@Weave(type = MatchType.ExactClass)
public class GlobalOpenTelemetry {

    public static OpenTelemetry get() {
        if (Boolean.getBoolean("otel.java.global-autoconfigure.enabled") &&
                System.getProperty("otel.exporter.otlp.endpoint") == null &&
                System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT") == null) {

            final Agent agent = NewRelic.getAgent();
            final String host = agent.getConfig().getValue("host");
            final String endpoint = "https://" + host + ":443";

            if (!endpoint.equals(System.getProperty("otel.exporter.otlp.endpoint"))) {
                agent.getLogger().log(Level.INFO, "Auto-initializing OpenTelemetry SDK");
                try {
                    final String licenseKey = agent.getConfig().getValue("license_key");
                    System.setProperty("otel.exporter.otlp.headers", "api-key=" + licenseKey);
                    System.setProperty("otel.exporter.otlp.endpoint", endpoint);
                    System.setProperty("otel.exporter.otlp.protocol", "http/protobuf");
                    System.setProperty("otel.exporter.otlp.headers", "api-key=" + licenseKey);
                    System.setProperty("otel.span.attribute.value.length.limit", "4095");
                    System.setProperty("otel.exporter.otlp.compression", "gzip");

                    final Object appName = agent.getConfig().getValue("app_name");
                    System.setProperty("otel.service.name", appName.toString());
                } catch (Exception e) {
                    agent.getLogger().log(Level.FINE, e, e.getMessage());
                }
            }
        }
        return Weaver.callOriginal();
    }
}
