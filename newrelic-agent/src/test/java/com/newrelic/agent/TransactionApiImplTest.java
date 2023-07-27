package com.newrelic.agent;

import com.newrelic.agent.bridge.NoOpToken;
import com.newrelic.agent.bridge.NoOpTracedMethod;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.async.AsyncTransactionService;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transaction.TransactionTimer;
import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.ConcurrentHashMapHeaders;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.TransportType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransactionApiImplTest {
    AsyncTransactionService mockAsyncTransactionService;

    @Before
    public void setup() {
        configureServiceFactory();
        mockAsyncTransactionService = mock(AsyncTransactionService.class);
    }

    @Test
    public void equals_properlyDeterminesEquality() {
        TransactionApiImpl transactionApi1 = new TransactionApiImpl();
        TransactionApiImpl transactionApi2 = new TransactionApiImpl();
        assertNotEquals(transactionApi1, new Object());
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            assertEquals(transactionApi1, transactionApi2);
        }
    }

    @Test
    public void hashCode_generatesProperHashValues() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(null);
            assertEquals(42, transactionApi.hashCode());
        }

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            assertNotEquals(42, transactionApi.hashCode());
        }
    }

    @Test
    public void setTransactionName_withActiveTxn_properlySetsTxnName() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);
        when(mockTxn.setTransactionName(TransactionNamePriority.SERVLET_NAME, true, "category", "part1", "part2")).thenReturn(true);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            assertTrue(transactionApi.setTransactionName(TransactionNamePriority.SERVLET_NAME, true, "category", "part1", "part2"));
        }
    }

    @Test
    public void setTransactionName_withNoTxn_ignoresRequest() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);
        verify(mockTxn, times(0)).setTransactionName(TransactionNamePriority.SERVLET_NAME, true, "category", "part1", "part2");

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            assertFalse(transactionApi.setTransactionName(TransactionNamePriority.SERVLET_NAME, true, "category", "part1", "part2"));
        }
    }

    @Test
    public void isTransactionNameSet_withActiveTxn_returnsTrue() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);
        when(mockTxn.isTransactionNameSet()).thenReturn(true);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            assertTrue(transactionApi.isTransactionNameSet());
        }
    }

    @Test
    public void isTransactionNameSet_withNoTxn_returnsFalse() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(null);
            assertFalse(transactionApi.isTransactionNameSet());
        }
    }

    @Test
    public void getLastTracer_withTxn_returnsTracedMethod() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);
        TransactionActivity mockTxnActivity = mock(TransactionActivity.class);
        Tracer mockTracer = mock(Tracer.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
            when(mockTxnActivity.getLastTracer()).thenReturn(mockTracer);

            assertEquals(mockTracer, transactionApi.getLastTracer());
        }
    }

    @Test
    public void getLastTracer_withNoTxn_returnsNoOpTraceMethod() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(null);

            assertEquals(NoOpTracedMethod.INSTANCE, transactionApi.getLastTracer());
        }
    }

    @Test
    public void getLastTracer_withNullLastTracer_returnsNoOpTracedMethod() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);
        TransactionActivity mockTxnActivity = mock(TransactionActivity.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
            when(mockTxnActivity.getLastTracer()).thenReturn(null);

            assertEquals(NoOpTracedMethod.INSTANCE, transactionApi.getLastTracer());
        }
    }

    @Test
    public void ignore_withTxn_ignoresTheTxn() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.ignore();
            verify(mockTxn).ignore();
        }
    }

    @Test
    public void ignoreErrors_withTxn_ignoresErrorsOnTxn() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.ignore();
            verify(mockTxn).ignore();
        }
    }

    @Test
    public void ignoreApdex_withTxn_ignoresApdex() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.ignoreApdex();
            verify(mockTxn).ignoreApdex();
        }
    }

    @Test
    public void beforeSendResponseHeaders_withTxn_addsOutboundHeaders() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.beforeSendResponseHeaders();
            verify(mockTxn).addOutboundResponseHeaders();
        }
    }

    @Test
    public void isStarted_withTxn_returnsStartStatus() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.isStarted();
            verify(mockTxn).isStarted();
        }
    }

    @Test
    public void isAutoAppNamingEnabled_withTxn_returnsTxnAutoAppNamingFlag() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.isAutoAppNamingEnabled();
            verify(mockTxn).isAutoAppNamingEnabled();
        }
    }

    @Test
    public void isWebRequestSet_withTxn_returnsTxnWebRequestFlag() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.isWebRequestSet();
            verify(mockTxn).isWebRequestSet();
        }
    }

    @Test
    public void isWebResponseSet_withTxn_returnsTxnWebResponsetFlag() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.isWebResponseSet();
            verify(mockTxn).isWebResponseSet();
        }
    }

    @Test
    public void setApplicationName_withTxn_setsNameOnTxn() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.setApplicationName(ApplicationNamePriority.NONE, "appName");
            verify(mockTxn).setApplicationName(ApplicationNamePriority.NONE, "appName");
        }
    }

    @Test
    public void isAutoAppNaming_withTxn_returnsAppNamingFlag() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.isAutoAppNamingEnabled();
            verify(mockTxn).isAutoAppNamingEnabled();
        }
    }

    @Test
    public void isWebRequestSet_withTxn_returnsWebRequestSetFlag() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.isWebRequestSet();
            verify(mockTxn).isWebRequestSet();
        }
    }

    @Test
    public void isWebResponseSet_withTxn_returnsWebResponetSetFlag() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.isWebResponseSet();
            verify(mockTxn).isWebResponseSet();
        }
    }

    @Test
    public void setWebRequest_withTxn_assignsRequest() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);
        com.newrelic.api.agent.Request mockRequest = mock(com.newrelic.api.agent.Request.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.setWebRequest(mockRequest);
            verify(mockTxn).setWebRequest(mockRequest);
        }
    }

    @Test
    public void setWebResponse_withTxn_assignsResponse() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);
        com.newrelic.api.agent.Response mockResponse = mock(com.newrelic.api.agent.Response.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.setWebResponse(mockResponse);
            verify(mockTxn).setWebResponse(mockResponse);
        }
    }

    @Test
    public void getWebResponse_withTxn_returnsResponse() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.getWebResponse();
            verify(mockTxn).getWebResponse();
        }
    }

    @Test
    public void convertToWebTransaction_withTxn_convertsTxn() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.convertToWebTransaction();
            verify(mockTxn).convertToWebTransaction();
        }
    }

    @Test
    public void isWebTransaction_withTxn_returnsWebTxnFlag() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.isWebTransaction();
            verify(mockTxn).isWebTransaction();
        }
    }

    @Test
    public void ignoreErrors_withTxn_setsFlagOnTxn() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.ignoreErrors();
            verify(mockTxn).ignoreErrors();
        }
    }

    @Test
    public void startFlyweightTracer_withoutTxn_startsFlyweightTracer() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        TransactionActivity mockTxnActivity = mock(TransactionActivity.class);
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
            TracedMethod result = transactionApi.startFlyweightTracer();
            assertEquals(NoOpTracedMethod.INSTANCE, result);
        }
    }

    @Test
    public void startFlyweightTracer_withTxn_startsFlyweightTracer() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        TransactionActivity mockTxnActivity = mock(TransactionActivity.class);
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            when(mockTxn.isStarted()).thenReturn(true);
            when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
            transactionApi.startFlyweightTracer();
            verify(mockTxnActivity).startFlyweightTracer();
        }
    }

    @Test
    public void finishFlyweightTracer_withTxn_startsFlyweightTracer() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        TransactionActivity mockTxnActivity = mock(TransactionActivity.class);
        Transaction mockTxn = mock(Transaction.class);
        TracedMethod mockTracedMethod = mock(com.newrelic.agent.bridge.TracedMethod.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            when(mockTxn.isStarted()).thenReturn(true);
            when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
            transactionApi.finishFlyweightTracer(mockTracedMethod, 1000, 2500, "class", "method", "desc", "metricName", new String[] {});
            verify(mockTxnActivity).finishFlyweightTracer(mockTracedMethod, 1000, 2500, "class", "method", "desc", "metricName", new String[] {});
        }
    }

    @Test
    public void getAgentAttributes_withTxn_fetchesAttributes() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.getAgentAttributes();
            verify(mockTxn).getAgentAttributes();
        }
    }

    @Test
    public void provideHeaders_withTxn_fetchesAttributes() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.provideHeaders(ConcurrentHashMapHeaders.build(HeaderType.MESSAGE));
            verify(mockTxn).provideHeaders(any());
        }
    }

    @Test
    public void setWebRequest_withTxn_setsWebRequestOnTxn() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            ExtendedRequest mockRequest = mock(ExtendedRequest.class);
            transactionApi.setWebRequest(mockRequest);
            verify(mockTxn).setWebRequest(mockRequest);
        }
    }

    @Test
    public void markFirstByteOfResponse_withTxn_marksByteOnTxn() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.markFirstByteOfResponse();
            verify(mockTxn).markFirstByteOfResponse(anyLong());
        }
    }

    @Test
    public void markLastByteOfResponse_withTxn_marksLastByteOnTxn() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.markLastByteOfResponse();
            verify(mockTxn).markLastByteOfResponse(anyLong());
        }
    }

    @Test
    public void markResponseAtTxaEnd_withTxn_marksResponseOnTxnActivity() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            TransactionActivity mockTxnActivity = mock(TransactionActivity.class);
            when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
            transactionApi.markResponseAtTxaEnd();
            verify(mockTxn).getTransactionActivity();
            verify(mockTxnActivity).markAsResponseSender();
        }
    }

    @Test
    public void markResponseSent_withTxn_returnsTrue() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            TransactionActivity mockTxnActivity = mock(TransactionActivity.class);
            TransactionTimer mockTxnTimer = mock(TransactionTimer.class);
            when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
            when(mockTxn.getTransactionTimer()).thenReturn(mockTxnTimer);
            when(mockTxnTimer.markResponseTime(anyLong())).thenReturn(true);
            assertTrue(transactionApi.markResponseSent());
            verify(mockTxn).getTransactionActivity();
            verify(mockTxnActivity).markAsResponseSender();
        }
    }

    @Test
    public void markResponseSent_withTxnAndMarkResponseTimeIsFalse_returnsFalse() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            TransactionActivity mockTxnActivity = mock(TransactionActivity.class);
            TransactionTimer mockTxnTimer = mock(TransactionTimer.class);
            when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
            when(mockTxn.getTransactionTimer()).thenReturn(mockTxnTimer);
            when(mockTxnTimer.markResponseTime(anyLong())).thenReturn(false);
            assertFalse(transactionApi.markResponseSent());
            verify(mockTxn).getTransactionActivity();
            verify(mockTxnActivity).markAsResponseSender();
        }
    }

    @Test
    public void markResponseSent_withoutTxn_returnsFalse() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(null);
            assertFalse(transactionApi.markResponseSent());
            verify(mockTxn, times(0)).getTransactionActivity();
        }
    }

    @Test
    public void getToken_withoutTxn_returnsNoopTokenInstance() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(null);
            assertEquals(NoOpToken.INSTANCE, transactionApi.getToken());
        }
    }

    @Test
    public void getToken_withTxn_returnsToken() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.getToken();
            verify(mockTxn).getToken();
        }
    }

    @Test
    public void clearTransaction_withTxn_clearsCurrentTxn() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);
        TransactionActivity mockTxnActivity = mock(TransactionActivity.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            when(mockTxn.getTransactionActivity()).thenReturn(mockTxnActivity);
            assertTrue(transactionApi.clearTransaction());
            mockStaticTxn.verify(Transaction::clearTransaction);
            verify(mockTxn).checkFinishTransactionFromActivity(mockTxnActivity);
        }
    }

    @Test
    public void acceptDistributedTracePayload_withTxn_acceptsPayload() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.acceptDistributedTracePayload("str");
            verify(mockTxn).acceptDistributedTracePayload("str");
        }
    }

    @Test
    public void getSecurityMetadata_withTxn_returnsMetadata() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.getSecurityMetaData();
            verify(mockTxn).getSecurityMetaData();
        }
    }

    @Test
    public void setTransportType_withTxn_returnsMetadata() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.setTransportType(TransportType.HTTP);
            verify(mockTxn).setTransportType(TransportType.HTTP);
        }
    }

    @Test
    public void requestInitialized_withTxn_initsRequest() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);
        com.newrelic.api.agent.Response mockResponse = mock(com.newrelic.api.agent.Response.class);
        com.newrelic.api.agent.Request mockRequest = mock(com.newrelic.api.agent.Request.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.requestInitialized(mockRequest, mockResponse);
            verify(mockTxn).requestInitialized(mockRequest, mockResponse);
        }
    }

    @Test
    public void requestDestroyed_withTxn_destroysRequest() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.requestDestroyed();
            verify(mockTxn).requestDestroyed();
        }
    }

    @Test
    public void saveMessageParameters_withTxn_saveParams() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);
        Map<String, String> map = new HashMap<>();

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.saveMessageParameters(map);
            verify(mockTxn).saveMessageParameters(map);
        }
    }

    @Test
    public void getCrossProcessState_withTxn_returnsProcessState() {
        TransactionApiImpl transactionApi = new TransactionApiImpl();
        Transaction mockTxn = mock(Transaction.class);

        try (MockedStatic<com.newrelic.agent.Transaction> mockStaticTxn = mockStatic(com.newrelic.agent.Transaction.class)) {
            mockStaticTxn.when(() -> com.newrelic.agent.Transaction.getTransaction(false)).thenReturn(mockTxn);
            transactionApi.getCrossProcessState();
            verify(mockTxn).getCrossProcessState();
        }
    }

    private void configureServiceFactory() {
        ServiceManager serviceManager = mock(ServiceManager.class);
        when(serviceManager.getAsyncTxService()).thenReturn(mockAsyncTransactionService);
        when(serviceManager.getConfigService()).thenReturn(Mockito.mock(ConfigService.class));
        when(serviceManager.getConfigService().getDefaultAgentConfig()).thenReturn(AgentConfigImpl.createAgentConfig(null));
        ServiceFactory.setServiceManager(serviceManager);
    }
}