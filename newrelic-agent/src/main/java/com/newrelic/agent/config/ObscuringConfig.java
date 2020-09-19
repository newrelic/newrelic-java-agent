package com.newrelic.agent.config;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.internal.DeepMapClone;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ObscuringConfig extends BaseConfig {

    public static final String OBSCURING_CONFIG = "config";
    static final String SYSTEM_PROPERTY_ROOT = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + OBSCURING_CONFIG + ".";
    private final ObscuringKeyConfig obscuringKeyConfig;
    private final Map<String, Object> obscureConfigProps;

    public ObscuringConfig(Map<String, Object> props, String systemPropertyRoot) {
        super(props, systemPropertyRoot);
        obscureConfigProps = getProperty(OBSCURING_CONFIG);
        obscuringKeyConfig = createObscuringKeyConfig();
    }

    public ObscuringKeyConfig createObscuringKeyConfig() {
        return new ObscuringKeyConfig(obscureConfigProps, SYSTEM_PROPERTY_ROOT);
    }


    public Map<String, Object> getDeobscuredProperties() {
        return DeepMapClone.deepCopy(Maps.transformValues(getProperties(), new Deobscurer()));
    }

    private class Deobscurer implements Function<Object, Object> {
        @Override
        public Object apply(Object input) {
            if (input instanceof ObscuredYamlPropertyWrapper) {
                final String obscuringKey = obscuringKeyConfig.getObscuringKey();
                if (obscuringKey != null) {
                    return ((ObscuredYamlPropertyWrapper) input).getValue(obscuringKey);
                }
                AgentBridge.getAgent().getLogger().log(Level.WARNING, "Unable to deobfuscate value. Missing a obscuring key");
            }
            if (input instanceof Map) {
                return Maps.transformValues((Map<?, ?>) input, this);
            }
            if (input instanceof List) {
                return Lists.transform((List<?>) input, this);
            }
            return input;
        }
    }
}
