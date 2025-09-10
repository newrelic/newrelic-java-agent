package com.newrelic.agent.config;

import com.newrelic.agent.tracing.samplers.TraceIdRatioBasedSampler;
import com.newrelic.api.agent.NewRelic;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

public class SamplerConfig extends BaseConfig {
    //sampler options
    public static final String ALWAYS_ON = "always_on";
    public static final String ALWAYS_OFF = "always_off";
    public static final String TRACE_ID_RATIO_BASED="trace_id_ratio_based";
    public static final String DEFAULT = "default";
    public static final String ADAPTIVE_SAMPLING = "adaptive_sampling";
    public static final String RATIO = "ratio";

    //instance fields
    private String samplerType;
    //The sampling ratio for a trace_id_ratio_based sampler. May be null.
    private Float samplingRatio;

    /***
     * Factory method for making a new Sampler Config.
     *
     * We support two kinds of configuration for samplers: legacy (top-level) sampler configuration, in which the samplers are specified as literal strings, and
     * property-based sampler configuration, in which samplers are specified as subproperties.
     *
     * For example, the following configurations are equivalent:
     *
     * -Dnewrelic.config.distributed_tracing.sampler.remote_parent_sampled=always_on   (Legacy)
     * -Dnewrelic.config.distributed_tracing.sampler.remote_parent_sampled.always_on   (Property-based)
     *
     * Valid sampler types for the legacy scheme are always_on, always_off, and default. Valid sampler types for the property-based scheme are always_on,
     * always_off, default, adaptive_sampling, and trace_id_ratio_based (see below). If a sampler type is not specified, or the specified sampler type
     * is not valid, "default" will be used. If multiple property-based samplers are provided, one will be chosen following the order of precedence:
     * always_on, always_off, trace_id_ratio_based, default/adaptive_sampling.
     *
     * trace_id_ratio_based samplers MUST be configured with a ratio, which represents the proportion of traces that will be sampled. This ratio
     * MUST be a 56-bit float in the range [0, 1].
     *
     * remote_parent_sampled:
     *   trace_id_ratio_based:
     *     ratio: 0.2f
     *
     * If a trace_id_ratio_based sampler is configured, but a ratio is not provided or is invalid, the trace_id_ratio_based sampler will be rejected
     * and a "default" sampler will be used.
     *
     * @param type An object describing what type of sampler to use and any suboptions. May be a String, in which case the legacy configuration
     * scheme will be used, a Map, in which case the property-based scheme will be used, or null, in which case a default sampler will be used.
     * @param prefix The system property prefix, eg. newrelic.config.distributed_tracing.sampler.remote_parent_sampled
     * @return a SamplerConfig
     */
    public static SamplerConfig createSamplerConfig(Object type, String prefix){
        String samplerType = DEFAULT;
        Map<String, Object> subproperties = Collections.emptyMap();
        if (type instanceof String) {
            samplerType = validateLegacySamplerName((String) type);
        } else if (type instanceof Map){
            Map<String, Object> props = (Map<String, Object>) type;
            samplerType = parseSamplerTypeFromProps(props);
            subproperties = props;
        }
        return new SamplerConfig(subproperties, prefix, samplerType);
    }

    public String getSamplerType(){
        return samplerType;
    }

    public Float getSamplingRatio(){
        return samplingRatio;
    }

    private SamplerConfig(Map<String, Object> props, String systemPropertyPrefix, String samplerType) {
        super(props, systemPropertyPrefix);
        if (samplerType.equals(TRACE_ID_RATIO_BASED)){
            Float ratio = getRawSamplingRatio(systemPropertyPrefix + TRACE_ID_RATIO_BASED + ".");
            if (ratio == null || !TraceIdRatioBasedSampler.validRatio(ratio)){
                this.samplerType = DEFAULT;
                this.samplingRatio = null;
            } else {
                this.samplerType = TRACE_ID_RATIO_BASED;
                this.samplingRatio = ratio;
            }
        } else {
            this.samplerType = samplerType;
            this.samplingRatio = null;
        }
    }

    private Float getRawSamplingRatio(String prefix){
        BaseConfig traceIdRatioBasedConfig = new BaseConfig(nestedProps(TRACE_ID_RATIO_BASED), prefix);
        return traceIdRatioBasedConfig.getProperty(RATIO);
    }

    private static String validateLegacySamplerName(String samplerType){
        if (samplerType.equals(ALWAYS_ON) || samplerType.equals(ALWAYS_OFF) || samplerType.equals(DEFAULT)){
            return samplerType;
        }
        NewRelic.getAgent().getLogger().log(Level.WARNING, "Legacy sampler configuration scheme was used, but a legacy sampler type was not provided. Valid legacy samplers are always_on, always_off, and default. Setting sampler type to default.");
        return DEFAULT;
    }

    private static String parseSamplerTypeFromProps(Map<String, Object> props) {
        if (props.containsKey(ALWAYS_ON)) {
            return ALWAYS_ON;
        }
        if (props.containsKey(ALWAYS_OFF)) {
            return ALWAYS_OFF;
        }
        if (props.containsKey(TRACE_ID_RATIO_BASED)) {
            return TRACE_ID_RATIO_BASED;
        }
        return DEFAULT;
    }
}
