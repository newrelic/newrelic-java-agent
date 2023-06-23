package com.newrelic.agent;

import com.newrelic.agent.bridge.NoOpTracedMethod;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.async.AsyncTransactionService;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.ApplicationNamePriority;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
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
            transactionApi.addOutboundResponseHeaders();
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

    private void configureServiceFactory() {
        ServiceManager serviceManager = mock(ServiceManager.class);
        when(serviceManager.getAsyncTxService()).thenReturn(mockAsyncTransactionService);
        when(serviceManager.getConfigService()).thenReturn(Mockito.mock(ConfigService.class));
        when(serviceManager.getConfigService().getDefaultAgentConfig()).thenReturn(AgentConfigImpl.createAgentConfig(null));
        ServiceFactory.setServiceManager(serviceManager);
    }
}
