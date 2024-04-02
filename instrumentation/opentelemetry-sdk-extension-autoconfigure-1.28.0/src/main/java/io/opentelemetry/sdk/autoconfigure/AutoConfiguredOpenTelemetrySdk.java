package io.opentelemetry.sdk.autoconfigure;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.logging.Level;

@Weave(type = MatchType.ExactClass)
public class AutoConfiguredOpenTelemetrySdk {

    public static AutoConfiguredOpenTelemetrySdkBuilder builder() {
        final AutoConfiguredOpenTelemetrySdkBuilder builder = Weaver.callOriginal();
        Boolean autoConfigure = NewRelic.getAgent().getConfig().getValue("opentelemetry.sdk.autoconfigure.enabled");
        if (autoConfigure == null || autoConfigure) {
            NewRelic.getAgent().getLogger().log(Level.INFO, "Appending OpenTelemetry SDK customizers");
            builder.addPropertiesCustomizer(new PropertiesCustomizer());
            builder.addResourceCustomizer(new ResourceCustomer());
        }
        return builder;
    }
}
