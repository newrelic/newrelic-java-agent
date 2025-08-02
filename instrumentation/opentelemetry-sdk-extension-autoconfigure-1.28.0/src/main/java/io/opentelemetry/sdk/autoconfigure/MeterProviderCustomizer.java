package io.opentelemetry.sdk.autoconfigure;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.View;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

public class MeterProviderCustomizer implements BiFunction<SdkMeterProviderBuilder, ConfigProperties, SdkMeterProviderBuilder> {

    @Override
    public SdkMeterProviderBuilder apply(SdkMeterProviderBuilder sdkMeterProviderBuilder, ConfigProperties configProperties) {
        for (String meterName : getExcludedMeters()){
            sdkMeterProviderBuilder.registerView(
                    InstrumentSelector.builder().setMeterName(meterName).build(),
                    View.builder().setAggregation(Aggregation.drop()).build()
            );
        }
        return sdkMeterProviderBuilder;
    }

    private List<String> getExcludedMeters(){
        return Arrays.asList("otel.demo");
    }
}
