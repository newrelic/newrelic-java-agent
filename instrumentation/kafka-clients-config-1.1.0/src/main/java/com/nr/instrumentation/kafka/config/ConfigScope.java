package com.nr.instrumentation.kafka.config;

import java.util.function.Predicate;

/**
 * Identifies a configuration scope within a specific configuration.
 * SSL and SASL configuration elements are considered to be in different scopes
 * than all other general configuration, and their reporting is emitted under separate
 * events according to extension configuration.
 */
public enum ConfigScope {

    GENERAL("Config", Constants.PREFIX_CONFIG_VALUE, Constants.IS_GENERAL_CONFIG),
    // i.e., report the Kafka default values that have been overridden by General config properties
    GENERAL_OVERRIDDEN_DEFAULTS("OverriddenDefaultConfig", Constants.PREFIX_DEFAULT_VALUE,
            Constants.IS_GENERAL_CONFIG, true),
    SSL("SslConfig", Constants.PREFIX_CONFIG_VALUE, propName -> propName.startsWith("ssl.")),
    SASL("SaslConfig", Constants.PREFIX_CONFIG_VALUE, propName -> propName.startsWith("sasl."));

    private final String eventNameContribution;
    private final String attrValuePrefix;
    private final boolean overriddenDefault;
    private Predicate<String> propertyOwnershipPredicate;

    ConfigScope(
            final String eventNameContribution,
            final String attrValuePrefix,
            final Predicate<String> propertyOwnershipPredicate) {
        this(eventNameContribution, attrValuePrefix, propertyOwnershipPredicate, false);
    }

    ConfigScope(
            final String eventNameContribution,
            final String attrValuePrefix,
            final Predicate<String> propertyOwnershipPredicate,
            final boolean overriddenDefault) {

        this.eventNameContribution = eventNameContribution;
        this.attrValuePrefix = attrValuePrefix;
        this.propertyOwnershipPredicate = propertyOwnershipPredicate;
        this.overriddenDefault = overriddenDefault;
    }

    public String configEventAttrName(final String propertyName) {
        return attrValuePrefix + propertyName;
    }

    public String getEventNameContribution() {
        return this.eventNameContribution;
    }

    public boolean ownsProperty(final String propertyName) {
        return propertyOwnershipPredicate.test(propertyName);
    }

    public boolean isOverriddenDefault() {
        return overriddenDefault;
    }

    private static class Constants {
        public static final Predicate<String> IS_GENERAL_CONFIG = propName -> !propName.startsWith("ssl.") && !propName.startsWith("sasl.");
        public static final String PREFIX_CONFIG_VALUE = "c_";
        public static final String PREFIX_DEFAULT_VALUE = "d_";
    }
}
