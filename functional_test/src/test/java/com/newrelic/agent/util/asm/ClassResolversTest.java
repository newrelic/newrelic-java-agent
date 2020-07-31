/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.Type;

import com.newrelic.api.agent.NewRelic;

public class ClassResolversTest {

    @Test
    public void testEmbedded() throws IOException {
        InputStream classResource = ClassResolvers.getEmbeddedJarsClassResolver().getClassResource(
                Type.getInternalName(NewRelic.class));
        Assert.assertNotNull(classResource);
    }
}
