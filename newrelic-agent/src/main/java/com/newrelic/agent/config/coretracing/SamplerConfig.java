/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config.coretracing;

import com.newrelic.agent.config.BaseConfig;
import com.newrelic.api.agent.NewRelic;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
    public static final String SAMPLING_TARGET = "sampling_target";
    public static final int DEFAULT_SAMPLING_TARGET = 120;
    public static final Integer USE_SHARED_ADAPTIVE_SAMPLING_TARGET = null;

    private final Object samplerProps;
    private final String sampler;
    private String samplerType;
    private Float samplerRatio;
    private Integer samplingTarget;
    private final SamplerConfig configDelegate;
    private final AtomicBoolean shouldUseConfigDelegate = new AtomicBoolean(false);

    public SamplerConfig(String sampler, Map<String, Object> props, String parentRoot) {
       this(sampler, props, parentRoot, null);
    }

    public SamplerConfig(String sampler, Map<String, Object> props, String parentRoot, SamplerConfig configDelegate) {
        super(props, parentRoot);
        this.configDelegate = configDelegate; //MAY BE NULL
        this.sampler = sampler;
        this.samplerProps = getProperty(sampler);
        this.samplerType = initSamplerType();
        this.samplerRatio = initSamplerRatio();
        this.samplingTarget = initSamplingTarget();

        NewRelic.getAgent()
                .getLogger()
                .log(Level.INFO,
                        "The " + this.sampler + " sampler was configured to use the " + this.samplerType + " sampler type" +
                                (this.samplerRatio != null ? " with a ratio of " + this.samplerRatio : "") +
                                (this.samplingTarget != null ? " with a target of " + this.samplingTarget : "") + ".");
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

    public Integer getSamplingTarget() { return this.samplingTarget; }

    /**
     * Initialize the sampler type based on the configuration properties.
     *
     * @return String The sampler type.
     */
    private String initSamplerType() {
        if (samplerType == null) {
            if (samplerProps == null) {
                //In the default case (nothing specified), fall back to the config delegate if set.
                //This is the only scenario in which the delegate should be switched into "in use".
                if (configDelegate != null) {
                    shouldUseConfigDelegate.set(true);
                    samplerType = configDelegate.getSamplerType();
                } else {
                    samplerType = DEFAULT_SAMPLER_TYPE;
                }
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

    private Float initSamplerRatio() {
        if (samplerRatio == null && TRACE_ID_RATIO_BASED.equals(getSamplerType())) {
            if (shouldUseConfigDelegate.get()) {
                samplerRatio = configDelegate.getSamplerRatio();
            } else {
                Object ratioVal = getSamplerSuboption(TRACE_ID_RATIO_BASED, RATIO);
                validateRatioOrSetToDefault(ratioVal);
            }
        }
        return samplerRatio;
    }

    private Integer initSamplingTarget() {
        if (samplingTarget == null && ADAPTIVE.equals(getSamplerType())) {
            Object targetVal = getSamplerSuboption(ADAPTIVE, SAMPLING_TARGET);
            validateSamplingTargetOrSetToDefault(targetVal);
        }
        return samplingTarget;
    }

    private Object getSamplerSuboption(String samplerName, String samplerSuboption){
        //first, check sys properties and environment vars
        Object suboptionValue = getProperty(sampler + "." + samplerName + "." + samplerSuboption);
        //next, check in local config
        if (suboptionValue == null){
            if (samplerProps != null && samplerProps instanceof Map) {
                Map<String, Object> samplerTypeProps = (Map<String, Object>) samplerProps;
                Object suboptionsObj = samplerTypeProps.get(samplerName);
                if (suboptionsObj instanceof Map) {
                    Map<String, Object> suboptionsProps = (Map<String, Object>) suboptionsObj;
                    suboptionValue = suboptionsProps.get(samplerSuboption);
                }
            }
        }
        return suboptionValue;
    }

    private void validateRatioOrSetToDefault(Object ratio){
        if (ratio != null) {
            if (ratio instanceof Number) {
                samplerRatio = ((Number) ratio).floatValue();
                // TODO validate ratio and logInvalidRatioAndSetToDefault(samplerRatio) if invalid
            } else {
                // invalid ratio specified
                logInvalidRatioAndSetToDefault(ratio);
            }
        } else {
            //ratio was missing
            logInvalidRatioAndSetToDefault("ratio not set");
        }
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

    private void validateSamplingTargetOrSetToDefault(Object target){
        if (target != null) {
            if (target instanceof Number) {
                samplingTarget = ((Number) target).intValue();
                // TODO validate sampling target and logInvalidRatioAndSetToDefault(samplerRatio) if invalid?
            } else {
                // invalid non-numeric target specified, use the default 120 instead.
                samplingTarget = DEFAULT_SAMPLING_TARGET;
                NewRelic.getAgent()
                        .getLogger()
                        .log(Level.WARNING,
                                "The {0} sampler was configured to use the adaptive sampler type with an invalid sampling target: {1}. " +
                                        "Configuring it to use the default sampling target {2}", sampler, target, DEFAULT_SAMPLING_TARGET);
            }
        } else {
            //target was missing. This is actually okay: in this case, we'll use the shared adaptive sampler.
            samplingTarget = USE_SHARED_ADAPTIVE_SAMPLING_TARGET;
            NewRelic.getAgent().getLogger().log(Level.INFO, "Sampler {0} was not configured with a sampling_target. " +
                    "Sampler will use the shared adaptive sampler instance.", sampler);
        }
    }

//    /**
//     * Initialize the sampler ratio for trace_id_ratio_based sampler type.
//     *
//     * @return Float The sampler ratio.
//     */
//    private Float initSamplerRatio() {
//        if (shouldUseConfigDelegate.get() && TRACE_ID_RATIO_BASED.equals(getSamplerType())) {
//            samplerRatio = configDelegate.getSamplerRatio();
//        } else if (samplerRatio == null && TRACE_ID_RATIO_BASED.equals(getSamplerType())) {
//            // look for ratio from system property or environment variable
//            Object ratioValue = getProperty(sampler + "." + TRACE_ID_RATIO_BASED + "." + RATIO);
//            if (ratioValue != null) {
//                if (ratioValue instanceof Number) {
//                    samplerRatio = ((Number) ratioValue).floatValue();
//                    // TODO validate ratio and logInvalidRatioAndSetToDefault(samplerRatio) if invalid
//                } else {
//                    // invalid non-numeric ratio specified
//                    logInvalidRatioAndSetToDefault(samplerRatio);
//                }
//            } else {
//                // look for ratio from yaml config
//                if (samplerProps != null && samplerProps instanceof Map) {
//                    Map<String, Object> traceIdRatioBasedProps = (Map<String, Object>) samplerProps;
//                    Object ratioObj = traceIdRatioBasedProps.get(TRACE_ID_RATIO_BASED);
//                    if (ratioObj instanceof Map) {
//                        Map<String, Object> ratioProps = (Map<String, Object>) ratioObj;
//                        ratioValue = ratioProps.get(RATIO);
//                        if (ratioValue instanceof Number) {
//                            samplerRatio = ((Number) ratioValue).floatValue();
//                            // TODO validate ratio and logInvalidRatioAndSetToDefault(samplerRatio) if invalid
//                        } else {
//                            // invalid non-numeric ratio specified
//                            logInvalidRatioAndSetToDefault(ratioValue);
//                        }
//                    } else {
//                        // no ratio value specified
//                        logInvalidRatioAndSetToDefault("ratio value not set");
//                    }
//                } else {
//                    // no ratio specified
//                    logInvalidRatioAndSetToDefault("ratio not set");
//                }
//            }
//        }
//        return samplerRatio;
//    }
//

//
//    private int initSamplingTarget(){
//        if (samplingTarget == null && ADAPTIVE.equals(getSamplerType())) {
//            // look for ratio from system property or environment variable
//            Object samplingValue = getProperty(sampler + "." + ADAPTIVE + "." + SAMPLING_TARGET);
//            if (samplingValue != null) {
//                if (samplingValue instanceof Number) {
//                    samplingTarget = ((Number) samplingValue).intValue();
//                    // TODO validate sampling target and do logging if invalid
//                } else {
//                    // invalid non-numeric target specified
//                    logInvalidSamplingTargetAndSetToDefault(samplingTarget);
//                }
//            } else {
//                // look for ratio from yaml config
//                if (samplerProps != null && samplerProps instanceof Map) {
//                    Map<String, Object> adaptiveSamplerProps = (Map<String, Object>) samplerProps;
//                    Object samplingTargetObj = adaptiveSamplerProps.get(ADAPTIVE);
//                    if (samplingTargetObj instanceof Map) {
//                        Map<String, Object> ratioProps = (Map<String, Object>) samplingTargetObj;
//                        samplingValue = ratioProps.get(SAMPLING_TARGET);
//                        if (samplingValue instanceof Number) {
//                            samplingTarget = ((Number) samplingValue).intValue();
//                            // TODO validate target and logInvalidSamplingTargetAndSetToDefault(samplingTarget) if invalid
//                        } else {
//                            // invalid non-numeric sampling target specified
//                            logInvalidSamplingTargetAndSetToDefault(samplingValue);
//                        }
//                    } else {
//                        // sampling target prop was there but no sampling target value specified
//                        logInvalidSamplingTargetAndSetToDefault("sampling target value not set");
//                    }
//                } else {
//                    // no sampling target was specified. That is fine. The shared sampler instance should be used.
//                    // here, samplingTarget will be NULL and that is EXPECTED.
//                    samplingTarget = USE_SHARED_ADAPTIVE_SAMPLING_TARGET;
//                    NewRelic.getAgent().getLogger().log(Level.FINE, "The {0} sampler was configured to use the shared adaptive sampling target", sampler);
//                }
//            }
//        }
//        return samplingTarget;
//    }

//    private void logInvalidSamplingTargetAndSetToDefault(Object samplingValue) {
//        samplingTarget = DEFAULT_SAMPLING_TARGET;
//
//        NewRelic.getAgent()
//                .getLogger()
//                .log(Level.WARNING,
//                        "The {0} sampler was configured to use the adaptive sampler type with an invalid sampling target: {1}. " +
//                                "Configuring it to use the default sampling target {2}", sampler, samplingValue, DEFAULT_SAMPLING_TARGET);
//    }
}