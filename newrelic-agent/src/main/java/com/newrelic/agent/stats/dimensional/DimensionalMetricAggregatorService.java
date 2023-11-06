package com.newrelic.agent.stats.dimensional;

import com.newrelic.agent.Harvestable;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.EventService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.transport.CollectorMethods;
import com.newrelic.api.agent.DimensionalMetricAggregator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class DimensionalMetricAggregatorService extends AbstractService implements DimensionalMetricAggregator, EventService {

    private volatile boolean enabled = true;
    private final List<Closeable> closeables = new CopyOnWriteArrayList<>();
    private volatile int maxSamplesStored;
    private final AtomicReference<Map<String, MetricAggregates>> aggregatesByMetricName = new AtomicReference<>(new ConcurrentHashMap<>());
    private final static Aggregator NO_OP_AGGREGATOR = new Aggregator() {
        final Measure NO_OP_MEASURE = new Measure() {};

        @Override
        public Measure getMeasure(Map<String, Object> attributes) {
            return NO_OP_MEASURE;
        }
    };

    public DimensionalMetricAggregatorService() {
        super(DimensionalMetricAggregatorService.class.getName());
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

    Collection<CustomInsightsEvent> harvestEvents() {
        final Map<String, MetricAggregates> aggregates = this.aggregatesByMetricName.getAndSet(new ConcurrentHashMap<>());

        final Collection<CustomInsightsEvent> events = new ArrayList<>();
        aggregates.values().forEach(agg -> agg.harvest(events));
        return events;
    }

    @Override
    public void harvestEvents(String appName) {
        final Collection<CustomInsightsEvent> events = harvestEvents();

        if (!events.isEmpty()) {
            try {
                ServiceFactory.getRPMServiceManager()
                        .getRPMService()
                        .sendCustomAnalyticsEvents(0, events.size(), events);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
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

    Aggregator getMetricAggregates(String name, MetricType type) {
        MetricAggregates metricAggregates = aggregatesByMetricName.get().computeIfAbsent(name, n -> new MetricAggregates(name, type));
        if (metricAggregates.getMetricType().equals(type)) {
            return metricAggregates;
        } else {
            // report error
            return NO_OP_AGGREGATOR;
        }
    }

    //**********************  Metric APIs *************************/

    @Override
    public void addToSummary(String name, Map<String, Object> attributes, double value) {
        getMetricAggregates(name, MetricType.summary).getMeasure(attributes).addToSummary(value);
    }

    @Override
    public void incrementCounter(String name, Map<String, Object> attributes) {
        getMetricAggregates(name, MetricType.count).getMeasure(attributes).incrementCount(1);
    }

    @Override
    public void incrementCounter(String name, Map<String, Object> attributes, int count) {
        getMetricAggregates(name, MetricType.count).getMeasure(attributes).incrementCount(count);
    }

    interface Closeable {
        void close();
    }
}
