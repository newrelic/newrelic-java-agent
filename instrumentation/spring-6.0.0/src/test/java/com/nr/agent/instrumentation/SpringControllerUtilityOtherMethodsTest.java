package com.nr.agent.instrumentation;

import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SpringControllerUtilityOtherMethodsTest {
    @Test
    public void retrieveRootMappingPathFromController_checkingInheritanceChain_returnsMapping() {
        assertEquals("/root",
                SpringControllerUtility.retrieveRootMappingPathFromController(TestControllerClasses.StandardController.class, true));

        assertEquals("/root",
                SpringControllerUtility.retrieveRootMappingPathFromController(TestControllerClasses.ControllerClassWithInterface.class, true));

        assertEquals("/root",
                SpringControllerUtility.retrieveRootMappingPathFromController(TestControllerClasses.ControllerExtendingAbstractClass.class, true));
    }

    @Test
    public void retrieveRootMappingPathFromController_withoutCheckingInheritanceChain_returnsMappingWhenPresent() {
        assertEquals("/root",
                SpringControllerUtility.retrieveRootMappingPathFromController(TestControllerClasses.StandardController.class, false));

        assertNull(SpringControllerUtility.retrieveRootMappingPathFromController(TestControllerClasses.ControllerClassWithInterface.class, false));

        assertNull(SpringControllerUtility.retrieveRootMappingPathFromController(TestControllerClasses.ControllerExtendingAbstractClass.class, false));
    }

    @Test
    public void doesClassContainControllerAnnotations_checkingInheritanceChain_returnsCorrectValue() {
        assertTrue(SpringControllerUtility.doesClassContainControllerAnnotations(TestControllerClasses.StandardController.class, true));

        assertTrue(SpringControllerUtility.doesClassContainControllerAnnotations(TestControllerClasses.ControllerClassWithInterface.class, true));

        assertTrue(SpringControllerUtility.doesClassContainControllerAnnotations(TestControllerClasses.ControllerExtendingAbstractClass.class, true));

        assertFalse(SpringControllerUtility.doesClassContainControllerAnnotations(TestControllerClasses.NoAnnotationController.class, true));
    }

    @Test
    public void doesClassContainControllerAnnotations_withoutCheckingInheritanceChain_returnsCorrectValue() {
        assertTrue(SpringControllerUtility.doesClassContainControllerAnnotations(TestControllerClasses.StandardController.class, false));

        assertFalse(SpringControllerUtility.doesClassContainControllerAnnotations(TestControllerClasses.ControllerClassWithInterface.class, false));

        assertFalse(SpringControllerUtility.doesClassContainControllerAnnotations(TestControllerClasses.ControllerExtendingAbstractClass.class, false));

        assertFalse(SpringControllerUtility.doesClassContainControllerAnnotations(TestControllerClasses.NoAnnotationController.class, false));
    }

    @Test
    public void assignTransactionNameFromControllerAndMethodRoutes_assignsProperName() {
        Transaction mockTxn = mock(Transaction.class);
        SpringControllerUtility.assignTransactionNameFromControllerAndMethodRoutes(mockTxn, "GET", "/root", "/get");
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController",
                "/root/get (GET)");
    }

    @Test
    public void assignTransactionNameFromControllerAndMethod_assignsProperName() throws NoSuchMethodException {
        Transaction mockTxn = mock(Transaction.class);
        SpringControllerUtility.assignTransactionNameFromControllerAndMethod(mockTxn, TestControllerClasses.StandardController.class,
                TestControllerClasses.StandardController.class.getMethod("get"));
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController",
                "/StandardController/get");
    }

    @Test
    public void getControllerClassAndMethodString_includingPrefix_returnsProperName() throws NoSuchMethodException {
        assertEquals("com.nr.agent.instrumentation.TestControllerClasses$StandardController/get", SpringControllerUtility.getControllerClassAndMethodString(TestControllerClasses.StandardController.class,
                TestControllerClasses.StandardController.class.getMethod("get"), true));
    }

    @Test
    public void getControllerClassAndMethodString_notIncludingPrefix_returnsProperName() throws NoSuchMethodException {
        assertEquals("StandardController/get", SpringControllerUtility.getControllerClassAndMethodString(TestControllerClasses.StandardController.class,
                TestControllerClasses.StandardController.class.getMethod("get"), false));
    }
}
