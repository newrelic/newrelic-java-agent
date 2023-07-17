package com.nr.instrumentation.kafka.config;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.NewRelic;
import com.nr.instrumentation.kafka.ThreadFactories;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.utils.AppInfoParser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Emits custom events describing the configuration properties of any producer/consumer/admin client constructed
 * in this JVM on a regular cadence.
 */
public class ConfigEmitter {

    private static final ConfigEmitter INSTANCE = new ConfigEmitter();
    private static final List<String> environmentAttrs = NewRelic.getAgent().getConfig().getValue("kafka.config.environment_attributes",
                Collections.emptyList());

    public static ConfigEmitter get() {
        return INSTANCE;
    }

    private final Map<KafkaConfigKey, ConfigurationEvent> events = new ConcurrentHashMap<>();
    private final AtomicBoolean warnedAboutTooManyConfigurations = new AtomicBoolean(false);
    private volatile ConfigEmissionConfiguration config = null;
    protected ScheduledExecutorService emissionExecutor;

    ConfigEmitter() {
        // singleton constructor; package visible for testing
    }

    /**
     * A lazy initialization to be accommodating of when agent configuration might be
     * available relative to instantiation
     */
    private synchronized void checkInitialization() {
        if (config == null) {
            config = readConfiguration();
            if (config.isEnabled()) {
                scheduleEmissions();
            }
        }
    }

    protected void scheduleEmissions() {
        emissionExecutor = Executors.newSingleThreadScheduledExecutor(ThreadFactories.build("Kafka-configEmitter"));

        getAgent().getLogger().log(Level.INFO,
                "Kafka configurations will be emitted every {0}, starting in {1}. " +
                        "SSL reporting: {2}; " +
                        "SASL reporting: {3}",
                config.getReportingFrequency(), config.getReportingDelay(),
                config.isSslReportingEnabled(),
                config.isSaslReportingEnabled());

        emissionExecutor.scheduleAtFixedRate(
                this::reportConfigs,
                config.getReportingDelay().getSeconds(),
                config.getReportingFrequency().getSeconds(), TimeUnit.SECONDS);
    }

    protected ConfigEmissionConfiguration readConfiguration() {
        return ConfigEmissionConfiguration.read();
    }

    /**
     * Register a configuration whose properties should be emitted via custom events. A
     * configuration is keyed by the configuration type (i.e., the subclass of kafka configuration)
     * and the client ID. If a configuration has previously been registered with those identical traits
     * then this new configuration will be reported in its place. This should not be the normal pattern
     * for producer/client configuration, though, because producers/consumers should typically live
     * as long as the service creating them.
     *
     * @param clientId    the client ID corresponding to the configuration
     * @param kafkaConfig the configuration
     * @param kafkaConfigDefinition the configuration definitions
     */
    public void registerConfiguration(final String clientId, final AbstractConfig kafkaConfig, final ConfigDef kafkaConfigDefinition) {
        checkInitialization();
        if (config.isEnabled() && !wouldConfigurationReportingCapBeExceeded(clientId, kafkaConfig)) {

            getAgent().getLogger().log(Level.FINE, "registering {0} for client {1}",
                    kafkaConfig.getClass().getSimpleName(), clientId);

            // If this is the third time we've seen a registration under this key then this looks more like a pattern and
            // we'll suggest that maybe the client should be re-used.
            // We only report this once per client so that we don't burden such applications with excessive logging in
            // addition to its poor re-use habit
            if (installConfigurationEvent(clientId, kafkaConfig, kafkaConfigDefinition, ConfigScope.GENERAL).getNumRegistrations() == 3) {
                getAgent().getLogger().log(Level.WARNING,
                        "There have been multiple constructions for client.id {0}; " +
                                "are these consumers/producers being instantiated too frequently? Can they be re-used? " +
                                "Query the {1} attribute on configuration events to review construction counts.",
                        clientId,
                        ConfigurationEvent.ATTR_CONSTRUCTIONS);
            }

            // the above code is sufficient for noticing over-registration/over-construction, so we won't
            // log for these cases

            if (config.isGeneralOverriddenDefaultReportingEnabled()) {
                installConfigurationEvent(clientId, kafkaConfig, kafkaConfigDefinition, ConfigScope.GENERAL_OVERRIDDEN_DEFAULTS);
            }

            if (config.isSslReportingEnabled()) {
                installConfigurationEvent(clientId, kafkaConfig, kafkaConfigDefinition, ConfigScope.SSL);
            }

            if (config.isSaslReportingEnabled()) {
                installConfigurationEvent(clientId, kafkaConfig, kafkaConfigDefinition, ConfigScope.SASL);
            }
        }
    }

