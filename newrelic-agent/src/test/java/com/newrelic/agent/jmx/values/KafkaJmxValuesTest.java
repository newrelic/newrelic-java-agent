/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.values;

import java.util.regex.Matcher;

import org.junit.Assert;
import org.junit.Test;

public class KafkaJmxValuesTest {

    @Test
    public void testRegexes() {
        Matcher m = KafkaProducerJmxValues.BYTES_SENT.matcher("JMX/\"this_is-myproduc-id-module_changes-BytesPerSec\"/");
        Assert.assertTrue(m.matches());
        Assert.assertEquals(2, m.groupCount());
        Assert.assertEquals("module_changes", m.group(2));

        m = KafkaProducerJmxValues.BYTES_SENT.matcher("JMX/\"module_changes-BytesPerSec\"/");
        Assert.assertFalse(m.matches());

    }

}