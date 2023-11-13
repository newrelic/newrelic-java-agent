package com.newrelic.agent.stats.dimensional;

import com.newrelic.agent.Harvestable;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.model.CustomInsightsEvent;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.EventService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.transport.CollectorMethods;
import com.newrelic.agent.transport.HttpError;
import com.newrelic.api.agent.metrics.Counter;
import com.newrelic.api.agent.metrics.Meter;
import com.newrelic.api.agent.metrics.Summary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class MeterService extends AbstractService implements Meter, EventService {

    private volatile boolean enabled = true;
    private final List<Closeable> closeables = new CopyOnWriteArrayList<>();
    private volatile int maxSamplesStored;
    // optimize when we cache a single instance of a map multiple times so that we only compute the hash once
    private final CachingMapHasher mapHasher = new CachingMapHasher(SimpleMapHasher.INSTANCE);
    // hash of attributes to map of metric aggregates
    private final AtomicReference<Map<Long, AttributeBucket>> bucketByAttributeHash = new AtomicReference<>(new ConcurrentHashMap<>());
    private final Map<String, MetricType> nameToMetricType = new ConcurrentHashMap<>();
    private final Counter noOpCounter = (value, attributes) -> {};
    private final Summary noOpSummary = (value, attributes) -> {};

    public MeterService() {
        super(MeterService.class.getName());
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
        final MetricType existingType = nameToMetricType.computeIfAbsent(name, key -> MetricType.count);
        if (!MetricType.count.equals(existingType)) {
            logger.log(Level.SEVERE, "Meter {0} has already been defined as type {1}", name, existingType.name());
            return noOpCounter;
        }
        return new Counter() {
            @Override
            public void add(long value) {
                getAttributeBucket(Collections.emptyMap()).getCounter(name).add(value);
            }

            @Override
            public void add(long value, Map<String, ?> attributes) {
                getAttributeBucket(attributes).getCounter(name).add(value);
            }
        };
    }

    @Override
    public Summary newSummary(String name) {
        final MetricType existingType = nameToMetricType.computeIfAbsent(name, key -> MetricType.summary);
        if (!MetricType.summary.equals(existingType)) {
            logger.log(Level.SEVERE, "Meter {0} has already been defined as type {1}", name, existingType.name());
            return noOpSummary;
        }
        return new Summary() {
            @Override
            public void add(double value) {
                add(value, Collections.emptyMap());
            }

            @Override
            public void add(double value, Map<String, ?> attributes) {
                getAttributeBucket(attributes).getSummary(name).add(value);
            }
        };
    }

    //********************** End Meter APIs *************************/

    Harvest harvestEvents() {
        final Collection<CustomInsightsEvent> events = new ArrayList<>();
        final Map<Long, AttributeBucket> buckets = this.bucketByAttributeHash.getAndSet(new ConcurrentHashMap<>());
        mapHasher.reset();

        buckets.values().forEach(bucket -> bucket.harvest(events));
        return new Harvest(events, buckets);
    }

    @Override
    public void harvestEvents(String appName) {
        final Harvest harvest = harvestEvents();
        final Collection<CustomInsightsEvent> events = harvest.events;

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
                    merge(harvest.buckets);
                    getLogger().log(Level.FINE, "Unable to send dimensional metrics.  Unsent metrics will be included in the next harvest.");
                }
            } catch (Exception e) {
                getLogger().log(Level.FINE, "Unable to send dimensional metrics.  Dropping data.");
            }
        }
    }

    private void merge(Map<Long, AttributeBucket> buckets) {
        buckets.forEach((name, bucket) -> bucketByAttributeHash.get().merge(name, bucket, AttributeBucket::merge));
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

    AttributeBucket getAttributeBucket(Map<String, ?> attributes) {
        final long hash = mapHasher.hash(attributes);
        return bucketByAttributeHash.get().computeIfAbsent(hash, key -> {
            mapHasher.addHash(attributes, hash);
            return new AttributeBucket(attributes);
        });
    }

    interface Closeable {
        void close();
    }

    static class Harvest {
        final Collection<CustomInsightsEvent> events;
        final Map<Long, AttributeBucket> buckets;

        public Harvest(Collection<CustomInsightsEvent> events, Map<Long, AttributeBucket> buckets) {
            this.events = events;
            this.buckets = buckets;
        }
    }
}
