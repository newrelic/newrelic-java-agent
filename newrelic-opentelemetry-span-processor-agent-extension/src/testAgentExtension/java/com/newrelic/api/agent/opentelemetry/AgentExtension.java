///*
// *
// *  * Copyright 2020 New Relic Corporation. All rights reserved.
// *  * SPDX-License-Identifier: Apache-2.0
// *
// */
//
//package com.newrelic.api.agent.opentelemetry;
//
//import com.google.protobuf.InvalidProtocolBufferException;
//import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
//import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
//import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
//import org.junit.jupiter.api.extension.AfterAllCallback;
//import org.junit.jupiter.api.extension.BeforeAllCallback;
//import org.junit.jupiter.api.extension.BeforeEachCallback;
//import org.junit.jupiter.api.extension.ExtensionContext;
//import org.mockserver.integration.ClientAndServer;
//import org.mockserver.model.HttpRequest;
//import org.mockserver.model.HttpResponse;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
//public class AgentExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
//
//    private static final int EXPORTER_ENDPOINT_PORT = 4318;
//
//    private final List<ExportMetricsServiceRequest> metricRequests = new ArrayList<>();
//    private final List<ExportLogsServiceRequest> logRequests = new ArrayList<>();
//    private final List<ExportTraceServiceRequest> traceRequests = new ArrayList<>();
//
//    private ClientAndServer collectorServer;
//
//    @Override
//    public void beforeAll(ExtensionContext context) {
//        collectorServer = ClientAndServer.startClientAndServer(EXPORTER_ENDPOINT_PORT);
//        collectorServer.when(HttpRequest.request()).respond(HttpResponse.response().withStatusCode(200));
//    }
//
//    @Override
//    public void afterAll(ExtensionContext context) {
//        collectorServer.stop();
//    }
//
//    @Override
//    public void beforeEach(ExtensionContext context) {
//        metricRequests.clear();
//        logRequests.clear();
//        traceRequests.clear();
//    }
//
//    public List<ExportMetricsServiceRequest> getMetricRequests() {
//        processRequests();
//        return metricRequests;
//    }
//
//    public List<ExportLogsServiceRequest> getLogRequests() {
//        processRequests();
//        return logRequests;
//    }
//
//    public List<ExportTraceServiceRequest> getTraceRequests() {
//        processRequests();
//        return traceRequests;
//    }
//
//    private synchronized void processRequests() {
//        HttpRequest[] httpRequests = collectorServer.retrieveRecordedRequests(HttpRequest.request());
//        collectorServer.clear(HttpRequest.request());
//
//        metricRequests.addAll(extractMetricRequests(httpRequests));
//        logRequests.addAll(extractLogRequests(httpRequests));
//        traceRequests.addAll(extractTraceRequests(httpRequests));
//    }
//
//    private static List<ExportMetricsServiceRequest> extractMetricRequests(HttpRequest[] requests) {
//        return Arrays.stream(requests)
//                .filter(request -> request.getPath().getValue().contains("/v1/metrics"))
//                .map(request -> {
//                    try {
//                        return ExportMetricsServiceRequest.parseFrom(request.getBody().getRawBytes());
//                    } catch (InvalidProtocolBufferException e) {
//                        return null;
//                    }
//                })
//                .collect(Collectors.toList());
//    }
//
//    private static List<ExportLogsServiceRequest> extractLogRequests(HttpRequest[] requests) {
//        return Arrays.stream(requests)
//                .filter(request -> request.getPath().getValue().contains("/v1/logs"))
//                .map(request -> {
//                    try {
//                        return ExportLogsServiceRequest.parseFrom(request.getBody().getRawBytes());
//                    } catch (InvalidProtocolBufferException e) {
//                        return null;
//                    }
//                })
//                .collect(Collectors.toList());
//    }
//
//    private static List<ExportTraceServiceRequest> extractTraceRequests(HttpRequest[] requests) {
//        return Arrays.stream(requests)
//                .filter(request -> request.getPath().getValue().contains("/v1/traces"))
//                .map(request -> {
//                    try {
//                        return ExportTraceServiceRequest.parseFrom(request.getBody().getRawBytes());
//                    } catch (InvalidProtocolBufferException e) {
//                        return null;
//                    }
//                })
//                .collect(Collectors.toList());
//    }
//}
