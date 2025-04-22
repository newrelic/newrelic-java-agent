/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.yaml;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import com.newrelic.agent.instrumentation.classmatchers.AndClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ChildClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.classmatchers.NotMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OrClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class InstrumentationConstructorTest {
    private Yaml yaml;

    @Before
    public void setup() {
        SafeConstructor constructor = new InstrumentationConstructor(new LoaderOptions());
        yaml = new Yaml(constructor);
    }

    @Test
    public void interfaceMatcher() throws Exception {
        InterfaceMatcher classMatcher = load("!interface_matcher 'java/lang/Runnable'");
        Assert.assertEquals(new InterfaceMatcher("java/lang/Runnable"), classMatcher);
    }

    @Test
    public void exactClassMatcher() throws Exception {
        ExactClassMatcher classMatcher = load("!exact_class_matcher 'java/lang/Object'");
        Assert.assertEquals(new ExactClassMatcher("java/lang/Object"), classMatcher);
    }

    @Test
    public void childClassMatcher() throws Exception {
        ChildClassMatcher classMatcher = load("!child_class_matcher 'java/lang/Object'");
        Assert.assertEquals(new ChildClassMatcher("java/lang/Object"), classMatcher);
    }

    @Test
    public void notClassMatcher() throws Exception {
        NotMatcher classMatcher = load("!not_class_matcher [ !exact_class_matcher 'java/lang/Object' ]");
        Assert.assertEquals(new NotMatcher(new ExactClassMatcher("java/lang/Object")), classMatcher);
    }

    @Test
    public void andOrClassMatchers() throws Exception {
        String classes = "[ !exact_class_matcher 'java/lang/Dude', !exact_class_matcher 'java/lang/Object' ]";
        ClassMatcher[] matchers = new ClassMatcher[] { new ExactClassMatcher("java/lang/Dude"),
                new ExactClassMatcher("java/lang/Object") };
        AndClassMatcher classMatcher = load("!and_class_matcher " + classes);
        Assert.assertEquals(new AndClassMatcher(matchers), classMatcher);

        ClassMatcher orClassMatcher = load("!or_class_matcher " + classes);
        Assert.assertEquals(OrClassMatcher.getClassMatcher(matchers), orClassMatcher);
    }

    @Test
    public void orMethodMatchers() throws Exception {
        String methods = "[ !exact_method_matcher [ 'getTracerFactoryClassName', '()Ljava/lang/String;' ], !exact_method_matcher [ 'getDude', '()V' ] ]";
        MethodMatcher[] matchers = new MethodMatcher[] {
                new ExactMethodMatcher("getTracerFactoryClassName", "()Ljava/lang/String;"),
                new ExactMethodMatcher("getDude", "()V") };
        MethodMatcher methodMatcher = load("!or_method_matcher " + methods);
        Assert.assertEquals(OrMethodMatcher.getMethodMatcher(matchers), methodMatcher);
    }

    @Test
    public void exactMethodMatcher() throws Exception {
        ExactMethodMatcher methodMatcher = load("!exact_method_matcher [ 'getTracerFactoryClassName', '()Ljava/lang/String;' ]");
        Assert.assertEquals(new ExactMethodMatcher("getTracerFactoryClassName", "()Ljava/lang/String;"), methodMatcher);
    }

    @Test
    public void exactMethodMatcherTwoSignatures() throws Exception {
        ExactMethodMatcher methodMatcher = load("!exact_method_matcher [ 'getTracerFactoryClassName', '()Ljava/lang/String;', '()V' ]");
        Assert.assertEquals(new ExactMethodMatcher("getTracerFactoryClassName", "()Ljava/lang/String;", "()V"),
                methodMatcher);
    }

    private <T> T load(String yaml) {
        return this.yaml.load(yaml);
    }
}
