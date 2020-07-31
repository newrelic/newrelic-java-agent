/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

import java.util.Arrays;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import com.newrelic.agent.service.ServiceFactory;

public class UnReinstrumentMultServiceTest {

    static interface Animal {
        String getName();
    }

    static class Dog implements Animal {
        public String getName() {
            return "fido";
        }

        public int getAge() {
            return 4;
        }

        public int getWeight() {
            return 10;
        }
    }

    static class Cat implements Animal {
        public String getName() {
            return "kitten";
        }

        public int getAge() {
            return 1;
        }
    }

    static class Retriever extends Dog {
        @Override
        public int getAge() {
            return 3;
        }

        @Override
        public int getWeight() {
            return 75;
        }
    }

    static class AnotherClass {
        public int getRandom() {
            return 5;
        }
    }

    private static final String XML_INIT;
    private static final String XML_REMOVE_PART;
    private static final String XML_REMOVE_ALL;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<urn:instrumentation>");

        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:className includeSubclasses=\"true\">com.newrelic.agent.reinstrument.UnReinstrumentMultServiceTest$Dog");
        sb.append("</urn:className>");
        sb.append("<urn:method>");
        sb.append("<urn:name>getAge</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");

        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        // default for includeSubclasses is false
        sb.append("<urn:className>com.newrelic.agent.reinstrument.UnReinstrumentMultServiceTest$Dog");
        sb.append("</urn:className>");
        sb.append("<urn:method>");
        sb.append("<urn:name>getWeight</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");

        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:interfaceName>com.newrelic.agent.reinstrument.UnReinstrumentMultServiceTest$Animal");
        sb.append("</urn:interfaceName>");
        sb.append("<urn:method>");
        sb.append("<urn:name>getName</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");

        sb.append("</urn:instrumentation>");
        sb.append("</urn:extension>");
        XML_INIT = sb.toString();

        sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<urn:instrumentation>");

        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:interfaceName>com.newrelic.agent.reinstrument.UnReinstrumentMultServiceTest$Animal");
        sb.append("</urn:interfaceName>");
        sb.append("<urn:method>");
        sb.append("<urn:name>getName</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");

        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:className includeSubclasses=\"true\">com.newrelic.agent.reinstrument.UnReinstrumentMultServiceTest$AnotherClass");
        sb.append("</urn:className>");
        sb.append("<urn:method>");
        sb.append("<urn:name>getRandom</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");

        sb.append("</urn:instrumentation>");
        sb.append("</urn:extension>");
        XML_REMOVE_PART = sb.toString();

        sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<urn:instrumentation>");

        sb.append("</urn:instrumentation>");
        sb.append("</urn:extension>");
        XML_REMOVE_ALL = sb.toString();
    }

    @Test
    public void testMultipleClasses() {
        String metricDogAge = "Java/" + Dog.class.getName() + "/getAge";
        String metricDogName = "Java/" + Dog.class.getName() + "/getName";
        String metricDogWeight = "Java/" + Dog.class.getName() + "/getWeight";
        String metricCatAge = "Java/" + Cat.class.getName() + "/getAge";
        String metricCatName = "Java/" + Cat.class.getName() + "/getName";
        String metricCatWeight = "Java/" + Cat.class.getName() + "/getWeight";
        String metricRetAge = "Java/" + Retriever.class.getName() + "/getAge";
        String metricRetName = "Java/" + Retriever.class.getName() + "/getName";
        String metricRetWeight = "Java/" + Retriever.class.getName() + "/getWeight";
        String metricAnotherRandom = "Java/" + AnotherClass.class.getName() + "/getRandom";

        Dog dog = new Dog();
        dog.getAge();
        dog.getName();
        dog.getWeight();

        Cat cat = new Cat();
        cat.getAge();
        cat.getName();

        Retriever ret = new Retriever();
        ret.getWeight();
        ret.getName();
        ret.getAge();

        AnotherClass another = new AnotherClass();
        another.getRandom();

        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(metricDogAge, metricDogName, metricDogWeight,
                metricCatAge, metricCatName, metricCatWeight, metricRetAge, metricRetName, metricRetWeight,
                metricAnotherRandom));

        // #1. Send in the instrumentation
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(XML_INIT);
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(3, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));

        ret = new Retriever();
        ret.getWeight();
        ret.getName();
        ret.getAge();

        InstrumentTestUtils.veryPresentNotPresent(Arrays.asList(metricRetAge, metricRetName), Arrays.asList(
                metricRetWeight, metricDogWeight));

        cat = new Cat();
        cat.getAge();
        cat.getName();

        InstrumentTestUtils.veryPresentNotPresent(Arrays.asList(metricCatName), Arrays.asList(metricCatAge));

        dog = new Dog();
        dog.getAge();
        dog.getName();
        dog.getWeight();

        another = new AnotherClass();
        another.getRandom();
        InstrumentTestUtils.veryPresentNotPresent(Arrays.asList(metricDogName, metricDogAge, metricDogWeight),
                Arrays.asList(metricAnotherRandom));

        // #2. Uninstrument Part
        result = ServiceFactory.getRemoteInstrumentationService().processXml(XML_REMOVE_PART);
        actual = result.getStatusMap();
        Assert.assertEquals(2, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));

        ret = new Retriever();
        ret.getWeight();
        ret.getName();
        ret.getAge();

        InstrumentTestUtils.veryPresentNotPresent(Arrays.asList(metricRetName), Arrays.asList(metricRetWeight,
                metricDogWeight, metricRetAge));

        cat = new Cat();
        cat.getAge();
        cat.getName();

        InstrumentTestUtils.veryPresentNotPresent(Arrays.asList(metricCatName), Arrays.asList(metricCatAge));

        dog = new Dog();
        dog.getAge();
        dog.getName();
        dog.getWeight();

        another = new AnotherClass();
        another.getRandom();
        InstrumentTestUtils.veryPresentNotPresent(Arrays.asList(metricDogName, metricAnotherRandom), Arrays.asList(
                metricDogAge, metricDogWeight));

        // #3. Uninstrument All
        result = ServiceFactory.getRemoteInstrumentationService().processXml(XML_REMOVE_ALL);
        actual = result.getStatusMap();
        Assert.assertEquals(0, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));

        ret = new Retriever();
        ret.getWeight();
        ret.getName();
        ret.getAge();

        cat = new Cat();
        cat.getAge();
        cat.getName();

        dog = new Dog();
        dog.getAge();
        dog.getName();
        dog.getWeight();

        another = new AnotherClass();
        another.getRandom();

        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(metricDogAge, metricDogName, metricDogWeight,
                metricCatAge, metricCatName, metricCatWeight, metricRetAge, metricRetName, metricRetWeight,
                metricAnotherRandom));
    }

}
