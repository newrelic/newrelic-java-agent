package com.newrelic;


import com.newrelic.api.agent.Logger;

public class InfiniteTracingConfig {

    private final String licenseKey;
    private final int maxQueueSize;
    private final String host;
    private final int port;
    private final Logger logger;
    private final Double flakyPercentage;
    private final Long flakyCode;
    private final boolean usePlaintext;
    private final String compression;
    private final boolean useBatching;
    private final int maxBatchSize;
    private final int lingerMs;

    public InfiniteTracingConfig(Builder builder) {
        this.licenseKey = builder.licenseKey;
        this.maxQueueSize = builder.maxQueueSize;
        this.host = builder.host;
        this.port = builder.port;
        this.logger = builder.logger;
        this.flakyPercentage = builder.flakyPercentage;
        this.flakyCode = builder.flakyCode;
        this.usePlaintext = builder.usePlaintext;
        this.compression = builder.compression;
        this.useBatching = builder.useBatching;
        this.maxBatchSize = builder.maxBatchSize;
        this.lingerMs = builder.lingerMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public Logger getLogger() {
        return logger;
    }

    public Double getFlakyPercentage() {
        return flakyPercentage;
    }

    public Long getFlakyCode() {
        return flakyCode;
    }

    public boolean getUsePlaintext() {
        return usePlaintext;
    }

    public String getCompression() {
        return compression;
    }

    public boolean getUseBatching() {
        return useBatching;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public int getLingerMs() {
        return lingerMs;
    }

    public static class Builder {
        public int maxQueueSize;
        public Logger logger;
        private String licenseKey;
        private String host;
        private int port;
        private Double flakyPercentage;
        private Long flakyCode;
        private boolean usePlaintext;
        private String compression;
        private boolean useBatching;
        private int maxBatchSize;
        private int lingerMs;

        /**
         * The New Relic APM license key configured for the application.
         */
        public Builder licenseKey(String licenseKey) {
            this.licenseKey = licenseKey;
            return this;
        }

        /**
         * A {@link Logger} instance the library can use for logging.
         */
        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * The hostname of the Trace Observer.
         */
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        /**
         * The TCP port which should be used to connect to the Trace Observer.
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the span buffer capacity.
         */
        public Builder maxQueueSize(int maxQueueSize) {
            this.maxQueueSize = maxQueueSize;
            return this;
        }

        /**
         * The optional percentage (specified as a double 0.0-100.0) where the Trace Observer
         * should forcibly disconnect.
         *
         * @param flakyPercentage The percentage (0.0-100.0), or null for no flakiness.
         */
        public Builder flakyPercentage(Double flakyPercentage) {
            this.flakyPercentage = flakyPercentage;
            return this;
        }

        /**
         * The optional gRPC error status to trigger when {@link #flakyPercentage(Double)} is
         * specified.
         *
         * @param flakyCode The gRPC error status code
         * @see <a href="https://github.com/grpc/grpc/blob/master/doc/statuscodes.md">gRPC status codes</a>
         */
        public Builder flakyCode(Long flakyCode) {
            this.flakyCode = flakyCode;
            return this;
        }

        /**
         * The optional boolean connect using plaintext
         *
         * @param usePlaintext true to use plaintext, false otherwise
         */
        public Builder usePlaintext(boolean usePlaintext) {
            this.usePlaintext = usePlaintext;
            return this;
        }

        /**
         * The optional compression type to use when sending to the Trace Observer.
         *
         * @param compression The compression type to use. Available options are "gzip" or "none".
         */
        public Builder compression(String compression) {
            this.compression = compression;
            return this;
        }

        /**
         * The optional boolean to use batching when sending to the Trace Observer.
         *
         * @param useBatching true to use batching, false otherwise
         */
        public Builder useBatching(boolean useBatching) {
            this.useBatching = useBatching;
            return this;
        }

        /**
         * Sets the maximum batch size when batching is enabled.
         */
        public Builder maxBatchSize(int maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
            return this;
        }

        /**
         * Sets the maximum amount of time to wait for a span batch
         * to fill before sending if below the {@link #maxBatchSize}
         */
        public Builder lingerMs(int lingerMs) {
            this.lingerMs = lingerMs;
            return this;
        }

        public InfiniteTracingConfig build() {
            return new InfiniteTracingConfig(this);
        }

    }
}
