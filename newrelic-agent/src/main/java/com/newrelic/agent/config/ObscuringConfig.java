package com.newrelic.agent.config;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.newrelic.agent.config.internal.DeepMapClone;

import java.util.List;
import java.util.Map;

public class ObscuringConfig extends BaseConfig {
    private static final String OBSCURING_KEY = "obscuring_key";
    private final String obscuringKey;

    public ObscuringConfig(Map<String, Object> props, String systemPropertyPrefix) {
        super(props, systemPropertyPrefix);
        obscuringKey = getProperty(OBSCURING_KEY);
    }

    public Map<String, Object> getDeobscuredProperties() {
        return DeepMapClone.deepCopy(Maps.transformValues(getProperties(), new Deobscurer()));
    }

    private class Deobscurer implements Function<Object, Object> {
        @Override
        public Object apply(Object input) {
            if (input instanceof ObscuredYamlPropertyWrapper) {
                return ((ObscuredYamlPropertyWrapper) input).getValue(obscuringKey);
            }
            if (input instanceof Map) {
                return Maps.transformValues((Map<?, ?>) input, this);
            }
            if (input instanceof List) {
                return Lists.transform((List<?>)input, this);
            }
            return input;
        }
    }
}
