package com.newrelic.agent.config.coretracing;

import java.util.Map;
import com.newrelic.agent.Transaction.PartialSampleType;

public class PartialGranularityConfig extends CoreTracingConfig {

    public static final String TYPE = "type";

    //partial granularity type options
    public static final String REDUCED = "reduced";
    public static final String ESSENTIAL = "essential";
    public static final String COMPACT = "compact";

    //defaults
    public static final boolean PARTIAL_GRANULARITY_ENABLED_DEFAULT = false;
    public static final String PARTIAL_GRANULARITY_DEFAULT_TYPE=ESSENTIAL;

    private final PartialSampleType type;

    public PartialGranularityConfig(Map<String, Object> props, String samplerSystemPropertyRoot) {
        super(props, samplerSystemPropertyRoot + CoreTracingConfig.PARTIAL_GRANULARITY + ".", PARTIAL_GRANULARITY_ENABLED_DEFAULT);
        this.type = initType();
    }

    public PartialSampleType getType() {
        return type;
    }

    private PartialSampleType initType() {
        switch (getProperty(TYPE, PARTIAL_GRANULARITY_DEFAULT_TYPE)) {
            case ESSENTIAL: return PartialSampleType.ESSENTIAL;
            case REDUCED: return PartialSampleType.REDUCED;
            case COMPACT: return PartialSampleType.COMPACT;
            default: return PartialSampleType.ESSENTIAL;
        }
    }
}
