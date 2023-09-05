/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormats;
import org.mockito.Mockito;

public class MetricNameFormatsTest {

    public MetricNameFormatsTest() {
    }

    // We are testing:
    // MetricNameFormat MetricNameFormats.getFormatter(Object invocationTarget, ClassMethodSignature sig, String
    // metricName, int flags)
    //
    // The invocationTarget can be null or an object
    // The sig cannot be null.
    // The metricName can be null
    // The flags can be 0 or other tracer flag combinations.
    //
    // So the test cases are:
    // (null, sig, null, 0)
    // (objx, sig, null, 0)
    // (null, sig, strx, 0)
    // (objx, sig, strx, 0)
    //
    // (null, sig, null, CUSTOM)
    // (objx, sig, null, CUSTOM)
    // (null, sig, strx, CUSTOM)
    // (objx, sig, strx, CUSTOM)
    //
    // (null, sig, null, DISPATCHER) <== is this case different?
    // (objx, sig, null, DISPATCHER)
    // (null, sig, strx, DISPATCHER)
    // (objx, sig, strx, DISPATCHER)
    //
    // Plus, when the metricName is non-null, it can contain the undocumented "magic" string "${classname}" which causes
    // a variable classname substitution to be performed.
    //
    // Note this test only attempts to test the metric naming variations available from that one static method. It is
    // in no sense a comprehensive test of metric naming.

    // Args to a formatter call
    static class Args {
        Object tgt;
        ClassMethodSignature cms;
        String name;
        int flags;

        Args(Object tgt, ClassMethodSignature cms, String name, int flags) {
            this.tgt = tgt;
            this.cms = cms;
            this.name = name;
            this.flags = flags;
        }

        @Override
        public String toString() {
            return "(" + tgt + ", " + cms + ", " + name + ", " + flags + ")";
        }
    }

    // Expected state of a formatter after a call
    static class Expected {
        String name;
        String segmentName;
        String segmentUri;

        Expected(String name, String segmentName, String segmentUri) {
            this.name = name;
            this.segmentName = segmentName;
            this.segmentUri = segmentUri;
        }

        @Override
        public String toString() {
            return "(" + name + ", " + segmentName + ", " + segmentUri + ")";
        }
    };

    static final ClassMethodSignature cms = new ClassMethodSignature("class", "method", "desc");
    static final Object xtgt = new Object();

    // The test cases are args, expected, args, expected, ...
    // PLEASE DO NOT REFORMAT THE ARRAY, it becomes unmaintainable then.
    // Note to Eclipse users: you have to enable the @formatter:off and :on stuff in your preferences,
    // or autosave will reformat the array. (And it looks like the same may be true for Idea users.)
    
    // @formatter:off
    static Object[] testCases = new Object[] {
        new Args(null, cms, null, 0),   new Expected("Java/class/method", "Java/class/method", ""),
        new Args(xtgt, cms, null, 0),   new Expected("Java/java.lang.Object/method", "Java/java.lang.Object/method", ""),
        new Args(null, cms, "xfoo", 0), new Expected("xfoo", "xfoo", null),
        new Args(xtgt, cms, "xfoo", 0), new Expected("xfoo", "xfoo", null),
        
        new Args(null, cms, null, TracerFlags.CUSTOM),   new Expected("Custom/class/method", "Custom/class/method", ""),
        new Args(xtgt, cms, null, TracerFlags.CUSTOM),   new Expected("Custom/java.lang.Object/method", "Custom/java.lang.Object/method", ""),
        new Args(null, cms, "xfoo", TracerFlags.CUSTOM), new Expected("xfoo", "xfoo", null),
        new Args(xtgt, cms, "xfoo", TracerFlags.CUSTOM), new Expected("xfoo", "xfoo", null),
        
        new Args(null, cms, "classname/xfoo", 0), new Expected("classname/xfoo", "classname/xfoo", null),
        new Args(xtgt, cms, "classname/xfoo", 0), new Expected("classname/xfoo", "classname/xfoo", null),
        new Args(null, cms, "classname/xfoo", TracerFlags.CUSTOM), new Expected("classname/xfoo", "classname/xfoo", null),
        new Args(xtgt, cms, "classname/xfoo", TracerFlags.CUSTOM), new Expected("classname/xfoo", "classname/xfoo", null),
        
        new Args(null, cms, "${className}/xfoo", 0), new Expected("class/xfoo", "class/xfoo", null),
        new Args(xtgt, cms, "${className}/xfoo", 0), new Expected("java.lang.Object/xfoo", "java.lang.Object/xfoo", null),
        new Args(null, cms, "${className}/xfoo", TracerFlags.CUSTOM), new Expected("class/xfoo", "class/xfoo", null),
        new Args(xtgt, cms, "${className}/xfoo", TracerFlags.CUSTOM), new Expected("java.lang.Object/xfoo", "java.lang.Object/xfoo", null),
    };
    // @formatter:on


    @Test
    public void replaceFirstSegment_withNewSegmentName_returnsNewMetricNameFormat() {
        MetricNameFormat mockFormatter = Mockito.mock(MetricNameFormat.class);
        Mockito.when(mockFormatter.getMetricName()).thenReturn("MyMetric");
        Mockito.when(mockFormatter.getTransactionSegmentName()).thenReturn("MySegment");
        MetricNameFormat result = MetricNameFormats.replaceFirstSegment(mockFormatter, "NewSegment");
        Assert.assertNotNull(result);
        Assert.assertEquals("NewSegment", result.getTransactionSegmentName());
    }
    @Test
    public void testFormatterCustom() {
        MetricNameFormat mnf;

        for (int i = 0; i < testCases.length; i += 2) {
            Args args = (Args) testCases[i];
            Expected expected = (Expected) testCases[i + 1];
            mnf = get(args);
            Assert.assertTrue("tried: " + args + "; expected: " + expected + "; got: " + mnfToString(mnf), chk(mnf,
                    expected));
        }

    }

    private MetricNameFormat get(Args args) {
        return MetricNameFormats.getFormatter(args.tgt, args.cms, args.name, args.flags);
    }

    private boolean chk(MetricNameFormat mnf, Expected expected) {
        return eq(expected.name, mnf.getMetricName()) && eq(expected.segmentName, mnf.getTransactionSegmentName())
                && eq(expected.segmentUri, mnf.getTransactionSegmentUri());
    }

    private boolean eq(String s1, String s2) {
        if (s1 == null) {
            return s2 == null;
        }
        return s1.equals(s2);
    }

    private String mnfToString(MetricNameFormat mnf) {
        return "(" + mnf.getMetricName() + ", " + mnf.getTransactionSegmentName() + ", "
                + mnf.getTransactionSegmentUri() + ")";
    }
}
