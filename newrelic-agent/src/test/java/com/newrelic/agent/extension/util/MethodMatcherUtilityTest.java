package com.newrelic.agent.extension.util;

import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method.Parameters;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method.Parameters.Type;
import com.newrelic.agent.extension.beans.MethodParameters;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.ArrayList;

/**
 * The bulk of this class is tested in com.newrelic.agent.instrumentation.methodmatchers.MethodMatcherTests.
 * This suite specifically addresses Exception handling.
 */
public class MethodMatcherUtilityTest {

    @Test(expected = XmlException.class)
    public void createMethodMatcher_nullMethod_shouldThrow() throws XmlException, NoSuchMethodException{
        MethodMatcherUtility.createMethodMatcher("clazz", (Method) null, null, "foo");
    }

    @Test(expected=XmlException.class)
    public void createMethodMatcher_nullMethodName_shouldThrow() throws XmlException, NoSuchMethodException{
        Method mockMethod = Mockito.mock(Method.class);
        Mockito.when(mockMethod.getReturnType()).thenReturn(null);
        Mockito.when(mockMethod.getName()).thenReturn(null);
        MethodMatcherUtility.createMethodMatcher("clazz", mockMethod, null, "foo");
    }

    @Test(expected = XmlException.class)
    public void createMethodMatcher_emptyMethodName_shouldThrow() throws XmlException, NoSuchMethodException{
        Method mockMethod = Mockito.mock(Method.class);
        Mockito.when(mockMethod.getReturnType()).thenReturn(null);
        Mockito.when(mockMethod.getName()).thenReturn("   ");
        MethodMatcherUtility.createMethodMatcher("clazz", mockMethod, null, "foo");
    }

    @Test(expected = NoSuchMethodException.class)
    public void createMethodMatcher_missingParameters_duplicateMethod_shouldThrow() throws XmlException, NoSuchMethodException {
        Method mockMethod = Mockito.mock(Method.class);
        Mockito.when(mockMethod.getReturnType()).thenReturn(null);
        Mockito.when(mockMethod.getName()).thenReturn("baz");
        Mockito.when(mockMethod.getParameters()).thenReturn(null);
        MethodMatcherUtility.createMethodMatcher(null, mockMethod, null, "foo");
    }

    @Test(expected=XmlException.class)
    public void createMethodMatcher_missingParameterDescriptor_shouldThrow() throws XmlException, NoSuchMethodException{
        Method mockMethod = Mockito.mock(Method.class);
        Parameters mockParams = Mockito.mock(Parameters.class);

        Mockito.when(mockMethod.getReturnType()).thenReturn(null);
        Mockito.when(mockMethod.getName()).thenReturn("baz");
        Mockito.when(mockMethod.getParameters()).thenReturn(mockParams);

        try(MockedStatic<MethodParameters> mockMethodParams = Mockito.mockStatic(MethodParameters.class)) {
            mockMethodParams.when(() -> MethodParameters.getDescriptor(mockParams)).thenReturn(null);
            MethodMatcherUtility.createMethodMatcher(null, mockMethod, null, "foo");
        }
    }

    @Test(expected=NoSuchMethodException.class)
    public void createMethodMatcher_duplicateMethodForParameters_shouldThrow() throws XmlException, NoSuchMethodException{
        Method mockMethod = Mockito.mock(Method.class);
        Parameters mockParams = Mockito.mock(Parameters.class);
        List<Type> typeList = new ArrayList<>();
        typeList.add(new Type());

        Mockito.when(mockMethod.getReturnType()).thenReturn(null);
        Mockito.when(mockMethod.getName()).thenReturn("baz");
        Mockito.when(mockMethod.getParameters()).thenReturn(mockParams);
        Mockito.when(mockParams.getType()).thenReturn(typeList);

        try(MockedStatic<MethodParameters> mockMethodParams = Mockito.mockStatic(MethodParameters.class)) {
            mockMethodParams.when(() -> MethodParameters.getDescriptor(mockParams)).thenReturn("thud");
            MethodMatcherUtility.createMethodMatcher(null, mockMethod, null, "foo");
        }
    }
}