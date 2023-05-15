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
    private final boolean useCompression;
    private final boolean useBatching;

    public InfiniteTracingConfig(Builder builder) {
        this.licenseKey = builder.licenseKey;
        this.maxQueueSize = builder.maxQueueSize;
        this.host = builder.host;
        this.port = builder.port;
        this.logger = builder.logger;
        this.flakyPercentage = builder.flakyPercentage;
        this.flakyCode = builder.flakyCode;
        this.usePlaintext = builder.usePlaintext;
        this.useCompression = builder.useCompression;
        this.useBatching = builder.useBatching;
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

    public boolean getUseCompression() {
        return useCompression;
    }

    public boolean getUseBatching() {
        return useBatching;
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
        private boolean useCompression;
        private boolean useBatching;

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
         * The optional boolean to use compression when sending to the Trace Observer.
         *
         * @param useCompression true to use compression, false otherwise
         */
        public Builder useCompression(boolean useCompression) {
            this.useCompression = useCompression;
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

        public InfiniteTracingConfig build() {
            return new InfiniteTracingConfig(this);
        }

    }
}
