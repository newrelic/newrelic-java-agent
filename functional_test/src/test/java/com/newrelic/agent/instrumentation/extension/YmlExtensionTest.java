/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.extension;

import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class YmlExtensionTest {

    @Test
    public void testYmlInstrumentationBasicInnerClass() {

        TheYamlInnerClass daClass = new TheYamlInnerClass(5);
        daClass.doubleValue();
        daClass.stringValue();

        String transName1 = InstrumentTestUtils.TRANS_PREFIX + TheYamlInnerClass.class.getName() + "/doubleValue";
        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/doubleValue";

        String transName2 = InstrumentTestUtils.TRANS_PREFIX + TheYamlInnerClass.class.getName() + "/stringValue";
        String javaName2 = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/doubleValue";

        InstrumentTestUtils.verifySingleMetrics(transName1, javaName1, transName2, javaName2);
    }

    @Test
    public void testYmlInstrumentationMetricNameFormat() {

        TheYamlInnerClass daClass = new TheYamlInnerClass(9);
        daClass.getValue();

        String transName1 = "OtherTransaction/ThisIsATestForGetValue";
        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/getValue";

        InstrumentTestUtils.verifySingleMetrics(transName1, javaName1);
    }

    @Test
    public void testYmlInstrumentationMetricPrefix() {

        TheYamlInnerClass daClass = new TheYamlInnerClass(7);
        daClass.performDoubleWork();
        daClass.performWork();

        String transName1 = "OtherTransaction/TadaTest/" + TheYamlInnerClass.class.getName() + "/performDoubleWork";
        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/performDoubleWork";

        String transName2 = "OtherTransaction/ThisIsATestForGetValue";
        String javaName2 = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/performWork";

        InstrumentTestUtils.verifySingleMetrics(transName1, javaName1, transName2, javaName2);
    }

    @Test
    public void testYmlInstrumentationMetricsInside() {

        TheYamlInnerClass daClass = new TheYamlInnerClass(7);
        daClass.doAllWork();

        String transName1 = "OtherTransaction/TadaTest/" + TheYamlInnerClass.class.getName() + "/doAllWork";

        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/doAllWork";
        String javaName2 = "OtherTransaction/HereYouGoTada";
        String javaName3 = "TadaTest/" + TheYamlInnerClass.class.getName() + "/anotherMethod1";
        String javaName4 = "Java/" + TheYamlInnerClass.class.getName() + "/anotherMethod2";

        InstrumentTestUtils.verifySingleMetrics(transName1, javaName1, javaName2, javaName3, javaName4);
    }

    @Test
    public void testDisabled() {

        TheYamlInnerClass daClass = new TheYamlInnerClass(6);
        daClass.run();
        daClass.goToSleep();

        String transName1 = InstrumentTestUtils.TRANS_PREFIX + TheYamlInnerClass.class.getName() + "/run";
        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/run";

        String transName2 = "OtherTransaction/ThisIsATestForSleeping";
        String javaName2 = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/goToSleep";

        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transName1, javaName1, transName2, javaName2));
    }

    @Test
    public void testYmlInstrumentationIgnoreMethod1() {

        TheYamlInnerClass daClass = new TheYamlInnerClass(5);
        daClass.doMoreWork();

        String transName1 = InstrumentTestUtils.TRANS_PREFIX + TheYamlInnerClass.class.getName() + "/doMoreWork";
        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/doMoreWork";

        String javaName2 = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/shouldBeIgnored";

        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transName1, javaName1, javaName2));
    }

    @Test
    public void testYmlInstrumentationIgnoreMethod2() {

        TheYamlInnerClass daClass = new TheYamlInnerClass(5);
        daClass.doMoreWorkYaya();

        String transName1 = InstrumentTestUtils.TRANS_PREFIX + TheYamlInnerClass.class.getName() + "/doMoreWorkYaya";
        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/doMoreWorkYaya";

        String javaName2 = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName()
                + "/shouldBeIgnoredYaya";

        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transName1, javaName1, javaName2));
    }

    @Test
    public void testYmlInstrumentationMethodsWithinTrans() {

        TheYamlInnerClass daClass = new TheYamlInnerClass(5);
        daClass.theMain();

        String transName1 = InstrumentTestUtils.TRANS_PREFIX + TheYamlInnerClass.class.getName() + "/theMain";
        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/theMain";

        String javaNameA = "Custom/" + TheYamlInnerClass.class.getName() + "/methodA";
        String javaNameB = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/methodB";
        String javaNameC = "OtherTransaction/ccmethod";
        String javaNameF = "OtherTransaction/ffmethod";

        InstrumentTestUtils.verifySingleMetrics(transName1, javaName1, javaNameA, javaNameB, javaNameC, javaNameF);

    }

    @Test
    public void testYmlInstrumentationExcludeTransTrace() {

        String transName1 = InstrumentTestUtils.TRANS_PREFIX + TheYamlInnerClass.class.getName() + "/theMain";
        String javaName1 = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/theMain";

        String javaNameA = "Custom/" + TheYamlInnerClass.class.getName() + "/methodA";
        String javaNameB = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/methodB";
        String javaNameC = "OtherTransaction/ccmethod";
        String javaNameD = InstrumentTestUtils.METHOD_PREFIX + TheYamlInnerClass.class.getName() + "/methodD";
        String javaNameE = "Custom/" + TheYamlInnerClass.class.getName() + "/methodE";

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

        TheYamlInnerClass daClass = new TheYamlInnerClass(5);
        daClass.theMain();

        // metrics should still be present for all
        InstrumentTestUtils.verifySingleMetrics(transName1, javaName1, javaNameA, javaNameB, javaNameC, javaNameD,
                javaNameE);

        // verify transaction trace segments
        checker.stop();
        checker.verifyAllMetrics();

    }

    public class TheYamlInnerClass {

        private int value;

        public TheYamlInnerClass(int pValue) {
            value = pValue;
        }

        public int getValue() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            return value;
        }

        public int doubleValue() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            return value * 2;
        }

        public String stringValue() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            return "the value is " + value;
        }

        public void performWork() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            value++;
        }

        public void performDoubleWork() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            value = value * 2;
        }

        public void doAllWork() {
            getTada1();
            anotherMethod1();
            anotherMethod2();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        public String getTada1() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            return "tada";
        }

        public void anotherMethod1() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            return;
        }

        public void anotherMethod2() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            return;
        }

        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        public void goToSleep() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        public void doMoreWork() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            shouldBeIgnored();
        }

        public void shouldBeIgnored() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        public void doMoreWorkYaya() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            shouldBeIgnoredYaya();
        }

        public void shouldBeIgnoredYaya() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        public void theMain() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            methodA();
            methodB();
            methodC();
            methodD();
            methodE();
            methodF();
        }

        public void methodA() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        public void methodB() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        public void methodC() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        public void methodD() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        public void methodE() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        public void methodF() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }

}
