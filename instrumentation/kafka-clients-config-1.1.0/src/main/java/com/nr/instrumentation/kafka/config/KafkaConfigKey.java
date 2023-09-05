package com.nr.instrumentation.kafka.config;

import org.apache.kafka.common.config.AbstractConfig;

import java.util.Objects;

/**
 * Unique key for a particular configuration to be emitted.
 */
class KafkaConfigKey {
    private final String clientId;
    private final String configurationClass;
    private final ConfigScope scope;

    public KafkaConfigKey(final String clientId, final AbstractConfig config, final ConfigScope scope) {
        this.clientId = clientId;
        this.configurationClass = config.getClass().getName();
        this.scope = scope;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final KafkaConfigKey that = (KafkaConfigKey) o;
        return clientId.equals(that.clientId) && configurationClass.equals(that.configurationClass) && scope == that.scope;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, configurationClass, scope);
    }
}
