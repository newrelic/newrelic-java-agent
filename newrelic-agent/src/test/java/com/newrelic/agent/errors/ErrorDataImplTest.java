package com.newrelic.agent.errors;


import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.transaction.PriorityTransactionName;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ErrorDataImplTest {

    private TransactionData transactionData;
    private TracedError tracedError;
    private ThrowableError throwableError;
    private ErrorDataImpl errorDataImpl;

    @Before
    public void setUp() {
        transactionData = mock(TransactionData.class);
        tracedError = mock(TracedError.class);
        throwableError = mock(ThrowableError.class);
    }

    // Test cases for getException()

    @Test
    public void testGetException_tracedErrorNotInstanceOfThrowableError() {
        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertNull(errorDataImpl.getException());
    }

    @Test
    public void testGetException_tracedErrorInstanceOfThrowableError() {
        Throwable throwable = new RuntimeException("Test exception");
        when(throwableError.getThrowable()).thenReturn(throwable);
        errorDataImpl = new ErrorDataImpl(transactionData, throwableError);
        assertEquals(throwable, errorDataImpl.getException());
    }

    // Test cases for getErrorClass()

    @Test
    public void testGetErrorClass_transactionDataNull() {
        errorDataImpl = new ErrorDataImpl(null, tracedError);
        assertNull(errorDataImpl.getErrorClass());
    }


    //TODO correct this test
    @Test
    public void testGetErrorClass_transactionDataNotNull() {
        Throwable throwable = new RuntimeException("Test exception");
//        when(transactionData.getThrowable()).thenReturn(throwable);
        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertEquals(throwable.getClass().toString(), errorDataImpl.getErrorClass());
    }

    // Test cases for getErrorMessage()

    @Test
    public void testGetErrorMessage_transactionDataNotNullThrowableNotNullMessageNotNull() {
        Throwable throwable = new RuntimeException("Test exception");
//        when(transactionData.getThrowable()).thenReturn(throwable);
        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertEquals(throwable.getMessage(), errorDataImpl.getErrorMessage());
    }

    @Test
    public void testGetErrorMessage_tracedErrorInstanceOfThrowableErrorThrowableNotNull() {
        Throwable throwable = new RuntimeException("Test exception");
        when(throwableError.getThrowable()).thenReturn(throwable);
        errorDataImpl = new ErrorDataImpl(transactionData, throwableError);
        assertEquals(throwable.getMessage(), errorDataImpl.getErrorMessage());
    }

    @Test
    public void testGetErrorMessage_noMessageAvailable() {
        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertNull(errorDataImpl.getErrorMessage());
    }

    // Test cases for getStackTraceElement()
    //TODO correct this test
    @Test
    public void testGetStackTraceElement_transactionDataNotNullThrowableNotNull() {
        Throwable throwable = new RuntimeException("Test exception");
//        when(transactionData.getThrowable()).thenReturn(throwable);
        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertArrayEquals(throwable.getStackTrace(), errorDataImpl.getStackTraceElement());
    }

    @Test
    public void testGetStackTraceElement_tracedErrorInstanceOfThrowableErrorThrowableNotNull() {
        Throwable throwable = new RuntimeException("Test exception");
        when(throwableError.getThrowable()).thenReturn(throwable);
        errorDataImpl = new ErrorDataImpl(transactionData, throwableError);
        assertArrayEquals(throwable.getStackTrace(), errorDataImpl.getStackTraceElement());
    }
    @Test
    public void testGetStackTraceElement_noStackTraceAvailable() {
        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertNull(errorDataImpl.getStackTraceElement());
    }

    // Test cases for getCustomAttributes()
    //TODO correct this test
    @Test
    public void testGetCustomAttributes_transactionDataAttributesAndTracedErrorAttributes() {
        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("userKey", "userValue");
        Map<String, Object> errorAttributes = new HashMap<>();
        errorAttributes.put("errorKey", "errorValue");
        Map<String, Object> tracedErrorAttributes = new HashMap<>();
        tracedErrorAttributes.put("tracedKey", "tracedValue");

        when(transactionData.getUserAttributes()).thenReturn(userAttributes);
//        when(transactionData.getErrorAttributes()).thenReturn(errorAttributes);
//        when(tracedError.getErrorAtts()).thenReturn(tracedErrorAttributes);

        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);

        Map<String, Object> expectedAttributes = new HashMap<>();
        expectedAttributes.putAll(userAttributes);
        expectedAttributes.putAll(errorAttributes);
        expectedAttributes.putAll(tracedErrorAttributes);

        assertEquals(expectedAttributes, errorDataImpl.getCustomAttributes());
    }

    // Test cases for getTransactionName()

    @Test
    public void testGetTransactionName() {
        String transactionName = "TestTransaction";
        Transaction transaction = mock(Transaction.class);
        PriorityTransactionName priorityTransactionName = mock(PriorityTransactionName.class);

        when(transaction.getPriorityTransactionName()).thenReturn(priorityTransactionName);
        when(priorityTransactionName.getName()).thenReturn(transactionName);
        when(transactionData.getTransaction()).thenReturn(transaction);

        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertEquals(transactionName, errorDataImpl.getTransactionName());
    }

    // Test cases for getTransactionUiName()

    @Test
    public void testGetTransactionUiName() {
        String transactionName = "TestTransaction";
        Transaction transaction = mock(Transaction.class);
        PriorityTransactionName priorityTransactionName = mock(PriorityTransactionName.class);

        when(transaction.getPriorityTransactionName()).thenReturn(priorityTransactionName);
        when(priorityTransactionName.getName()).thenReturn(transactionName);
        when(transactionData.getTransaction()).thenReturn(transaction);

        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertEquals(transactionName, errorDataImpl.getTransactionUiName());
    }

    // Test cases for getRequestUri(), getHttpStatusCode(), and getHttpMethod()

    @Test
    public void testGetRequestUri_transactionDataNull() {
        errorDataImpl = new ErrorDataImpl(null, tracedError);
        assertNull(errorDataImpl.getRequestUri());
    }

    @Test
    public void testGetHttpStatusCode_transactionDataNull() {
        errorDataImpl = new ErrorDataImpl(null, tracedError);
        assertNull(errorDataImpl.getHttpStatusCode());
    }

    @Test
    public void testGetHttpMethod_transactionDataNull() {
        errorDataImpl = new ErrorDataImpl(null, tracedError);
        assertNull(errorDataImpl.getHttpMethod());
    }

    @Test
    public void testGetRequestUri_transactionDataNotNull() {
        String requestUri = "/test/uri";
        Map<String, Object> agentAttributes = new HashMap<>();
        agentAttributes.put(AttributeNames.REQUEST_URI, requestUri);
        when(transactionData.getAgentAttributes()).thenReturn(agentAttributes);

        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertEquals(requestUri, errorDataImpl.getRequestUri());
    }

    //TODO correct this test
    @Test
    public void testGetHttpStatusCode_transactionDataNotNull() {
        String httpStatusCode = "200";
//        Map < String, Object
    }


        @Test
        public void testGetHttpMethod_transactionDataNotNull() {
            String httpMethod = "GET";
            Map<String, Object> agentAttributes = new HashMap<>();
            agentAttributes.put(AttributeNames.REQUEST_METHOD_PARAMETER_NAME, httpMethod);
            when(transactionData.getAgentAttributes()).thenReturn(agentAttributes);

            errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
            assertEquals(httpMethod, errorDataImpl.getHttpMethod());
        }

        // Test cases for isErrorExpected()

        @Test
        public void testIsErrorExpected_tracedErrorNull() {
            errorDataImpl = new ErrorDataImpl(transactionData, null);
            assertFalse(errorDataImpl.isErrorExpected());
        }

        @Test
        public void testIsErrorExpected_tracedErrorNotNull() {
            when(tracedError.isExpected()).thenReturn(true);
            errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
            assertTrue(errorDataImpl.isErrorExpected());
        }
    }


