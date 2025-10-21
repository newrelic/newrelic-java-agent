/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

//import com.newrelic.agent.tracing.samplers.TraceIdRatioBasedSampler;

import com.newrelic.api.agent.NewRelic;

import java.util.Map;
import java.util.logging.Level;

/**
 * Configuration for samplers used in distributed tracing.
 * <p>
 * The following config options are supported via YAML, system properties, or environment variables, where ROOT/root
 * can any of the defined samplers: 'root', 'remote_parent_sampled', or 'remote_parent_not_sampled'.
 * <p>
 * Environment Variables:
 * <pre>
 * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT: 'default'
 * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT: 'adaptive'
 * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT: 'always_on'
 * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT: 'always_off'
 * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT: 'trace_id_ratio_based'
 * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT_TRACE_ID_RATIO_BASED_RATIO: '0.1'
 * </pre>
 * <p>
 * System Properties:
 * <pre>
 * -Dnewrelic.config.distributed_tracing.sampler.root=default
 * -Dnewrelic.config.distributed_tracing.sampler.root=adaptive
 * -Dnewrelic.config.distributed_tracing.sampler.root=always_on
 * -Dnewrelic.config.distributed_tracing.sampler.root=always_off
 * -Dnewrelic.config.distributed_tracing.sampler.root=trace_id_ratio_based
 * -Dnewrelic.config.distributed_tracing.sampler.root.trace_id_ratio_based.ratio=0.1
 * </pre>
 * <p>
 * YAML:
 * <p>
 * Yaml supports both of the following formats for specifying the sampler and sampler type:
 * <p>
 * <pre>
 *   distributed_tracing:
 *     sampler:
 *       root:
 *         default:
 * </pre>
 * OR
 * <pre>
 *   distributed_tracing:
 *     sampler:
 *       root: default
 * </pre>
 */
public class SamplerConfig extends BaseConfig {
    public static final String SAMPLER_CONFIG_ROOT = "sampler";

    // sampling target
    public static final String ADAPTIVE_SAMPLING_TARGET = "adaptive_sampling_target";
    public static final Integer DEFAULT_ADAPTIVE_SAMPLING_TARGET = 120;
    public static final Integer DEFAULT_ADAPTIVE_SAMPLING_PERIOD = 60;

    // samplers
    public static final String ROOT = "root";
    public static final String REMOTE_PARENT_SAMPLED = "remote_parent_sampled";
    public static final String REMOTE_PARENT_NOT_SAMPLED = "remote_parent_not_sampled";

    // sampler types
    public static final String DEFAULT = "default";
    public static final String ADAPTIVE = "adaptive";
    public static final String ALWAYS_ON = "always_on";
    public static final String ALWAYS_OFF = "always_off";
    public static final String TRACE_ID_RATIO_BASED = "trace_id_ratio_based";
    public static final String DEFAULT_SAMPLER_TYPE = ADAPTIVE;

    // sampler types sub-options
    public static final String RATIO = "ratio";

    private final Object samplerProps;
    private final String sampler;
    private String samplerType;
    private Float samplerRatio;

    public SamplerConfig(String sampler, Map<String, Object> props, String parentRoot) {
        super(props, parentRoot + SAMPLER_CONFIG_ROOT + ".");
        this.sampler = sampler;
        this.samplerProps = getProperty(sampler);
        this.samplerType = initSamplerType();
        this.samplerRatio = initSamplerRatio();

        NewRelic.getAgent()
                .getLogger()
                .log(Level.INFO,
                        "The " + this.sampler + " sampler was configured to use the " + this.samplerType + " sampler type" +
                                (this.samplerRatio != null ? " with a ratio of " + this.samplerRatio : "") + ".");
    }

    public int getAdaptiveSamplingTarget() {
        return getProperty(ADAPTIVE_SAMPLING_TARGET, DEFAULT_ADAPTIVE_SAMPLING_TARGET);
    }

    public String getSampler() {
        return this.sampler;
    }

    public String getSamplerType() {
        return this.samplerType;
    }

    public Float getSamplerRatio() {
        return this.samplerRatio;
    }

