package com.newrelic.weave.weavepackage;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.weave.WeaveTestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InstrumentationPriorityTest {

    /**
     * Tests three modules applying to the same method, but with different priorities.
     */
    @Test
    public void testWeavePriority() throws IOException {
        WeavePackageManager wpm = new WeavePackageManager();
        WeavePackage weavePackage1 = createWeavePackage("instrumentation.order-one",
                "com.newrelic.weave.weavepackage.InstrumentationPriorityTest$OrderOne", -1L);
        wpm.register(weavePackage1);
        WeavePackage weavePackage2 = createWeavePackage("instrumentation.order-two",
                "com.newrelic.weave.weavepackage.InstrumentationPriorityTest$OrderTwo", 0L);
        wpm.register(weavePackage2);
        WeavePackage weavePackage3 = createWeavePackage("instrumentation.order-three",
                "com.newrelic.weave.weavepackage.InstrumentationPriorityTest$OrderThree", 1L);
        wpm.register(weavePackage3);

        String internalName = "com/newrelic/weave/weavepackage/InstrumentationPriorityTest$OriginalClass";
        String className = "com.newrelic.weave.weavepackage.InstrumentationPriorityTest$OriginalClass";
        byte[] compositeBytes = WeaveTestUtils.getClassBytes(className);
        // /*-
        compositeBytes = wpm.weave(Thread.currentThread().getContextClassLoader(), internalName, compositeBytes,
                Collections.emptyMap());
        for (PackageValidationResult res :
                wpm.validPackages.get(Thread.currentThread().getContextClassLoader()).values()) {
            WeaveTestUtils.expectViolations(res);
        }

        assertNotNull(compositeBytes);
        WeaveTestUtils.addToContextClassloader(className, compositeBytes);
        InstrumentationPriorityTest.OriginalClass oc = new InstrumentationPriorityTest.OriginalClass();
        assertEquals("123original321", oc.aMethodToWeave());
    }

    /**
     * Tests three modules applying to the same method, but with the same priorities,
     * should use alphabetical order.
     */
    @Test
    public void testWeaveMatchingPriority() throws IOException {
        WeavePackageManager wpm = new WeavePackageManager();
        WeavePackage weavePackage1 = createWeavePackage("instrumentation.order-one",
                "com.newrelic.weave.weavepackage.InstrumentationPriorityTest$OrderOne", 0L);
        wpm.register(weavePackage1);
        WeavePackage weavePackage2 = createWeavePackage("instrumentation.order-two",
                "com.newrelic.weave.weavepackage.InstrumentationPriorityTest$OrderTwo", 0L);
        wpm.register(weavePackage2);
        WeavePackage weavePackage3 = createWeavePackage("instrumentation.order-three",
                "com.newrelic.weave.weavepackage.InstrumentationPriorityTest$OrderThree", 0L);
        wpm.register(weavePackage3);

        String internalName = "com/newrelic/weave/weavepackage/InstrumentationPriorityTest$OriginalClass";
        String className = "com.newrelic.weave.weavepackage.InstrumentationPriorityTest$OriginalClass2";
        byte[] compositeBytes = WeaveTestUtils.getClassBytes(className);
        assertNotNull(compositeBytes);

        // /*-
        compositeBytes = wpm.weave(Thread.currentThread().getContextClassLoader(), internalName, compositeBytes,
                Collections.emptyMap());
        for (PackageValidationResult res :
                wpm.validPackages.get(Thread.currentThread().getContextClassLoader()).values()) {
            WeaveTestUtils.expectViolations(res);
        }

        assertNotNull(compositeBytes);
        WeaveTestUtils.addToContextClassloader(className, compositeBytes);
        InstrumentationPriorityTest.OriginalClass2 oc = new InstrumentationPriorityTest.OriginalClass2();
        assertEquals("132original231", oc.aMethodToWeave());
    }

    private WeavePackage createWeavePackage(String weavePackageName, String instrumentationClass, long priority) throws IOException {
        List<byte[]> weaveBytes = new ArrayList<>();
        weaveBytes.add(WeaveTestUtils.getClassBytes(instrumentationClass));
        WeavePackageConfig config = WeavePackageConfig.builder()
                .name(weavePackageName)
                .priority(priority)
                .source("com.newrelic.weave.weavepackage.testclasses")
                .build();
        return new WeavePackage(config, weaveBytes);
    }

    private interface TargetInterface {
        String aMethodToWeave();
    }

    private static class OriginalClass implements TargetInterface {
        public String aMethodToWeave() {
            return "original";
        }
    }

    // this second class is needed because the classloader was not cleaned between tests
    private static class OriginalClass2 implements TargetInterface {
        public String aMethodToWeave() {
            return "original";
        }
    }

    @Weave(type = MatchType.Interface, originalName = "com.newrelic.weave.weavepackage.InstrumentationPriorityTest$TargetInterface")
    private static class OrderOne {
        public String aMethodToWeave() {
            return "1" + Weaver.callOriginal() + "1";
        }
    }

    @Weave(type = MatchType.Interface, originalName = "com.newrelic.weave.weavepackage.InstrumentationPriorityTest$TargetInterface")
    private static class OrderTwo {

        public String aMethodToWeave() {
            return "2" + Weaver.callOriginal() + "2";
        }
    }

    @Weave(type = MatchType.Interface, originalName = "com.newrelic.weave.weavepackage.InstrumentationPriorityTest$TargetInterface")
    private static class OrderThree {
        public String aMethodToWeave() {
            return "3" + Weaver.callOriginal() + "3";
        }
    }
}
