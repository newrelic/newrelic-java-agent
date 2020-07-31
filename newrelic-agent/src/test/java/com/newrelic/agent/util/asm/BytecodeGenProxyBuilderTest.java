/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.commons.GeneratorAdapter;

public class BytecodeGenProxyBuilderTest {

    @Test
    public void test() {
        GeneratorAdapter methodAdapter = Mockito.mock(GeneratorAdapter.class);
        List list = BytecodeGenProxyBuilder.newBuilder(List.class, methodAdapter, true).build();

        // just verify that this doesn't blow up. Primitive return values were causing NPEs at one point
        list.add("test");
        list.indexOf("test");
    }
}
