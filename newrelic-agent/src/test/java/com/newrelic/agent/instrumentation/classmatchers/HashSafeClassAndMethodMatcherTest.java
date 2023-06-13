package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.agent.instrumentation.methodmatchers.AccessMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactParamsMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactReturnTypeMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NameMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ReturnTypeMethodMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Arrays;

public class HashSafeClassAndMethodMatcherTest {

    private static String BASE_CLASS_NAME = "class";
    private static String DIFF_CLASS_NAME = "differentClass";
    private static String BASE_METHOD_NAME = "method";
    private static String BASE_DESC = "desc";
    private static String DIFF_METHOD_NAME = "differentMethod";
    private static String DIFF_DESC = "differentDesc";

    @Test
    public void testEqualsAndHasCode() {
        testVariantsOnMatchers(
                new ChildClassMatcher(BASE_METHOD_NAME+"/"+BASE_METHOD_NAME),
                new ChildClassMatcher(DIFF_METHOD_NAME+"/"+DIFF_METHOD_NAME),
                ExactParamsMethodMatcher.createExactParamsMethodMatcher(BASE_METHOD_NAME, BASE_DESC),
                ExactParamsMethodMatcher.createExactParamsMethodMatcher(DIFF_METHOD_NAME, DIFF_DESC)
        );

        testVariantsOnMatchers(
                new ExactClassMatcher(BASE_CLASS_NAME),
                new ExactClassMatcher(DIFF_CLASS_NAME),
                new AccessMethodMatcher(Opcodes.ACC_PUBLIC),
                new AccessMethodMatcher(Opcodes.ACC_PRIVATE)
        );

        testVariantsOnMatchers(
                new InterfaceMatcher(BASE_CLASS_NAME),
                new InterfaceMatcher(DIFF_CLASS_NAME),
                new ExactReturnTypeMethodMatcher(Type.BOOLEAN_TYPE),
                new ExactReturnTypeMethodMatcher(Type.INT_TYPE)
        );

        testVariantsOnMatchers(
                new InterfaceMatcher(BASE_CLASS_NAME),
                new InterfaceMatcher(DIFF_CLASS_NAME),
                new NameMethodMatcher(BASE_METHOD_NAME),
                new NameMethodMatcher(DIFF_METHOD_NAME)
        );

        testVariantsOnMatchers(
                new InterfaceMatcher(BASE_CLASS_NAME),
                new InterfaceMatcher(DIFF_CLASS_NAME),
                new ReturnTypeMethodMatcher(Arrays.asList("boolean")),
                new NameMethodMatcher(DIFF_METHOD_NAME) // the equals method on ReturnTypeMethodMatcher only really checks class
        );
    }

    private void testVariantsOnMatchers(ClassMatcher classMatcher1, ClassMatcher classMatcher2,
            MethodMatcher methodMatcher1, MethodMatcher methodMatcher2) {
        HashSafeClassAndMethodMatcher fullMatcher11 = new HashSafeClassAndMethodMatcher(classMatcher1, methodMatcher1);
        HashSafeClassAndMethodMatcher fullMatcher12 = new HashSafeClassAndMethodMatcher(classMatcher1, methodMatcher2);
        HashSafeClassAndMethodMatcher fullMatcher21 = new HashSafeClassAndMethodMatcher(classMatcher2, methodMatcher1);
        HashSafeClassAndMethodMatcher fullMatcher22 = new HashSafeClassAndMethodMatcher(classMatcher2, methodMatcher2);

        Assert.assertTrue(fullMatcher11.equals(fullMatcher11));
        Assert.assertFalse(fullMatcher11.equals(null));
        Assert.assertFalse(fullMatcher11.equals(fullMatcher12));
        Assert.assertFalse(fullMatcher11.equals(fullMatcher21));
        Assert.assertFalse(fullMatcher11.equals(fullMatcher22));

        Assert.assertNotEquals(fullMatcher12.hashCode(), fullMatcher11.hashCode());
    }
}
