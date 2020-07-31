/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassLoaderFinder;
import com.newrelic.weave.weavepackage.PackageValidationResult;
import com.newrelic.weave.weavepackage.WeavePackage;
import com.newrelic.weave.weavepackage.WeavePackageConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class EnumAccessTest {
    public static WeavePackage makeWeavePackage() throws IOException {
        List<byte[]> weaveBytes = new ArrayList<>();
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.EnumAccessTest$State_Instrumentation"));
        weaveBytes.add(WeaveTestUtils.getClassBytes("com.newrelic.weave.EnumAccessTest$Foo_Instrumentation"));
        WeavePackageConfig config = WeavePackageConfig.builder().name("weave_unittest").source(
                "com.newrelic.weave.weavepackage.testclasses").build();
        return new WeavePackage(config, weaveBytes);
    }

    @Before
    public void setup() throws IOException {
        WeavePackage weavePackage = makeWeavePackage();
        ClassCache cache = new ClassCache(new ClassLoaderFinder(EnumAccessTest.class.getClassLoader()));
        PackageValidationResult result = weavePackage.validate(cache);
        WeaveTestUtils.expectViolations(result);
        WeaveTestUtils.loadUtilityClasses(EnumAccessTest.class.getClassLoader(), result.computeUtilityClassBytes(cache));

        byte[] stateCompositeBytes = result.weave("com/newrelic/weave/EnumAccessTest$Foo$State", new String[0],
                new String[0], WeaveTestUtils.getClassBytes("com.newrelic.weave.EnumAccessTest$Foo$State"), cache).getCompositeBytes(
                cache);
        Assert.assertNotNull(stateCompositeBytes);
        byte[] aCompositeBytes = result.weave("com/newrelic/weave/EnumAccessTest$Foo", new String[0], new String[0],
                WeaveTestUtils.getClassBytes("com.newrelic.weave.EnumAccessTest$Foo"), cache).getCompositeBytes(cache);
        Assert.assertNotNull(aCompositeBytes);

        WeaveTestUtils.addToContextClassloader("com.newrelic.weave.EnumAccessTest$Foo$State", stateCompositeBytes);
        WeaveTestUtils.addToContextClassloader("com.newrelic.weave.EnumAccessTest$Foo", aCompositeBytes);
    }

    @Test
    public void testAccessPublicEnum() {
        Foo foo = new Foo();
        assertTrue(foo.enumWeaved());
        assertEquals(foo.getWaitingState(), foo.currentState());
        foo.switchStates();
        assertEquals(foo.getRuningState(), foo.currentState());
    }

    static class Foo {
        private State state = State.WAITING;

        private enum State {
            RUNNING, WAITING;

            public boolean weaved() {
                return false;
            }
        }

        public boolean enumWeaved() {
            return state.weaved();
        }

        public State currentState() {
            return state;
        }

        public State getRuningState() {
            return State.RUNNING;
        }

        public State getWaitingState() {
            return State.WAITING;
        }

        public void switchStates() {
        }
    }

    @Weave(originalName = "com.newrelic.weave.EnumAccessTest$Foo")
    static class Foo_Instrumentation {
        private State_Instrumentation state = Weaver.callOriginal();

        public void switchStates() {
            state = state.equals(State_Instrumentation.WAITING) ? State_Instrumentation.RUNNING
                    : State_Instrumentation.WAITING;
            Weaver.callOriginal();
        }
    }

    @Weave(originalName = "com.newrelic.weave.EnumAccessTest$Foo$State")
    enum State_Instrumentation {
        RUNNING, WAITING;

        public boolean weaved() {
            return true;
        }
    };
}
