///*
// * Copyright The OpenTelemetry Authors
// * SPDX-License-Identifier: Apache-2.0
// */
//
//package com.newrelic.opentelemetry;
//
//import com.google.auto.service.AutoService;
//import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
//import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
//import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
//import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
//import io.opentelemetry.sdk.trace.SpanLimits;
//import java.util.HashMap;
//import java.util.Map;
//
//@AutoService(AutoConfigurationCustomizerProvider.class)
//public class DemoAutoConfigurationCustomizerProvider
//    implements AutoConfigurationCustomizerProvider {
//
//  @Override
//  public void customize(AutoConfigurationCustomizer autoConfiguration) {
//    autoConfiguration
//        .addTracerProviderCustomizer(this::configureSdkTracerProvider);
////        .addPropertiesSupplier(this::getDefaultProperties);
//  }
//
//  private SdkTracerProviderBuilder configureSdkTracerProvider(
//      SdkTracerProviderBuilder tracerProvider, ConfigProperties config) {
//
//    return tracerProvider
//        .setIdGenerator(new DemoIdGenerator())
//        .setSpanLimits(SpanLimits.builder().setMaxNumberOfAttributes(1024).build())
//        .addSpanProcessor(new DemoSpanProcessor());
//  }
//
//  private Map<String, String> getDefaultProperties() {
//    Map<String, String> properties = new HashMap<>();
//    properties.put("otel.exporter.otlp.endpoint", "http://backend:8080");
//    properties.put("otel.exporter.otlp.insecure", "true");
//    properties.put("otel.config.max.attrs", "16");
//    properties.put("otel.traces.sampler", "demo");
//    return properties;
//  }
//}
