package com.newrelic.agent.tracers.metricname;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OtherTransSimpleMetricNameFormatTest {
    @Test
    public void constructor_withMetricName_constructsCorrectMetricName() {
        OtherTransSimpleMetricNameFormat formatter = new OtherTransSimpleMetricNameFormat("mymetric");
        assertEquals("OtherTransaction/mymetric", formatter.getMetricName());
        assertEquals("OtherTransaction/mymetric", formatter.getTransactionSegmentName());

        formatter = new OtherTransSimpleMetricNameFormat("/mymetric");
        assertEquals("OtherTransaction/mymetric", formatter.getMetricName());
        assertEquals("OtherTransaction/mymetric", formatter.getTransactionSegmentName());

        formatter = new OtherTransSimpleMetricNameFormat("OtherTransaction/mymetric");
        assertEquals("OtherTransaction/mymetric", formatter.getMetricName());
        assertEquals("OtherTransaction/mymetric", formatter.getTransactionSegmentName());
    }

    @Test
    public void constructor_withNullMetricName_setsMetricNameToNull() {
        OtherTransSimpleMetricNameFormat formatter = new OtherTransSimpleMetricNameFormat(null);
        assertNull(formatter.getMetricName());
    }

    @Test
    public void constructor_withMetricNameAndSegmentName_constructsCorrectSegmentName() {
        OtherTransSimpleMetricNameFormat formatter = new OtherTransSimpleMetricNameFormat("mymetric", "mysegment");
        assertEquals("OtherTransaction/mymetric", formatter.getMetricName());
        assertEquals("mysegment", formatter.getTransactionSegmentName());

        formatter = new OtherTransSimpleMetricNameFormat("/mymetric", "mysegment");
        assertEquals("OtherTransaction/mymetric", formatter.getMetricName());
        assertEquals("mysegment", formatter.getTransactionSegmentName());

        formatter = new OtherTransSimpleMetricNameFormat("OtherTransaction/mymetric", "mysegment");
        assertEquals("OtherTransaction/mymetric", formatter.getMetricName());
        assertEquals("mysegment", formatter.getTransactionSegmentName());
    }

    @Test
    public void getTransactionSegmentUri_returnsNull() {
        OtherTransSimpleMetricNameFormat formatter = new OtherTransSimpleMetricNameFormat("mymetric");
        assertNull(formatter.getTransactionSegmentUri());
    }
}
