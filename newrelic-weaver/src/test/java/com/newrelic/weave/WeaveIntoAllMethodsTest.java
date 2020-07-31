/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.newrelic.api.agent.weaver.WeaveIntoAllMethods;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.violation.WeaveViolationType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class WeaveIntoAllMethodsTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        final String originalClass = "com.newrelic.weave.WeaveIntoAllMethodsTest$Example";
        final String weaveClass = "com.newrelic.weave.WeaveIntoAllMethodsTest$ExampleWeave";
        WeaveTestUtils.weaveAndAddToContextClassloader(originalClass, weaveClass);

        final String original = "com.newrelic.weave.WeaveIntoAllMethodsTest$Original";
        final String weave = "com.newrelic.weave.WeaveIntoAllMethodsTest$Original_Weave";
        WeaveTestUtils.weaveAndAddToContextClassloader(original, weave);

        final String originalOverrider = "com.newrelic.weave.WeaveIntoAllMethodsTest$Overrider";
        final String weaverOverrider = "com.newrelic.weave.WeaveIntoAllMethodsTest$Overrider_Weave";
        WeaveTestUtils.weaveAndAddToContextClassloader(originalOverrider, weaverOverrider);

        final String originalBrowny = "com.newrelic.weave.WeaveIntoAllMethodsTest$StaticMethodClass";
        final String weaveBrowny = "com.newrelic.weave.WeaveIntoAllMethodsTest$StaticMethodWeave";
        WeaveTestUtils.weaveAndAddToContextClassloader(originalBrowny, weaveBrowny);

        final String noLocal = "com.newrelic.weave.WeaveIntoAllMethodsTest$NoLocal";
        final String noLocalWeave = "com.newrelic.weave.WeaveIntoAllMethodsTest$NoLocal_Weave";
        WeaveTestUtils.weaveAndAddToContextClassloader(noLocal, noLocalWeave);

        final String stressTest = "com.newrelic.weave.WeaveIntoAllMethodsTest$StressTest";
        final String stressTestWeave = "com.newrelic.weave.WeaveIntoAllMethodsTest$StressTest_Weave";
        WeaveTestUtils.weaveAndAddToContextClassloader(stressTest, stressTestWeave);

        final String generic = "com.newrelic.weave.WeaveIntoAllMethodsTest$GenericClass";
        final String genericWeave = "com.newrelic.weave.WeaveIntoAllMethodsTest$GenericClass_Weave";
        WeaveTestUtils.weaveAndAddToContextClassloader(generic, genericWeave);

        final String foo = "com.newrelic.weave.WeaveIntoAllMethodsTest$Foo";
        final String fooWeaveOne = "com.newrelic.weave.WeaveIntoAllMethodsTest$Foo_Weave_One";
        final String fooWeaveTwo = "com.newrelic.weave.WeaveIntoAllMethodsTest$Foo_Weave_Two";

        ClassWeave classWeave = WeaveTestUtils.weave(foo, fooWeaveOne);
        WeaveTestUtils.weaveAndAddToContextClassloader(classWeave.getComposite(), WeaveTestUtils.readClass(
                fooWeaveTwo));

        final String foo2 = "com.newrelic.weave.WeaveIntoAllMethodsTest$Foo2";
        final String fooWeaveAllOne = "com.newrelic.weave.WeaveIntoAllMethodsTest$Foo_Weave_All_One";
        final String fooWeaveAllTwo = "com.newrelic.weave.WeaveIntoAllMethodsTest$Foo_Weave_All_Two";

        ClassWeave classWeaveFirst = WeaveTestUtils.weave(foo2, fooWeaveAllOne);
        WeaveTestUtils.weaveAndAddToContextClassloader(classWeaveFirst.getComposite(), WeaveTestUtils.readClass(
                fooWeaveAllTwo));

    }

    static class Example {
        private static int callCount;

        public int number;

        public Example(int k) {
            number = k;
        }

        public Example() {
        }

        public int returnPrimitiveInt() {
            return 42;
        }

        public long returnPrimitiveLong() {
            return 123l;
        }

        public short returnPrimitiveShort() {
            return 2;
        }

        public byte returnByte() {
            return 101;
        }

        public float returnFloat() {
            return 12.4f;
        }

        public double returnDouble() {
            return 3.14;
        }

        public String returnString() {
            return "Jay";
        }

        public char returnCharacter() {
            Random random = new Random();
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < 100; i++) {
                buffer.append((char) (random.nextInt(61) + 61));
            }

            char result = buffer.charAt(random.nextInt(buffer.length() - 1));
            result = (char) ('R' - result + result);
            return result;
        }

        public boolean returnBoolean(boolean b, int x) {
            String y = "hi";
            return !b;
        }

        public void returnVoid() {
            List<String> useless = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                useless.add(String.valueOf(i));
            }
        }

        public Example returnReference123() {
            return new Example(123);
        }

        public int[] array() {
            int[] countDown = { 3, 2, 1 };
            return countDown;
        }

        public Object returnObject() {
            return new Object();
        }

        public Object[] returnArrayObject() {
            Object[] objects = { null, "String", new Object() };
            return objects;
        }

        public String[] returnStringArray() {
            String[] numbers = { "one", "two", "three" };
            return numbers;
        }

        public Example[] returnEmptyArray() {
            return new Example[0];
        }
    }

    static class ExampleWeave {
        private static int callCount = Weaver.callOriginal();

        @WeaveIntoAllMethods
        private static void instrumentation() {
            Random random = new Random();
            int i = random.nextInt(10);
            i = i + 100;
            if (i >= 100) {
                callCount++;
            } else {
                // Never executed
                callCount--;
            }
            System.out.println("I am instrumented");
        }

    }

    @Test
    public void testWeaveIntoAllMethods() {
        Example example = new Example();

        assertEquals(0, example.number);
        assertEquals(42, example.returnPrimitiveInt());
        // Quick check, did weaving work?
        assertEquals(1, example.callCount);

        assertEquals(123l, example.returnPrimitiveLong());
        assertEquals(2, example.returnPrimitiveShort());
        assertEquals(101, example.returnByte());
        assertEquals(12.4f, example.returnFloat(), 0.01f);
        assertEquals(3.14f, example.returnDouble(), 0.01);
        assertEquals("Jay", example.returnString());
        assertEquals('R', example.returnCharacter());
        assertEquals(false, example.returnBoolean(true, 1));
        assertEquals(true, example.returnBoolean(false, 2));

        // sanity check
        example.returnVoid();

        Example reference = example.returnReference123();
        assertEquals(123, reference.number);

        int[] array = example.array();
        assertEquals(3, array[0]);
        assertEquals(2, array[1]);
        assertEquals(1, array[2]);

        assertNotNull(example.returnObject());

        Object[] object = example.returnArrayObject();
        assertNull(object[0]);
        assertEquals(String.class, object[1].getClass());
        assertEquals(Object.class, object[2].getClass());

        String[] numbers = example.returnStringArray();
        assertEquals("one", numbers[0]);
        assertEquals("two", numbers[1]);
        assertEquals("three", numbers[2]);

        Example[] emptyArray = example.returnEmptyArray();
        assertEquals(0, emptyArray.length);

        // This test invokes 17 Example methods. Constructors, class initializers, and accessors are not to be weaved
        assertEquals(17, example.callCount);
    }

    static class Original {
        static int count;

        public void foo() {
            count++;
        }

        public void bar() {
            count++;
        }

        public void baz() {
            count++;
        }

        public void a() {
            count++;
        }

        public void b() {
            count++;
        }

        public void c() {
            count++;
        }
    }

    static class Original_Weave {
        static int count = Weaver.callOriginal();

        @WeaveIntoAllMethods
        private static void instrumentation() {
            int length = Original_Weave.class.getDeclaredMethods().length;
            count += length;
        }

    }

    @Test
    public void testUseThisVariable() {
        Original original = new Original();
        original.foo();
        original.bar();
        original.baz();
        assertEquals((3 * 6) + 3, original.count);
    }

    static class Overrider {
        public static int callCount;

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return this;
        }

        @Override
        public String toString() {
            return "original";
        }

        @Override
        protected void finalize() throws Throwable {

        }

        public String foo() {
            return "foo";
        }
    }

    static class Overrider_Weave {
        public static int callCount;

        @WeaveIntoAllMethods
        private static void instrumentation() {
            callCount++;
        }
    }

    /**
     * Make sure {@link WeaveIntoAllMethods} doesn't weave toString, hashCode, finalize, clone, or equals.
     */
    @Test
    public void testMethodsWeNeverInstrument() throws Throwable {
        Overrider overrider = new Overrider();
        overrider.hashCode();
        overrider.equals(null);
        overrider.clone();
        overrider.toString();
        overrider.finalize();

        overrider.foo();

        assertEquals(1, overrider.callCount);
    }

    @Test
    public void testStaticMethod() {

        new StaticMethodClass().brownie();
        Assert.assertEquals(1, StaticMethodClass.count);
    }

    static class StaticMethodClass {

        static int count;

        @MyPath(name = "annotation")
        public static String brownie() {
            return "fun time";
        }
    }

    static class StaticMethodWeave {

        static int count;

        @WeaveIntoAllMethods
        private static void weaveAll() {
            StaticMethodWeave.count++;
            Weaver.getMethodAnnotation(MyPath.class);
            String s = String.valueOf(1);
        }
    }

    public @interface MyPath {
        String name();
    }

    @Test
    public void testNoLocal() {

        new NoLocal().brownie();
        Assert.assertEquals(2, NoLocal.count);
    }

    static class NoLocal {

        static int count;

        @MyPath(name = "annotation")
        public String brownie() {
            getBrownies();

            return "fun time";
        }

        private static String getBrownies() throws RuntimeException {
            try {
                String inTry = "hi";
            } catch (RuntimeException e) {
            }
            return "brownies!";
        }
    }

    static class NoLocal_Weave {

        static int count;

        @WeaveIntoAllMethods
        private static void weaveAll() {
            count++;
            Class<NoLocal_Weave> noLocal_weaveClass = NoLocal_Weave.class;
            Integer i = 123;
            Integer j = 123;
            Integer k = 123;
            System.out.println("Incrementing count");
        }
    }

    @Test
    public void testBreaker() {
        StressTest testBreaker = new StressTest();
        testBreaker.noIncrement(testBreaker);

        Assert.assertEquals(4, StressTest.count);

        long l = testBreaker.multiParam(3, 3, 2, "sdf", 4, 4, 3, true, "somethingLong");

        Assert.assertEquals(4, l);
        Assert.assertEquals(6, StressTest.count);
    }

    static class StressTest {

        static int count;

        @MyPath(name = "annotation")
        public String noIncrement(StressTest tb) {
            if (tb == null) {
                return null;
            }

            Random random = new Random();
            Signature signature = null;
            Class klass = "stressTest".getClass();
            klass = tb.getClass();
            try {
                Long oneTwoThree = 123l;
            } catch (Exception e) {
                klass = e.getClass();
                boolean b = random.nextBoolean();
                String oneTwoThree = "oneTwoThree";
                tb = new StressTest();
            } finally {
                String s = "test";
            }

            tb.noIncrement(null);
            return "erf";
        }

        @MyPath(name = "annotation")
        private long multiParam(double d, long l, int i, String s, double dd, long ll, int ii, boolean z, String ss) {

            d++;
            l++;
            i++;
            s += "Rfew";
            dd++;
            ll += 4;
            ii += 23;
            z = false;
            ss += "wer";

            return l;
        }
    }

    static class StressTest_Weave {

        static int count;

        @WeaveIntoAllMethods
        private static void weaveAll() {
            count++;
            printMe();
        }

        private static void printMe() {
            count++;
        }
    }

    @Test
    public void testNonStaticViolation() throws IOException {
        final String simpleWeave = "com/newrelic/weave/WeaveIntoAllMethodsTest$SimpleClass_Weave";
        WeaveViolation[] expected = { new WeaveViolation(WeaveViolationType.NON_STATIC_WEAVE_INTO_ALL_METHODS,
                simpleWeave) };
        WeaveTestUtils.expectViolations(SimpleClass.class, SimpleClass_Weave.class, false, new HashSet<String>(),
                Collections.<String>emptySet(), expected);
    }

    static class SimpleClass {

        static int count;

        private static void nothing() {
        }
    }

    static class SimpleClass_Weave {

        static int count;

        @WeaveIntoAllMethods
        private void weaveAll() {
            count++;
        }
    }

    static class GenericClass<T> {
        public static int count;

        public T returnType(T obj) {
            return obj;
        }

        public static int plusTwo(int number) {
            return number + 2;
        }
    }

    static class GenericClass_Weave {

        public static int count;

        @WeaveIntoAllMethods
        public static void instrumentation() {
            count++;
        }

    }

    @Test
    public void testGenericMethods() {
        assertEquals("Hi", new GenericClass<String>().returnType("Hi"));
        assertEquals("foo", new GenericClass<String>().returnType("foo"));
        assertEquals("bar", new GenericClass<String>().returnType("bar"));
        assertEquals(3, GenericClass.count);

        assertEquals(5, GenericClass.plusTwo(3));
        assertEquals(4, GenericClass.count);
    }

    static class Foo {
        public static int count;

        public void bar() {
        }

    }

    static class Foo_Weave_One {
        public static int count;

        @WeaveIntoAllMethods
        private static void instrumentation() {
            count++;
        }

    }

    static class Foo_Weave_Two {
        public static int count;

        public void bar() {
            count++;
            Weaver.callOriginal();
        }

    }

    @Test
    public void testWeaveIntoAllMethodsAndNormalWeave() {
        new Foo().bar();
        assertEquals(2, Foo.count);
    }

    static class Foo2 {
        public static int count;

        public void bar() {
        }

    }

    static class Foo_Weave_All_One {
        public static int count;

        @WeaveIntoAllMethods
        private static void instrumentation() {
            count++;
        }

    }

    static class Foo_Weave_All_Two {
        public static int count;

        @WeaveIntoAllMethods
        private static void instrumentation() {
            count++;
        }

    }

    @Test
    public void testWeaveIntoAllMethodsTwice() {
        new Foo2().bar();
        assertEquals(2, Foo2.count);
    }

}
