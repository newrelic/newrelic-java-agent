package com.newrelic.agent.stats.dimensional;

import com.google.common.collect.ImmutableList;
import com.newrelic.agent.Harvestable;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.EventService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.transport.CollectorMethods;
import com.newrelic.agent.transport.HttpError;
import com.newrelic.api.agent.metrics.Counter;
import com.newrelic.api.agent.metrics.Meter;
import com.newrelic.api.agent.metrics.Summary;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.HistogramData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.data.SumData;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MeterService extends AbstractService implements Meter, EventService {

    private volatile boolean enabled = true;
    private final List<Closeable> closeables = new CopyOnWriteArrayList<>();
    private volatile int maxSamplesStored;

    private final io.opentelemetry.api.metrics.Meter meter;
    final Supplier<Collection<MetricData>> metricDataSupplier;

    public MeterService() {
        super(MeterService.class.getName());
        final AtomicReference<CollectionRegistration> collectionRegistrationReference =
                new AtomicReference<>(CollectionRegistration.noop());
        this.metricDataSupplier = () -> collectionRegistrationReference.get().collectAllMetrics();
        final MetricReader metricReader = new MetricReader() {
            @Override
            public void register(CollectionRegistration registration) {
                collectionRegistrationReference.set(registration);
            }

            @Override
            public CompletableResultCode forceFlush() {
                return CompletableResultCode.ofSuccess();
            }

            @Override
            public CompletableResultCode shutdown() {
                return CompletableResultCode.ofSuccess();
            }

            @Override
            public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
                return AggregationTemporality.DELTA;
            }
        };
        final SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder().registerMetricReader(metricReader).build();
        meter = sdkMeterProvider.meterBuilder("newrelic").build();
    }

    @Override
    protected void doStart() throws Exception {
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        enabled = config.getInsightsConfig().isEnabled();
    }

    @Override
    protected void doStop() throws Exception {
        closeables.forEach(Closeable::close);
        closeables.clear();
    }

    @Override
    public int getMaxSamplesStored() {
        return maxSamplesStored;
    }

    @Override
    public void setMaxSamplesStored(int maxSamplesStored) {
        this.maxSamplesStored = maxSamplesStored;
    }

    @Override
    public void clearReservoir() {
    }

    //**********************  Meter APIs *************************/

    @Override
    public Counter newCounter(String name) {
        final LongCounter longCounter = meter.counterBuilder(name).build();
        return (increment, attributes) -> longCounter.add(increment, toAttributes(attributes));
    }

    @Override
    public Summary newSummary(String name) {
        // we want a summary, so we call setExplicitBucketBoundariesAdvice with an empty list
        final DoubleHistogram doubleHistogram = meter.histogramBuilder(name)
                .setExplicitBucketBoundariesAdvice(Collections.emptyList()).build();
        return (value, attributes) -> doubleHistogram.record(value, toAttributes(attributes));
    }

    //********************** End Meter APIs *************************/

    static Attributes toAttributes(Map<String,?> map) {
        final AttributesBuilder builder = Attributes.builder();
        map.forEach((key, value) -> {
            if (value instanceof String) {
                builder.put(key, value.toString());
            } else if (value instanceof Number) {
                final Number number = (Number) value;
                if (value instanceof Float | value instanceof Double) {
                    builder.put(key, number.doubleValue());
                } else {
                    builder.put(key, number.longValue());
                }
            } else if (value instanceof Boolean) {
                builder.put(key, ((Boolean) value).booleanValue());
            } else {
                builder.put(key, value.toString());
            }
        });
        return builder.build();
    }

    private static Map<String, Object> toMap(Attributes attributes) {
        return attributes.asMap().entrySet().stream().collect(
                Collectors.toMap(entry -> entry.getKey().getKey(), Map.Entry::getValue));
    }

    @Override
    public void harvestEvents(String appName) {
        final Collection<MetricData> metricData = metricDataSupplier.get();
        final List<CustomInsightsEvent> events = toEvents(metricData);

        System.err.println("Harvested metrics: " + events.size());
        if (!events.isEmpty()) {
            try {
                ServiceFactory.getRPMServiceManager()
                        .getRPMService()
                        .sendCustomAnalyticsEvents(1000, events.size(), events);
            } catch (HttpError e) {
                if (e.discardHarvestData()) {
                    getLogger().log(Level.FINE, "Unable to send dimensional metrics.  Dropping data.");
                } else {
                    // FIXME send later
                    getLogger().log(Level.FINE, "Unable to send dimensional metrics.  Unsent metrics will be included in the next harvest.");
                }
            } catch (Exception e) {
                getLogger().log(Level.FINE, "Unable to send dimensional metrics.  Dropping data.");
            }
        }
    }

    static List<CustomInsightsEvent> toEvents(Collection<MetricData> metricData) {
        final List<CustomInsightsEvent> events = new ArrayList<>();
        metricData.forEach(md -> {
            switch (md.getType()) {
                case LONG_SUM:
                    final SumData<LongPointData> longSumData = md.getLongSumData();
                    longSumData.getPoints().forEach(pointData -> {
                        final Map<String, Object> intrinsics = getIntrinsics(md.getName(), pointData);
                        intrinsics.put("metric.count", pointData.getValue());
                        events.add(createEvent(pointData, intrinsics));
                    });
                    break;
                case HISTOGRAM:
                    final HistogramData histogramData = md.getHistogramData();
                    histogramData.getPoints().forEach(pointData -> {
                        final Map<String, Object> intrinsics = getIntrinsics(md.getName(), pointData);
                        intrinsics.put("metric.summary", toSummary(pointData));
                        events.add(createEvent(pointData, intrinsics));
                    });
            }
        });
        return events;
    }

    private static CustomInsightsEvent createEvent(PointData pointData, Map<String, Object> intrinsics) {
        final long timestamp = TimeUnit.NANOSECONDS.toMillis(pointData.getStartEpochNanos());
        return new CustomInsightsEvent("Metric", timestamp, toMap(pointData.getAttributes()), intrinsics, DistributedTraceServiceImpl.nextTruncatedFloat());
    }

    private static List<Number> toSummary(HistogramPointData pointData) {
        return ImmutableList.of(pointData.getCount(), pointData.getSum(), pointData.getMin(), pointData.getMax());
    }

    static Map<String, Object> getIntrinsics(String metricName, PointData pointData) {
        final Map<String, Object> intrinsics = new HashMap<>();
        intrinsics.put("metric.name", metricName);
        final long durationMs = TimeUnit.NANOSECONDS.toMillis(pointData.getEpochNanos() - pointData.getStartEpochNanos());
        intrinsics.put("duration.ms", durationMs);
        return intrinsics;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void addHarvestableToService(String appName) {
        if (!enabled) {
            return;
        }
        final String defaultAppName = ServiceFactory.getRPMService().getApplicationName();
        if (appName.equals(defaultAppName)) {
            final Harvestable harvestable = new Harvestable(this, appName) {
                @Override
                public String getEndpointMethodName() {
                    return CollectorMethods.CUSTOM_EVENT_DATA;
                }

                @Override
                public int getMaxSamplesStored() {
                    return ServiceFactory.getConfigService().getDefaultAgentConfig().getInsightsConfig().getMaxSamplesStored();
                }
            };
            ServiceFactory.getHarvestService().addHarvestable(harvestable);
            closeables.add(() -> ServiceFactory.getHarvestService().removeHarvestable(harvestable));
        }
    }

    interface Closeable {
        void close();
    }
}
