/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

public class ClassInfoTest {


    public static class RegularClass {
        private String aprivate = "private";
        public String apublic = "public";

        public static int staticMethod(long l) {
            return 0;
        }

        public static int staticMethod(String s) {
            return staticMethod(Long.valueOf(s));
        }

        public long instanceMethod(Object o) {
            return 5l;
        }
    }

    public static interface Interface1 {
        public void foo();
    }

    public static interface Interface2 extends Interface1 {
        public void bar();
    }

    public static interface Interface3 {
        public void baz();
    }

    public static interface Interface4 {
        public void biz();
    }

    public static class SuperClass1 {
        public String superField1;

        public void superMethod1() {
        }

    }

    public abstract static class SuperClass2 extends SuperClass1 implements Interface4 {
        public String superField2;

        public void superMethod2() {
        }
    }

    public static class ImplClass extends SuperClass2 implements Interface3, Interface2, Interface4 {
        public String implField;

        public void implMethod() {
        }

        @Override
        public void foo() {
        }

        @Override
        public void bar() {
        }

        @Override
        public void baz() {
        }

        @Override
        public void biz() {
        }
    }
}