    /**
     * Initialize the sampler type based on the configuration properties.
     *
     * @return String The sampler type.
     */
    private String initSamplerType() {
        if (samplerType == null) {
            if (samplerProps == null) {
                samplerType = DEFAULT_SAMPLER_TYPE;
            } else if (samplerProps instanceof String) {
                if (samplerProps.equals(ALWAYS_ON)) {
                    samplerType = ALWAYS_ON;
                } else if (samplerProps.equals(ALWAYS_OFF)) {
                    samplerType = ALWAYS_OFF;
                } else if (samplerProps.equals(ADAPTIVE)) {
                    samplerType = ADAPTIVE;
                } else if (samplerProps.equals(TRACE_ID_RATIO_BASED)) {
                    samplerType = TRACE_ID_RATIO_BASED;
                } else if (samplerProps.equals(DEFAULT)) {
                    samplerType = DEFAULT_SAMPLER_TYPE;
                } else {
                    // invalid sampler type specified
                    logInvalidSamplerTypeAndSetToDefault();
                }
            } else if (samplerProps instanceof Map) {
                Map<String, Object> props = (Map<String, Object>) samplerProps;
                if (props.size() == 1) {
                    if (props.containsKey(ALWAYS_ON)) {
                        samplerType = ALWAYS_ON;
                    } else if (props.containsKey(ALWAYS_OFF)) {
                        samplerType = ALWAYS_OFF;
                    } else if (props.containsKey(ADAPTIVE)) {
                        samplerType = ADAPTIVE;
                    } else if (props.containsKey(TRACE_ID_RATIO_BASED)) {
                        samplerType = TRACE_ID_RATIO_BASED;
                    } else if (props.containsKey(DEFAULT)) {
                        samplerType = DEFAULT_SAMPLER_TYPE;
                    } else {
                        // invalid sampler type specified
                        logInvalidSamplerTypeAndSetToDefault();
                    }
                } else {
                    // multiple sampler types specified
                    logInvalidSamplerTypeAndSetToDefault();
                }
            } else {
                // no sampler type specified
                logInvalidSamplerTypeAndSetToDefault();
            }
        }
        return samplerType;
    }

    private void logInvalidSamplerTypeAndSetToDefault() {
        samplerType = DEFAULT_SAMPLER_TYPE;

        NewRelic.getAgent()
                .getLogger()
                .log(Level.WARNING,
                        "The " + sampler + " sampler was configured with an invalid sampler type. Configuring it to use the default " + DEFAULT_SAMPLER_TYPE +
                                " sampler type.");
    }

    /**
     * Initialize the sampler ratio for trace_id_ratio_based sampler type.
     *
     * @return Float The sampler ratio.
     */
    private Float initSamplerRatio() {
        if (samplerRatio == null && TRACE_ID_RATIO_BASED.equals(getSamplerType())) {
            // look for ratio from system property or environment variable
            Object ratioValue = getProperty(sampler + "." + TRACE_ID_RATIO_BASED + "." + RATIO);
            if (ratioValue != null) {
                if (ratioValue instanceof Number) {
                    samplerRatio = ((Number) ratioValue).floatValue();
                    // FIXME TraceIdRatioBasedSampler.validRatio needs to be implemented for this logic to work
//                    if (!TraceIdRatioBasedSampler.validRatio(samplerRatio)) {
//                        // invalid numeric ratio specified
//                        logInvalidRatioAndSetToDefault(samplerRatio);
//                    }
                } else {
                    // invalid non-numeric ratio specified
                    logInvalidRatioAndSetToDefault(samplerRatio);
                }
            } else {
                // look for ratio from yaml config
                if (samplerProps != null && samplerProps instanceof Map) {
                    Map<String, Object> traceIdRatioBasedProps = (Map<String, Object>) samplerProps;
                    Object ratioObj = traceIdRatioBasedProps.get(TRACE_ID_RATIO_BASED);
                    if (ratioObj instanceof Map) {
                        Map<String, Object> ratioProps = (Map<String, Object>) ratioObj;
                        ratioValue = ratioProps.get(RATIO);
                        if (ratioValue instanceof Number) {
                            samplerRatio = ((Number) ratioValue).floatValue();
                            // FIXME TraceIdRatioBasedSampler.validRatio needs to be implemented for this logic to work
//                            if (!TraceIdRatioBasedSampler.validRatio(samplerRatio)) {
//                                // invalid numeric ratio specified
//                                logInvalidRatioAndSetToDefault(ratioValue);
//                            }
                        } else {
                            // invalid non-numeric ratio specified
                            logInvalidRatioAndSetToDefault(ratioValue);
                        }
                    } else {
                        // no ratio value specified
                        logInvalidRatioAndSetToDefault("ratio value not set");
                    }
                } else {
                    // no ratio specified
                    logInvalidRatioAndSetToDefault("ratio not set");
                }
            }
        }
        return samplerRatio;
    }

    private void logInvalidRatioAndSetToDefault(Object ratioValue) {
        // There is no default value for ratio; if trace_id_ratio_based is configured and ratio is
        // not valid, the agent should log an error and fall back to using default sampler type.
        samplerRatio = null;
        samplerType = DEFAULT_SAMPLER_TYPE;

        NewRelic.getAgent()
                .getLogger()
                .log(Level.WARNING,
                        "The " + sampler + " sampler was configured to use the trace_id_ratio_based sampler type with an invalid ratio: " + ratioValue +
                                ". Configuring it to use the default " + DEFAULT_SAMPLER_TYPE + " sampler type.");
    }
}