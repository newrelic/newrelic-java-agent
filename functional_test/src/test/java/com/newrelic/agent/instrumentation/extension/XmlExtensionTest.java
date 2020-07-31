/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.extension;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import com.newrelic.api.agent.Trace;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class XmlExtensionTest {

    @Test
    public void testXmlInstrumentationBasicInnerClass() {

        TheInnerClass daClass = new TheInnerClass(5);
        daClass.doubleValue();
        daClass.stringValue();

        String transName1 = InstrumentTestUtils.TRANS_PREFIX + TheInnerClass.class.getName() + "/doubleValue";
        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheInnerClass.class.getName() + "/doubleValue";

        String transName2 = InstrumentTestUtils.TRANS_PREFIX + TheInnerClass.class.getName() + "/stringValue";
        String javaName2 = InstrumentTestUtils.METHOD_PREFIX + TheInnerClass.class.getName() + "/doubleValue";

        InstrumentTestUtils.verifySingleMetrics(transName1, javaName1, transName2, javaName2);
    }

    @Test
    public void testXmlInstrumentationMetricNameFormat() {

        TheInnerClass daClass = new TheInnerClass(9);
        daClass.getValue();

        String transName1 = "OtherTransaction/ThisIsATestForGetValue";
        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheInnerClass.class.getName() + "/getValue";

        InstrumentTestUtils.verifySingleMetrics(transName1, javaName1);
    }

    @Test
    public void testXmlInstrumentationMetricPrefix() {

        TheInnerClass daClass = new TheInnerClass(7);
        daClass.performDoubleWork();
        daClass.performWork();

        String transName1 = "OtherTransaction/TadaTest/" + TheInnerClass.class.getName() + "/performDoubleWork";
        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheInnerClass.class.getName() + "/performDoubleWork";

        String transName2 = "OtherTransaction/ThisIsATestForGetValue";
        String javaName2 = InstrumentTestUtils.METHOD_PREFIX + TheInnerClass.class.getName() + "/performWork";

        InstrumentTestUtils.verifySingleMetrics(transName1, javaName1, transName2, javaName2);
    }

    @Test
    public void testMetricNamePrefix() {
        // make sure web transactions started from xml use the configured metricPrefix
        TheInnerClass classy = new TheInnerClass(7);
        classy.dispatchWebTx();
        String txName = "WebTransaction/TadaTest/" + TheInnerClass.class.getName() + "/dispatchWebTx";
        String tracerName = "TadaTest/" + TheInnerClass.class.getName() + "/anotherMethod1";
        InstrumentTestUtils.verifySingleMetrics(txName, tracerName);
    }
    

    @Test
    public void testXmlInstrumentationMetricsInside() throws InterruptedException {

        TheInnerClass daClass = new TheInnerClass(7);
        daClass.doAllWork();

        String transName1 = "OtherTransaction/TadaTest/" + TheInnerClass.class.getName() + "/doAllWork";

        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheInnerClass.class.getName() + "/doAllWork";
        String javaName2 = "OtherTransaction/HereYouGoTada";
        String javaName3 = "TadaTest/" + TheInnerClass.class.getName() + "/anotherMethod1";
        String javaName4 = "Java/" + TheInnerClass.class.getName() + "/anotherMethod2";

        InstrumentTestUtils.verifySingleMetrics(transName1, javaName1, javaName2, javaName3, javaName4);
    }

    @Test
    public void testXmlInstrumentationIgnoreTrans1() {

        TheInnerClass daClass = new TheInnerClass(5);
        daClass.doMoreWork();

        String transName1 = InstrumentTestUtils.TRANS_PREFIX + TheInnerClass.class.getName() + "/doMoreWork";
        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheInnerClass.class.getName() + "/doMoreWork";

        String javaName2 = InstrumentTestUtils.METHOD_PREFIX + TheInnerClass.class.getName() + "/shouldBeIgnored";

        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transName1, javaName1, javaName2));
    }

    @Test
    public void testXmlInstrumentationIgnoreTrans2() {

        TheInnerClass daClass = new TheInnerClass(5);
        daClass.doMoreWorkYaya();

        String transName1 = InstrumentTestUtils.TRANS_PREFIX + TheInnerClass.class.getName() + "/doMoreWorkYaya";
        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheInnerClass.class.getName() + "/doMoreWorkYaya";

        String javaName2 = InstrumentTestUtils.METHOD_PREFIX + TheInnerClass.class.getName() + "/shouldBeIgnoredYaya";

        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transName1, javaName1, javaName2));
    }

    @Test
    public void testNameTransaction() {
        final Transaction[] tx = new Transaction[1];
        Runnable r = new Runnable() {

            @Trace(dispatcher = true)
            public void run() {
                tx[0] = Transaction.getTransaction();
                new TheInnerClass(0).nameThisThing();
            }
        };
        r.run();
        Assert.assertEquals(
                "OtherTransaction/Custom/com.newrelic.agent.instrumentation.extension.XmlExtensionTest$TheInnerClass/nameThisThing",
                tx[0].getPriorityTransactionName().getName());
    }

    @Test
    public void testXmlInstrumentationMethodsWithinTrans() throws InterruptedException {

        TheInnerClass daClass = new TheInnerClass(5);
        daClass.theMain();

        String transName1 = InstrumentTestUtils.TRANS_PREFIX + TheInnerClass.class.getName() + "/theMain";
        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheInnerClass.class.getName() + "/theMain";

        String javaNameA = "Custom/" + TheInnerClass.class.getName() + "/methodA";
        String javaNameB = InstrumentTestUtils.METHOD_PREFIX + TheInnerClass.class.getName() + "/methodB";
        String javaNameC = "OtherTransaction/ccmethod";
        String javaNameF = "OtherTransaction/ffmethod";

        InstrumentTestUtils.verifySingleMetrics(transName1, javaName1, javaNameA, javaNameB, javaNameC, javaNameF);

    }

    @Test
    public void testXmlInstrumentationExcludeTransTrace() throws InterruptedException {

        String transName1 = InstrumentTestUtils.TRANS_PREFIX + TheInnerClass.class.getName() + "/theMain";
        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheInnerClass.class.getName() + "/theMain";

        String javaNameA = "Custom/" + TheInnerClass.class.getName() + "/methodA";
        String javaNameB = InstrumentTestUtils.METHOD_PREFIX + TheInnerClass.class.getName() + "/methodB";
        String javaNameC = "OtherTransaction/ccmethod";
        String javaNameD = InstrumentTestUtils.METHOD_PREFIX + TheInnerClass.class.getName() + "/methodD";
        String javaNameE = "Custom/" + TheInnerClass.class.getName() + "/methodE";

        // these values can be found in the customExt.xml
        Map<String, Boolean> expected = new HashMap<>();
        expected.put(javaNameA, Boolean.TRUE);
        expected.put(javaNameB, Boolean.TRUE);
        expected.put(javaNameC, Boolean.TRUE);
        // this one is true since dispatcher is true and that takes presidence over skip_transaction_trace
        expected.put(javaNameD, Boolean.TRUE);
        expected.put(javaNameE, Boolean.FALSE);

        CheckTTTransactionListener checker = new CheckTTTransactionListener(expected);
        checker.start();

        TheInnerClass daClass = new TheInnerClass(5);
        daClass.theMain();

        // metrics should still be present for all
        InstrumentTestUtils.verifySingleMetrics(transName1, javaName1, javaNameA, javaNameB, javaNameC, javaNameD,
                javaNameE);

        // verify transaction trace segments
        checker.stop();
        checker.verifyAllMetrics();

    }

    public class TheInnerClass {

        private int value;

        public TheInnerClass(int pValue) {
            value = pValue;
        }

        public int getValue() {
            return value;
        }

        public int doubleValue() {
            return value * 2;
        }

        public String stringValue() {
            return "the value is " + value;
        }

        public void performWork() {
            value++;
        }

        public void performDoubleWork() {
            value = value * 2;
        }

        public void doAllWork() throws InterruptedException {
            getTada1();
            anotherMethod1();
            anotherMethod2();
            Thread.sleep(10);
        }

        public String getTada1() throws InterruptedException {
            Thread.sleep(10);
            return "tada";
        }

        public void anotherMethod1() throws InterruptedException {
            Thread.sleep(10);
            return;
        }

        public void anotherMethod2() throws InterruptedException {
            Thread.sleep(10);
            return;
        }

        public void doMoreWork() {
            shouldBeIgnored();
        }

        public void shouldBeIgnored() {

        }

        public void doMoreWorkYaya() {
            shouldBeIgnoredYaya();
        }

        public void shouldBeIgnoredYaya() {

        }

        public void theMain() throws InterruptedException {
            Thread.sleep(10);
            methodA();
            methodB();
            methodC();
            methodD();
            methodE();
            methodF();
        }

        public void nameThisThing() {
        }

        public void methodA() {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }

        public void methodB() {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }

        public void methodC() {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }

        public void methodD() {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }

        public void methodE() {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }

        public void methodF() throws InterruptedException {
            Thread.sleep(10);
        }

        public void dispatchWebTx(){
            try {
                anotherMethod1();
            } catch (InterruptedException e) {
            }
        }
    }

}
