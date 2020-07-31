/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;

class EmptyConstructor {
    public EmptyConstructor() {
    }
}

class NonEmptyConstructor {
    long x;

    public NonEmptyConstructor() {
        x = System.currentTimeMillis();
    }
}

class NonEmptyConstructorTwo {
    long x;

    public NonEmptyConstructorTwo() {
        x = 123;
    }
}

class NonEmptyConstructorParams {
   NonEmptyConstructorParams(String[] arr) {
       // Parameters make this a non-empty constructor
   }
}

class GeneratedEmptyConstructor {
}

class EmptyConstructorStaticInit {
    static int x;

    static {  x = -123;}

}

class Outer {
    class Inner {
        // generated init populates synthetic this$0 field. Making this a non empty constructor
    }
}

