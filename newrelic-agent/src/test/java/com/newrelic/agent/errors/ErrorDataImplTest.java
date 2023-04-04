package com.newrelic.agent.errors;


import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.transaction.TransactionThrowable;
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

    @Test
    public void testGetErrorClass_transactionDataNull() {
        errorDataImpl = new ErrorDataImpl(null, tracedError);
        assertEquals("", errorDataImpl.getErrorClass());
    }

    @Test
    public void testGetErrorClass_transactionDataNotNull() {
        TransactionThrowable throwable = new TransactionThrowable(new RuntimeException("error"), true, "");
        when(transactionData.getThrowable()).thenReturn(throwable);
        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertEquals(throwable.throwable.getClass().toString(), errorDataImpl.getErrorClass());
    }

    @Test
    public void testGetErrorMessage_transactionDataNotNullThrowableNotNullMessageNotNull() {
        TransactionThrowable throwable = new TransactionThrowable(new RuntimeException("error"), true, "");
        when(transactionData.getThrowable()).thenReturn(throwable);
        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertEquals(throwable.throwable.getMessage(), errorDataImpl.getErrorMessage());
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
        assertEquals("", errorDataImpl.getErrorMessage());
    }

    @Test
    public void testGetStackTraceElement_transactionDataNotNullThrowableNotNull() {
        TransactionThrowable throwable = new TransactionThrowable(new RuntimeException("error"), true, "");
        when(transactionData.getThrowable()).thenReturn(throwable);
        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertArrayEquals(throwable.throwable.getStackTrace(), errorDataImpl.getStackTraceElement());
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
        assertEquals(0, errorDataImpl.getStackTraceElement().length);
    }

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

    @Test
    public void testGetRequestUri_transactionDataNull() {
        errorDataImpl = new ErrorDataImpl(null, tracedError);
        assertEquals("", errorDataImpl.getRequestUri());
    }

    @Test
    public void testGetHttpStatusCode_transactionDataNull() {
        errorDataImpl = new ErrorDataImpl(null, tracedError);
        assertEquals("", errorDataImpl.getHttpStatusCode());
    }

    @Test
    public void testGetHttpMethod_transactionDataNull() {
        errorDataImpl = new ErrorDataImpl(null, tracedError);
        assertEquals("", errorDataImpl.getHttpMethod());
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

    @Test
    public void testGetHttpStatusCode_transactionDataNotNull() {
        String statusCode = "200";
        Map<String, Object> agentAttributes = new HashMap<>();
        agentAttributes.put(AttributeNames.HTTP_STATUS_CODE, statusCode);
        when(transactionData.getAgentAttributes()).thenReturn(agentAttributes);

        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertEquals(statusCode, errorDataImpl.getHttpStatusCode());
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

    @Test
    public void testGetRequestUri_transactionDataNotNull_requestUriKeyMissing() {
        Map<String, Object> agentAttributes = new HashMap<>();
        when(transactionData.getAgentAttributes()).thenReturn(agentAttributes);

        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertEquals("", errorDataImpl.getRequestUri());
    }

    @Test
    public void testGetHttpStatusCode_transactionDataNotNull_statusCodeKeyMissing() {
        Map<String, Object> agentAttributes = new HashMap<>();
        when(transactionData.getAgentAttributes()).thenReturn(agentAttributes);

        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertEquals("", errorDataImpl.getHttpStatusCode());
    }

    @Test
    public void testGetHttpMethod_transactionDataNotNull_httpMethodKeyMissing() {
        Map<String, Object> agentAttributes = new HashMap<>();
        when(transactionData.getAgentAttributes()).thenReturn(agentAttributes);

        errorDataImpl = new ErrorDataImpl(transactionData, tracedError);
        assertEquals("", errorDataImpl.getHttpMethod());
    }

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