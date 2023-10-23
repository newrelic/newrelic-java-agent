package com.nr.instrumentation.kafka.config;

import com.newrelic.api.agent.NewRelic;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;

/**
 * Configuration for behavior of {@link ConfigEmitter}
 */
class ConfigEmissionConfiguration {

    public static final String PROP_REPORTING_CONFIGURATION_CAP = "kafka.config.reporting.configurationCap";
    public static final String PROP_REPORTING_ENABLED = "kafka.config.reporting.enabled";
    public static final String PROP_OVERRIDDEN_GENERAL_DEFAULTS = "kafka.config.events.overriddenGeneralDefaults";
    public static final String PROP_EVENTS_SSL = "kafka.config.events.ssl";
    public static final String PROP_EVENTS_SASL = "kafka.config.events.sasl";
    public static final String PROP_REPORTING_DELAY = "kafka.config.reporting.delay";
    public static final String PROP_REPORTING_FREQUENCY = "kafka.config.reporting.frequency";

    private final boolean enabled;
    private final boolean overriddenGeneralDefaultsReported;
    private final boolean sslReportingEnabled;
    private final boolean saslReportingEnabled;
    private final Duration reportingDelay;
    private final Duration reportingFrequency;
    private final int clientCountCap;

    ConfigEmissionConfiguration(
            final boolean enabled,
            final boolean overriddenGeneralDefaultsReported,
            final boolean sslReportingEnabled,
            final boolean saslReportingEnabled,
            final Duration reportingDelay,
            final Duration reportingFrequency,
            final int clientCountCap) {
        this.enabled = enabled;
        this.overriddenGeneralDefaultsReported = overriddenGeneralDefaultsReported;
        this.sslReportingEnabled = sslReportingEnabled;
        this.saslReportingEnabled = saslReportingEnabled;
        this.reportingDelay = reportingDelay;
        this.reportingFrequency = reportingFrequency;
        this.clientCountCap = clientCountCap;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Should ssl.* SSL connection configuration events be reported?
     * @return if SSL reporting is enabled
     */
    public boolean isSslReportingEnabled() {
        return sslReportingEnabled;
    }

    /**
     * Should sasl.* SASL connection configuration events be reported?
     * @return if SASL reporting is enabled
     */
    public boolean isSaslReportingEnabled() {
        return saslReportingEnabled;
    }

    /**
     * Get the delay before the first configuration report is made after the first registration
     * @return the delay, may be <code>null</code> when reporting is disabled
     */
    public Duration getReportingDelay() {
        return reportingDelay;
    }

    /**
     * Get the interval between configuration reports after the initial report
     * @return the frequency, may be <code>null</code> when reporting is disabled
     */
    public Duration getReportingFrequency() {
        return reportingFrequency;
    }

    /**
     * Get a cap on the number clients for which configuration is reportable. The intention here is to cap
     * configuration reporting so that if an application is over-creating consumers/producers/admins with
     * unique client IDs then we won't accrue a ridiculous number of configurations (we never remove them)
     * and we won't dominate event reporting. This isn't a strict cap (it's possible we may report more clients
     * than the cap indicates), but mainly a release valve to ensure that we don't accumulate a never-ending list
     * of configuration events should client construction not be bounded. The default value should be sufficient
     * for most well-behaved services, but otherwise this should be set to accommodate the number of expected
     * Kafka clients during a service lifetime (assuming it's reasonable!), possibly with a bit of extra slop.
     * @return the maximum number of configurations to report
     */
    public int getClientCountCap() {
        return clientCountCap;
    }

    /**
     * Given the entire configuration, how many configuration events do we anticipate actually needing to store?
     * @return the anticipated max number of events
     */
    public int getConfigurationEventCap() {
        return clientCountCap * (1 +
                (isGeneralOverriddenDefaultReportingEnabled() ? 1 : 0) +
                (isSaslReportingEnabled() ? 1 : 0) +
                (isSslReportingEnabled() ? 1 : 0));
    }

    public boolean isGeneralOverriddenDefaultReportingEnabled() {
        return overriddenGeneralDefaultsReported;
    }

    public static ConfigEmissionConfiguration read() {
        final boolean enabled = parseBoolSetting(PROP_REPORTING_ENABLED, true);
        if (enabled) {
            return new ConfigEmissionConfiguration(true,
                    parseBoolSetting(PROP_OVERRIDDEN_GENERAL_DEFAULTS, true),
                    parseBoolSetting(PROP_EVENTS_SSL, false),
                    parseBoolSetting(PROP_EVENTS_SASL, false),
                    parseDurationSetting(PROP_REPORTING_DELAY, Duration.ofMinutes(5)),
                    parseDurationSetting(PROP_REPORTING_FREQUENCY, Duration.ofHours(1)),
                    parseIntSetting(PROP_REPORTING_CONFIGURATION_CAP, 15));
        } else {
            return new ConfigEmissionConfiguration(false, false, false, false, null, null, 0);
        }

    }

    private static Duration parseDurationSetting(final String propertyName, final Duration defaultDuration) {
        Duration duration = defaultDuration;
        final String val = NewRelic.getAgent().getConfig().getValue(propertyName);
        if (val != null) {
            try {
                duration = Duration.parse(val);
            } catch (final DateTimeParseException e) {
                NewRelic.getAgent().getLogger().log(Level.SEVERE,
                        "Could not parse duration value {0} for setting {1}, reverting to default. " +
                                "Values should use ISO-8601 duration format. Message: {2}",
                        val, propertyName, e.getMessage());
                // retain default duration and drop through
            }
        }
        if (duration.isNegative()) {
            NewRelic.getAgent().getLogger().log(Level.SEVERE,
                    "Negative duration {0} specified for {1}, reverting to default",
                    val, propertyName);
            duration = defaultDuration;
        }
        return duration;
    }

    private static boolean parseBoolSetting(final String propertyName, final boolean defaultVal) {
        try {
            return NewRelic.getAgent().getConfig().getValue(propertyName, defaultVal);
        } catch (final ClassCastException cce) {
            NewRelic.getAgent().getLogger().log(Level.SEVERE,
                    "Expected boolean value specified for {}, but it wasn't. Ignoring and using default of {}",
                    propertyName, defaultVal);
            return defaultVal;
        }
    }

    private static int parseIntSetting(final String propertyName, final int defaultVal) {
        try {
            return NewRelic.getAgent().getConfig().getValue(propertyName, defaultVal);
        } catch (final ClassCastException cce) {
            NewRelic.getAgent().getLogger().log(Level.SEVERE,
                    "Expected int value specified for {}, but it wasn't. Ignoring and using default of {}",
                    propertyName, defaultVal);
            return defaultVal;
        }

    }


}
