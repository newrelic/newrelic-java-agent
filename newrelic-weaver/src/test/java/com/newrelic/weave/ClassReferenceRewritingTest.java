/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClassReferenceRewritingTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        final String originalClass = "com.newrelic.weave.ClassReferenceRewritingTest$Original";
        final String weaveClass = "com.newrelic.weave.ClassReferenceRewritingTest$Original_Weaved";
        WeaveTestUtils.weaveAndAddToContextClassloader(originalClass, weaveClass);
    }

    static class Original {

        public int declaredMethods() {
            return 0;
        }

        public void foo() {}

        public void bar() {}

        public void baz() {}

        public void xyz() {}

    }

    static class Original_Weaved {

        public int declaredMethods() {
            // Reference to Original_Weaved should get rewritten to Original
            return Original_Weaved.class.getDeclaredMethods().length;
        }
    }

    @Test
    public void testRenameWeaveClassReference() {
        Original original = new Original();
        // 5 = declaredMethod, foo, bar, baz, xyz.
        assertEquals(5, original.declaredMethods());
    }
}
