/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.yaml;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.extension.ConfigurationConstruct;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OrClassMatcher;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.InvalidMethodDescriptor;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NoMethodsMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.util.Strings;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.LoaderOptions;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class PointCutFactory {
    private final String defaultMetricPrefix;
    private final ClassLoader classLoader;
    private final String extensionName;

    public static class ClassMethodNameFormatDescriptor implements MetricNameFormatFactory {
        private final String prefix;

        public ClassMethodNameFormatDescriptor(final String prefix, boolean dispatcher) {
            this.prefix = getMetricPrefix(prefix, dispatcher);
        }

        @Override
        public MetricNameFormat getMetricNameFormat(ClassMethodSignature sig, Object object, Object[] args) {
            if (StringUtils.isEmpty(prefix)) {
                return new ClassMethodMetricNameFormat(sig, object);
            } else {
                return new ClassMethodMetricNameFormat(sig, object, prefix);
            }
        }

        private static String getMetricPrefix(String prefix, boolean dispatcher) {
            if (dispatcher) {
                if (prefix.startsWith(MetricNames.OTHER_TRANSACTION)) {
                    return prefix;
                } else {
                    return MetricNames.OTHER_TRANSACTION + '/' + prefix;
                }
            } else {
                return prefix;
            }
        }
    }

    public PointCutFactory(ClassLoader classLoader, String metricPrefix, String name) {
        this.classLoader = classLoader;
        defaultMetricPrefix = metricPrefix;
        extensionName = name;
    }

    @SuppressWarnings("unchecked")
    public Collection<ExtensionClassAndMethodMatcher> getPointCuts(Object config) throws ParseException {
        if (config instanceof List) {
            return getPointCuts((List) config);
        } else if (config instanceof Map) {
            return getPointCuts((Map) config);
        }
        return Collections.EMPTY_LIST;
    }

    public ExtensionClassAndMethodMatcher getPointCut(Object obj) throws ParseException {
        if (obj instanceof String) {
            return getPointCut((String) obj);
        } else if (obj instanceof Map) {
            return getPointCut((Map) obj);
        } else {
            throw new RuntimeException(MessageFormat.format("Unknown pointcut type: {0} ({1}", obj,
                    obj.getClass().getName()));
        }
    }

    public ExtensionClassAndMethodMatcher getPointCut(String string) throws ParseException {
        ClassMethodSignature sig = parseClassMethodSignature(string);
        if (sig != null) {
            return new ExtensionClassAndMethodMatcher(extensionName, null, defaultMetricPrefix, new ExactClassMatcher(
                    sig.getClassName()), createExactMethodMatcher(sig.getMethodName(), sig.getMethodDesc()), false,
                    false, false, false, null);
        }
        throw new RuntimeException("Unable to parse point cut: " + string);
    }

    private ExtensionClassAndMethodMatcher getPointCut(Map attrs) {
        return YmlExtensionPointCutConverter.createExtensionPointCut(attrs, defaultMetricPrefix, classLoader,
                extensionName);
    }

    public List<ExtensionClassAndMethodMatcher> getPointCuts(List list) throws ParseException {
        List<ExtensionClassAndMethodMatcher> pcs = new ArrayList<>();
        for (Object obj : list) {
            pcs.add(getPointCut(obj));
        }
        return pcs;
    }

    @SuppressWarnings("unchecked")
    public List<ExtensionClassAndMethodMatcher> getPointCuts(Map namesToPointCuts) throws ParseException {
        Collection<Object> values = null;
        // Throw away the names
        if (null != namesToPointCuts) {
            values = namesToPointCuts.values();
        }
        if (null == values) {
            return Collections.EMPTY_LIST;
        }

        List<ExtensionClassAndMethodMatcher> pcs = new ArrayList<>();
        for (Object obj : values) {
            if (obj instanceof String) {
                pcs.add(getPointCut((String) obj));
            } else if (obj instanceof Map) {
                pcs.add(getPointCut((Map) obj));
            }
        }
        return pcs;
    }

    static Collection<ClassMatcher> getClassMatchers(Collection matchers) {
        Collection<ClassMatcher> list = new ArrayList<>(matchers.size());
        for (Object matcher : matchers) {
            list.add(getClassMatcher(matcher));
        }
        return list;
    }

    static ClassMatcher getClassMatcher(Object yaml) {
        if (yaml instanceof ClassMatcher) {
            return (ClassMatcher) yaml;
        } else if (yaml instanceof String) {
            return new ExactClassMatcher(((String) yaml).trim());
        } else if (yaml instanceof List) {
            List list = (List) yaml;
            return OrClassMatcher.getClassMatcher(getClassMatchers(list));
        }
        return null;
    }

    static Collection<MethodMatcher> getMethodMatchers(Collection matchers) {
        Collection<MethodMatcher> list = new ArrayList<>(matchers.size());
        for (Object matcher : matchers) {
            list.add(getMethodMatcher(matcher));
        }
        return list;
    }

    static MethodMatcher getMethodMatcher(Object yaml) {
        MethodMatcher matcher = null;
        if (yaml instanceof MethodMatcher) {
            matcher = (MethodMatcher) yaml;
        } else if (yaml instanceof List) {
            List list = (List) yaml;
            if (!list.isEmpty() && list.get(0) instanceof String && list.get(0).toString().indexOf('(') < 0) {
                return createExactMethodMatcher(list.get(0).toString().trim(),
                        Strings.trim(list.subList(1, list.size())));
            }
            return OrMethodMatcher.getMethodMatcher(getMethodMatchers(list));
        } else if (yaml instanceof String) {
            String text = yaml.toString().trim();
            int index = text.indexOf('(');
            if (index > 0) {
                String methodName = text.substring(0, index);
                String methodDesc = text.substring(index);
                return createExactMethodMatcher(methodName, methodDesc);
            } else {
                return new ExactMethodMatcher(text);
            }
        }
        return matcher;
    }

    public static ClassMethodSignature parseClassMethodSignature(String signature) {
        int methodArgIndex = signature.indexOf('(');
        if (methodArgIndex > 0) {
            String methodDesc = signature.substring(methodArgIndex);
            String classAndMethod = signature.substring(0, methodArgIndex);
            int methodStart = classAndMethod.lastIndexOf('.');
            if (methodStart > 0) {
                String methodName = classAndMethod.substring(methodStart + 1);
                String className = classAndMethod.substring(0, methodStart).replace('/', '.');
                return new ClassMethodSignature(className, methodName, methodDesc);
            }
        }
        return null;
    }

    public static MethodMatcher createExactMethodMatcher(String methodName, String methodDesc) {
        ExactMethodMatcher methodMatcher = new ExactMethodMatcher(methodName, methodDesc);
        return validateMethodMatcher(methodMatcher);
    }

    public static MethodMatcher createExactMethodMatcher(String methodName, Collection<String> methodDescriptions) {
        ExactMethodMatcher methodMatcher = new ExactMethodMatcher(methodName, methodDescriptions);
        return validateMethodMatcher(methodMatcher);
    }

    private static MethodMatcher validateMethodMatcher(ExactMethodMatcher methodMatcher) {
        try {
            methodMatcher.validate();
            return methodMatcher;
        } catch (InvalidMethodDescriptor e) {
            Agent.LOG.log(
                    Level.SEVERE,
                    MessageFormat.format(
                            "The method matcher can not be created, meaning the methods associated with it will not be monitored - {0}",
                            e.toString()));
            Agent.LOG.log(Level.FINER, "Error creating method matcher.", e);
            return new NoMethodsMatcher();
        }

    }

    public static Collection<ConfigurationConstruct> getConstructs() {
        return new InstrumentationConstructor(new LoaderOptions()).constructs;
    }

}