    /**
     * Would the configuration reporting cap be exceeded if some configuration were registered?
     * @param clientId the client id
     * @param kafkaConfig the config
     * @return if this config would not replace an already recorded config for the specified client and its
     * registration would lead to the configuration reporting cap being exceeded
     */
    private boolean wouldConfigurationReportingCapBeExceeded(final String clientId, final AbstractConfig kafkaConfig) {
        final KafkaConfigKey generalEventKey = new KafkaConfigKey(clientId, kafkaConfig, ConfigScope.GENERAL);
        if (!events.containsKey(generalEventKey)) { // never deny a key once registered; allow it to be updated

            // because we're not really controlling concurrency here, there's scope here for us to end up storing
            // more than the max reportable configurations, or maybe only reporting part of the configuration for
            // some clients, but that's okay, as this configuration is only really here to stop unbounded
            // growth, and we shouldn't be hitting these limits at all in normal usage where this reporting
            // is actually valuable
            if (events.size() >= config.getConfigurationEventCap()) {
                if (!warnedAboutTooManyConfigurations.getAndSet(true)) {
                    getAgent().getLogger().log(Level.WARNING,
                            "More than {0} Kafka client configurations have being used, possibly " +
                                    "indicating over-construction of clients with unique clientIds. " +
                                    "New clients will not have their configuration " +
                                    "state reported. Config reporting is only really designed for stable " +
                                    "producer/consumer sets -- consider disabling config reporting ({1}) or " +
                                    "increasing the client cap ({2})",
                            config.getClientCountCap(),
                            ConfigEmissionConfiguration.PROP_REPORTING_ENABLED,
                            ConfigEmissionConfiguration.PROP_REPORTING_CONFIGURATION_CAP);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Create an event corresponding to a configuration and record it for later emission.
     * Since kafka config is immutable, we can prepare the event for this once at registration
     * time and then re-use that event information for the next time we wish to emit it.
     *
     * @param clientId the clientId being used against the config
     * @param kafkaConfig the config
     * @param kafkaConfigDefinition the definition for the config (i.e., kafkaConfig.definition)
     * @param scope which configuration scope should the event include attributes for?
     * @return the created event
     */
    private ConfigurationEvent installConfigurationEvent(
            final String clientId,
            final AbstractConfig kafkaConfig,
            final ConfigDef kafkaConfigDefinition,
            final ConfigScope scope) {

        final ConfigurationEvent event = new ConfigurationEvent(
                configToEventType(kafkaConfig, scope), configToAttributes(clientId, kafkaConfig, kafkaConfigDefinition, scope));
        final ConfigurationEvent previous = events.put(new KafkaConfigKey(clientId, kafkaConfig, scope), event);
        if (previous != null) {
            event.replacing(previous);
        }
        return event;
    }

    private Map<String, Object> configToAttributes(
            final String clientId,
            final AbstractConfig config,
            final ConfigDef kafkaConfigDefinition,
            final ConfigScope scope) {

        final Map<String, ?> configValues = config.values();
        final Map<String, Object> attrs = new HashMap<>(configValues.size());
        // defaultValues() reconstructs a map each time, so we'll remember and re-use the result
        final Map<String, Object> kafkaConfigDefaults = kafkaConfigDefinition.defaultValues();
        for (final Map.Entry<String, ?> valEntry : configValues.entrySet()) {
            if (isPropertyReported(valEntry, scope, kafkaConfigDefaults)) {
                attrs.put(
                        scope.configEventAttrName(valEntry.getKey()),
                        getReportedConfigValue(valEntry, scope, kafkaConfigDefinition));
            }
        }

        attrs.put("clientId", clientId);
        attrs.put("kafka.version", AppInfoParser.getVersion());
        addEnvironmentAttributes(attrs);
        return attrs;
    }

    private void addEnvironmentAttributes(final Map<String, Object> attrs) {
        if (environmentAttrs != null && !environmentAttrs.isEmpty()) {
            for (String environmentAttr : environmentAttrs) {
                String value = readFromEnvironment(environmentAttr);
                if (value != null) {
                    attrs.put(environmentAttr, value);
                }
            }
        }
    }

    protected String readFromEnvironment(final String name) {
        return System.getenv(name);
    }

    /**
     * Get the reportable value for a configuration property -- converts Classes and Lists into useful
     * attribute values and reports default values for overridden default scopes
     *
     * @param configEntry the config entry to report
     * @param scope the scope
     * @param configDefinition the definition for the config, expressing default values
     * @return the attribute value for the config value
     */
    private Object getReportedConfigValue(
            final Map.Entry<String, ?> configEntry,
            final ConfigScope scope,
            final ConfigDef configDefinition) {

        final Object reportedValue;
        if (!scope.isOverriddenDefault()) {
            reportedValue = configEntry.getValue();
        } else {
            reportedValue = configDefinition.defaultValues().get(configEntry.getKey());
        }
        return configValueToReportedAttribute(reportedValue);
    }

    /**
     * Get the reportable value for a configuration property -- converts Classes and Lists into useful
     * attribute values
     * @param configValue the config value
     * @return the event attribute value for the config value
     */
     private Object configValueToReportedAttribute(final Object configValue) {
         // isPropertyReported() should never return true for a property that would yield a null
         // configuration value for its given scope, making us confident of this assertion. Null values are not
         // allowed against custom event attributes.
         assert configValue != null;

         Object attrVal = configValue;
         if (attrVal instanceof Class) {
             attrVal = ((Class<?>) attrVal).getName();
         } else if (attrVal instanceof List) {
             final List<?> list = (List<?>) attrVal;
             attrVal = list.stream()
                     .map(this::configValueToReportedAttribute)
                     .map(Object::toString)
                     .collect(Collectors.joining("|", "|", "|"));
         }
         // we'll let the agent coerce other data to applicable types
         return attrVal;
    }

    /**
     * Should this property be reported for a given scope? This should always be false for a property
     * that would yield a <code>null</code> reported value.
     *
     * @param kafkaConfigProperty the key/value pair identifying the Kafka config property to be tested
     * @param scope the scope that the property will be reported for
     * @param kafkaConfigDefaults a map of all default Kafka property values for the client configuration being reported
     * @return whether the property should be reported.
     */
    private boolean isPropertyReported(
            final Map.Entry<String, ?> kafkaConfigProperty,
            final ConfigScope scope,
            final Map<String, Object> kafkaConfigDefaults) {

        if (kafkaConfigProperty.getValue() == null) {
            // can't report null values! their absence in reporting equates to null though.
            return false;
        }

        if (CommonClientConfigs.CLIENT_ID_CONFIG.equals(kafkaConfigProperty.getKey())) {
            // we explicitly emit client.id as clientId, as the client.id configuration property may disagree
            // with both the actual clientId and the clientId we'll report when none is explicitly specified, just
            // due to the way Kafka clients generate clientIds when none are specified.
            // To avoid confusion we exclude c_clientId so that queries will always be written against clientId,
            // thus avoiding this potential discrepancy.
            return false;
        }

        if (!scope.ownsProperty(kafkaConfigProperty.getKey())) {
            return false;
        }

        return !scope.isOverriddenDefault() || isDefaultOverridden(kafkaConfigProperty, kafkaConfigDefaults);
    }

    private boolean isDefaultOverridden(
            final Map.Entry<String, ?> kafkaConfigProperty,
            final Map<String, Object> kafkaConfigDefaults) {

        final Object defaultValue = kafkaConfigDefaults.get(kafkaConfigProperty.getKey());
        if (defaultValue == null) {
            // we'll regard this as indicating that there is no default value, as opposed to the default
            // value being null -- this should be fine in practice given the general shape of Kafka configs
            return false;
        } else {
            return !Objects.equals(kafkaConfigProperty.getValue(), defaultValue);
        }
    }

    /**
     * Identify the custom event type that should be used given a particular flavor of config
     * and the scope being reported. We're limited to 64 attributes per event when using
     * custom events via the agent which means that a single producer event, for example, can't
     * hold all general config + SSL + SASL config -- there just aren't enough attributes! This is why
     * we use different events, via scope, to report SSL or SASL config. We use different events
     * for producer vs consumer events so that we maintain a somewhat consistent schema.
     * @param kafkaConfig the config
     * @param scope the scope of the config being reported
     * @return the event type
     */
    private String configToEventType(final AbstractConfig kafkaConfig, final ConfigScope scope) {
        final String clientType;
        if (kafkaConfig instanceof ConsumerConfig) {
            clientType = "KafkaConsumer";
        } else if (kafkaConfig instanceof ProducerConfig) {
            clientType = "KafkaProducer";
        } else if (kafkaConfig instanceof AdminClientConfig) {
            clientType = "KafkaAdmin";
        } else {
            clientType = "KafkaUnknownClient";
        }
        return clientType + scope.getEventNameContribution();
    }

    protected void reportConfigs() {
        getAgent().getLogger().log(Level.FINE, "reporting {0} kafka config event(s)", events.size());
        for (final ConfigurationEvent e : events.values()) {
            getAgent().getInsights().recordCustomEvent(e.getEventType(), e.getAttributes());
        }
    }

    protected Agent getAgent() {
        return NewRelic.getAgent();
    }

}
