/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.yaml;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import com.newrelic.agent.extension.ConfigurationConstruct;
import com.newrelic.agent.instrumentation.classmatchers.AndClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ChildClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.classmatchers.NotMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OrClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.AllMethodsMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.yaml.PointCutFactory.ClassMethodNameFormatDescriptor;
import org.yaml.snakeyaml.nodes.Tag;

public class InstrumentationConstructor extends SafeConstructor {
    public final Collection<ConfigurationConstruct> constructs;

    public InstrumentationConstructor(LoaderOptions loaderOptions) {
        super(loaderOptions);
        constructs = Arrays.asList(new ConstructClassMethodNameFormatDescriptor(), new ConstructChildClassMatcher(),
                new ConstructNotClassMatcher(), new ConstructAndClassMatcher(), new ConstructOrClassMatcher(),
                new ConstructExactClassMatcher(), new ConstructInterfaceMatcher(), new ConstructAllMethodsMatcher(),
                new ConstructOrMethodMatcher(), new ConstructExactMethodMatcher(),
                new ConstructInstanceMethodMatcher(), new ConstructStaticMethodMatcher());
        for (ConfigurationConstruct c : constructs) {
            this.yamlConstructors.put(new Tag(c.getName()), c);
        }
    }

    private class ConstructClassMethodNameFormatDescriptor extends ConfigurationConstruct {
        public ConstructClassMethodNameFormatDescriptor() {
            super("!class_method_metric_name_format");
        }

        @Override
        public Object construct(Node node) {
            String prefix = constructScalar((ScalarNode) node);
            return new ClassMethodNameFormatDescriptor(prefix, false);
        }
    }

    private class ConstructChildClassMatcher extends ConfigurationConstruct {
        public ConstructChildClassMatcher() {
            super("!child_class_matcher");
        }

        @Override
        public Object construct(Node node) {
            String val = constructScalar((ScalarNode) node);
            return new ChildClassMatcher(val);
        }
    }

    private class ConstructNotClassMatcher extends ConfigurationConstruct {
        public ConstructNotClassMatcher() {
            super("!not_class_matcher");
        }

        @Override
        public Object construct(Node node) {
            List args = constructSequence((SequenceNode) node);
            return new NotMatcher(PointCutFactory.getClassMatcher(args.get(0)));
        }
    }

    private class ConstructAndClassMatcher extends ConfigurationConstruct {
        public ConstructAndClassMatcher() {
            super("!and_class_matcher");
        }

        @Override
        public Object construct(Node node) {
            List args = constructSequence((SequenceNode) node);
            return new AndClassMatcher(PointCutFactory.getClassMatchers(args));
        }
    }

    private class ConstructOrClassMatcher extends ConfigurationConstruct {
        public ConstructOrClassMatcher() {
            super("!or_class_matcher");
        }

        @Override
        public Object construct(Node node) {
            List args = constructSequence((SequenceNode) node);
            return OrClassMatcher.getClassMatcher(PointCutFactory.getClassMatchers(args));
        }
    }

    private class ConstructExactClassMatcher extends ConfigurationConstruct {
        public ConstructExactClassMatcher() {
            super("!exact_class_matcher");
        }

        @Override
        public Object construct(Node node) {
            String val = constructScalar((ScalarNode) node);
            return new ExactClassMatcher(val);
        }
    }

    private class ConstructInterfaceMatcher extends ConfigurationConstruct {
        public ConstructInterfaceMatcher() {
            super("!interface_matcher");
        }

        @Override
        public Object construct(Node node) {
            String val = constructScalar((ScalarNode) node);
            return new InterfaceMatcher(val);
        }
    }

    private static class ConstructAllMethodsMatcher extends ConfigurationConstruct {
        public ConstructAllMethodsMatcher() {
            super("!all_methods_matcher");
        }

        @Override
        public Object construct(Node node) {
            return new AllMethodsMatcher();
        }
    }

    private class ConstructOrMethodMatcher extends ConfigurationConstruct {
        public ConstructOrMethodMatcher() {
            super("!or_method_matcher");
        }

        @Override
        public Object construct(Node node) {
            List args = constructSequence((SequenceNode) node);
            return OrMethodMatcher.getMethodMatcher(PointCutFactory.getMethodMatchers(args));
        }
    }

    private class ConstructExactMethodMatcher extends ConfigurationConstruct {
        public ConstructExactMethodMatcher() {
            super("!exact_method_matcher");
        }

        @Override
        public Object construct(Node node) {
            List args = constructSequence((SequenceNode) node);
            List methodDescriptors = args.subList(1, args.size());
            return PointCutFactory.createExactMethodMatcher((String) args.get(0), methodDescriptors);
        }
    }

    private class ConstructInstanceMethodMatcher extends ConfigurationConstruct {
        public ConstructInstanceMethodMatcher() {
            super("!instance_method_matcher");
        }

        @Override
        public Object construct(Node node) {
            List args = constructSequence((SequenceNode) node);
            return PointCutFactory.getMethodMatcher(args.get(0));
        }
    }

    private class ConstructStaticMethodMatcher extends ConfigurationConstruct {
        public ConstructStaticMethodMatcher() {
            super("!static_method_matcher");
        }

        @Override
        public Object construct(Node node) {
            List args = constructSequence((SequenceNode) node);
            return PointCutFactory.getMethodMatcher(args.get(0));
        }
    }
}
