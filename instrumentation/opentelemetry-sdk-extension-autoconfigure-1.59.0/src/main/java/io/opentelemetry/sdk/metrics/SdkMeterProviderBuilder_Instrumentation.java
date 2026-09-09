package io.opentelemetry.sdk.metrics;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.utils.metrics.MetricReaderWrapper;
import com.nr.agent.instrumentation.utils.metrics.ServerlessMetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;

import java.time.Duration;

@Weave(originalName = "io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder", type = MatchType.ExactClass)
public class SdkMeterProviderBuilder_Instrumentation {

    public SdkMeterProviderBuilder_Instrumentation registerMetricReader(MetricReader reader) {
        return Weaver.callOriginal();
    }

    public SdkMeterProvider build() {
        PeriodicMetricReader periodicMetricReader = PeriodicMetricReader.builder(new ServerlessMetricExporter())
                .setInterval(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999))
                .build();

        MetricReader metricReader = new MetricReaderWrapper(periodicMetricReader);
        this.registerMetricReader(metricReader);

        SdkMeterProvider provider = Weaver.callOriginal();
        AgentBridge.serverlessApi.addMetricCollector(metricReader, o -> {
            if (o instanceof MetricReader) {
                ((MetricReader) o).forceFlush();
            }
        });

        return provider;
    }

}
