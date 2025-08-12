package io.opentelemetry.sdk.autoconfigure;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.View;

import java.util.List;
import java.util.function.BiFunction;
import com.newrelic.agent.service.ServiceFactory;

public class MeterProviderCustomizer implements BiFunction<SdkMeterProviderBuilder, ConfigProperties, SdkMeterProviderBuilder> {

    private final List<String> excludedMeters = ServiceFactory.getConfigService().getDefaultAgentConfig().getOtelConfig().getExcludedMeters();

    @Override
    public SdkMeterProviderBuilder apply(SdkMeterProviderBuilder sdkMeterProviderBuilder, ConfigProperties configProperties) {
        for (String meterName : excludedMeters) {
            sdkMeterProviderBuilder.registerView(
                    InstrumentSelector.builder().setMeterName(meterName).build(),
                    View.builder().setAggregation(Aggregation.drop()).build()
            );
        }
        return sdkMeterProviderBuilder;
    }

}
