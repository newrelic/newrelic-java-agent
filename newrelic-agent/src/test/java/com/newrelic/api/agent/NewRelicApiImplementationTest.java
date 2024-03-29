/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import com.newrelic.agent.IRPMService;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.attributes.AgentAttributeSender;
import com.newrelic.agent.attributes.AttributeSender;
import com.newrelic.agent.config.ConfigConstant;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.errors.ErrorGroupCallbackHolder;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class NewRelicApiImplementationTest {

    @Before
    public void setUp() {
        previousServiceManager = ServiceFactory.getServiceManager();
    }

    @After
    public void tearDown() {
        ServiceFactory.setServiceManager(previousServiceManager);
    }

    @Test
    public void noticeErrorWithExceptionShouldProduceAnExceptionReport() {
        mockOutServices();

        NewRelicApiImplementation target = new NewRelicApiImplementation();
        Exception exc = new Exception("~~ oops ~~");
        target.noticeError(exc);

        Mockito.verify(errorService).reportException(exc, Collections.emptyMap(), false);
    }

    @Test
    public void noticeErrorWithExceptionShouldProduceAnExceptionReport2() {
        mockOutServices();

        NewRelicApiImplementation target = new NewRelicApiImplementation();
        Exception exc = new Exception("~~ oops ~~");
        target.noticeError(exc, true);

        Mockito.verify(errorService).reportException(exc, Collections.emptyMap(), true);
    }

    @Test
    public void noticeErrorWithStringMessageShouldProduceAnExceptionReport() {
        mockOutServices();

        NewRelicApiImplementation target = new NewRelicApiImplementation();
        target.noticeError("errorMessage", true);

        Mockito.verify(errorService).reportError("errorMessage", Collections.emptyMap(), true);
    }

    @Test
    public void noticeErrorWithStringMessageShouldProduceAnExceptionReport2() {
        mockOutServices();

        NewRelicApiImplementation target = new NewRelicApiImplementation();
        target.noticeError("errorMessage");

        Mockito.verify(errorService).reportError("errorMessage", Collections.emptyMap(), false);
    }

    @Test
    public void ignoreTransaction_withValidTxn_setsIgnoreFlag() {
        mockOutServices();
        Transaction txn = Mockito.mock(Transaction.class);
        NewRelicApiImplementation target = new NewRelicApiImplementation();

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(txn);
            target.ignoreTransaction();
            Mockito.verify(txn, Mockito.times(1)).setIgnore(true);
        }
    }

    @Test
    public void ignoreTransaction_withNullTxn_setsIgnoreFlag() {
        mockOutServices();
        Transaction txn = Mockito.mock(Transaction.class);
        NewRelicApiImplementation target = new NewRelicApiImplementation();

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(null);
            target.ignoreTransaction();
            Mockito.verify(txn, Mockito.times(0)).setIgnore(true);
        }
    }

    @Test
    public void ignoreApdex_withValidTxn_setsIgnoreFlag() {
        mockOutServices();
        Transaction txn = Mockito.mock(Transaction.class);
        NewRelicApiImplementation target = new NewRelicApiImplementation();

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(txn);
            target.ignoreApdex();
            Mockito.verify(txn, Mockito.times(1)).ignoreApdex();
        }
    }

    @Test
    public void ignoreApdex_withNullTxn_setsIgnoreFlag() {
        mockOutServices();
        Transaction txn = Mockito.mock(Transaction.class);
        NewRelicApiImplementation target = new NewRelicApiImplementation();

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(null);
            target.ignoreApdex();
            Mockito.verify(txn, Mockito.times(0)).ignoreApdex();
        }
    }

    @Test
    public void setRequestAndResponse_withValidTxn_setsReqAndResp() {
        mockOutServices();
        Transaction txn = Mockito.mock(Transaction.class);
        Request mockReq = Mockito.mock(Request.class);
        Response mockResp = Mockito.mock(Response.class);

        NewRelicApiImplementation target = new NewRelicApiImplementation();

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(txn);
            target.setRequestAndResponse(mockReq, mockResp);
            Mockito.verify(txn, Mockito.times(1)).setRequestAndResponse(mockReq, mockResp);
        }
    }

    @Test
    public void setRequestAndResponse_withNullTxn_setsReqAndResp() {
        mockOutServices();
        Transaction txn = Mockito.mock(Transaction.class);
        Request mockReq = Mockito.mock(Request.class);
        Response mockResp = Mockito.mock(Response.class);
        NewRelicApiImplementation target = new NewRelicApiImplementation();

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(null);
            target.setRequestAndResponse(mockReq, mockResp);
            Mockito.verify(txn, Mockito.times(0)).setRequestAndResponse(Mockito.any(), Mockito.any());
        }
    }

    @Test
    public void getBrowserTimingHeaderForContentType_withTxn_returnsHeader() {
        mockOutServices();
        Transaction txn = Mockito.mock(Transaction.class, Mockito.RETURNS_DEEP_STUBS);

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(txn);
            Mockito.when(txn.getBrowserTransactionState().getBrowserTimingHeaderForJsp()).thenReturn("header");
            assertEquals("header", NewRelicApiImplementation.getBrowserTimingHeaderForContentType("application/json"));
        }
    }

    @Test
    public void getBrowserTimingHeaderForContentType_withNullTxn_returnsEmptyString() {
        mockOutServices();

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(null);
            assertEquals("", NewRelicApiImplementation.getBrowserTimingHeaderForContentType("application/json"));
        }
    }

    @Test
    public void getBrowserTimingHeader_withTxn_returnsHeader() {
        mockOutServices();
        Transaction txn = Mockito.mock(Transaction.class, Mockito.RETURNS_DEEP_STUBS);
        NewRelicApiImplementation target = new NewRelicApiImplementation();

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(txn);
            Mockito.when(txn.getBrowserTransactionState().getBrowserTimingHeader()).thenReturn("header");
            assertEquals("header", target.getBrowserTimingHeader());
        }
    }

    @Test
    public void getBrowserTimingHeaderWithNonce_withTxn_returnsHeader() {
        mockOutServices();
        Transaction txn = Mockito.mock(Transaction.class, Mockito.RETURNS_DEEP_STUBS);
        NewRelicApiImplementation target = new NewRelicApiImplementation();

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(txn);
            Mockito.when(txn.getBrowserTransactionState().getBrowserTimingHeader("123")).thenReturn("header");
            assertEquals("header", target.getBrowserTimingHeader("123"));
        }
    }

    @Test
    public void getBrowserTimingHeaderWithoutNonce_withTxn_returnsHeader() {
        mockOutServices();
        Transaction txn = Mockito.mock(Transaction.class, Mockito.RETURNS_DEEP_STUBS);
        NewRelicApiImplementation target = new NewRelicApiImplementation();

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(txn);
            Mockito.when(txn.getBrowserTransactionState().getBrowserTimingHeader()).thenReturn("header");
            assertEquals("header", target.getBrowserTimingHeader());
        }
    }

    @Test
    public void getBrowserTimingHeaderWithoutNonce_withoutTxn_returnsHeader() {
        mockOutServices();
        Transaction txn = Mockito.mock(Transaction.class, Mockito.RETURNS_DEEP_STUBS);
        NewRelicApiImplementation target = new NewRelicApiImplementation();

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(null);
            assertEquals("", target.getBrowserTimingHeader());
        }
    }

    @Test
    public void setUserId_withValidId_setsAttribute() {
        mockOutServices();
        AttributeSender mockSender = Mockito.mock(AttributeSender.class);
        AgentAttributeSender mockAgentSender = Mockito.mock(AgentAttributeSender.class);
        NewRelicApiImplementation target = new NewRelicApiImplementation(mockSender, mockAgentSender);

        target.setUserId("123");
        Mockito.verify(mockAgentSender).addAttribute("enduser.id", "123", "setUserId");
    }

    @Test
    public void setUserId_withInvalidId_doesNotSetAttribute() {
        mockOutServices();
        AttributeSender mockSender = Mockito.mock(AttributeSender.class);
        AgentAttributeSender mockAgentSender = Mockito.mock(AgentAttributeSender.class);
        NewRelicApiImplementation target = new NewRelicApiImplementation(mockSender, mockAgentSender);

        target.setUserId(null);
        Mockito.verify(mockAgentSender, Mockito.times(0)).addAttribute("enduser.id", "123", "setUserId");

        target.setUserId("");
        Mockito.verify(mockAgentSender, Mockito.times(0)).addAttribute("enduser.id", "123", "setUserId");
    }

    @Test
    public void setUserId_withoutActiveTxn_isNoOp() {
        mockOutServices();
        AttributeSender mockSender = Mockito.mock(AttributeSender.class);
        AgentAttributeSender mockAgentSender = new AgentAttributeSender();
        NewRelicApiImplementation target = new NewRelicApiImplementation(mockSender, mockAgentSender);

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(null);
            //These used to throw an NPE when there was no active transaction so the "assertion" is that these method calls execute without an exception
            target.setUserId(null);
            target.setUserId("");
        }
    }

    @Test
    public void setUserName_withValidName_setsNameAttribute() {
        mockOutServices();
        AttributeSender mockSender = Mockito.mock(AttributeSender.class);
        AgentAttributeSender mockAgentSender = Mockito.mock(AgentAttributeSender.class);
        Transaction txn = Mockito.mock(Transaction.class);
        Dispatcher mockDispatcher = Mockito.mock(Dispatcher.class);
        NewRelicApiImplementation target = new NewRelicApiImplementation(mockSender, mockAgentSender);

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(txn);
            Mockito.when(txn.getDispatcher()).thenReturn(mockDispatcher);
            Mockito.when(mockDispatcher.isWebTransaction()).thenReturn(true);
            MockedStatic<MetricNames> mockMetricNames = Mockito.mockStatic(MetricNames.class);

            target.setUserName("name");
            mockMetricNames.verify(() -> MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_USER_NAME));
            Mockito.verify(mockSender).addAttribute("user", "name", "setUserName");
            mockMetricNames.close();
        }
    }

    @Test
    public void setAccountName_withValidName_setsNameAttribute() {
        mockOutServices();
        AttributeSender mockSender = Mockito.mock(AttributeSender.class);
        AgentAttributeSender mockAgentSender = Mockito.mock(AgentAttributeSender.class);
        Transaction txn = Mockito.mock(Transaction.class);
        Dispatcher mockDispatcher = Mockito.mock(Dispatcher.class);
        NewRelicApiImplementation target = new NewRelicApiImplementation(mockSender, mockAgentSender);

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(txn);
            Mockito.when(txn.getDispatcher()).thenReturn(mockDispatcher);
            Mockito.when(mockDispatcher.isWebTransaction()).thenReturn(true);
            MockedStatic<MetricNames> mockMetricNames = Mockito.mockStatic(MetricNames.class);

            target.setAccountName("name");
            mockMetricNames.verify(() -> MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_ACCOUNT_NAME));
            Mockito.verify(mockSender).addAttribute("account", "name", "setAccountName");
            mockMetricNames.close();
        }
    }

    @Test
    public void setProductName_withValidName_setsNameAttribute() {
        mockOutServices();
        AttributeSender mockSender = Mockito.mock(AttributeSender.class);
        AgentAttributeSender mockAgentSender = Mockito.mock(AgentAttributeSender.class);
        Transaction txn = Mockito.mock(Transaction.class);
        Dispatcher mockDispatcher = Mockito.mock(Dispatcher.class);
        NewRelicApiImplementation target = new NewRelicApiImplementation(mockSender, mockAgentSender);

        try(MockedStatic<Transaction> mockTxn = Mockito.mockStatic(Transaction.class)) {
            mockTxn.when(() -> Transaction.getTransaction(false)).thenReturn(txn);
            Mockito.when(txn.getDispatcher()).thenReturn(mockDispatcher);
            Mockito.when(mockDispatcher.isWebTransaction()).thenReturn(true);
            MockedStatic<MetricNames> mockMetricNames = Mockito.mockStatic(MetricNames.class);

            target.setProductName("name");
            mockMetricNames.verify(() -> MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_PRODUCT_NAME));
            Mockito.verify(mockSender).addAttribute("product", "name", "setProductName");
            mockMetricNames.close();
        }
    }

    @Test
    public void setAppServerPort_recordsSupportabilityMetric() {
        mockOutServices();
        NewRelicApiImplementation target = new NewRelicApiImplementation();

        try(MockedStatic<MetricNames> mockMetricNames = Mockito.mockStatic(MetricNames.class)) {
            target.setAppServerPort(8888);
            mockMetricNames.verify(() -> MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_APP_SERVER_PORT));
        }
    }

    @Test
    public void setServerInfo_recordsSupportabilityMetric() {
        mockOutServices();
        NewRelicApiImplementation target = new NewRelicApiImplementation();

        try(MockedStatic<MetricNames> mockMetricNames = Mockito.mockStatic(MetricNames.class)) {
            target.setServerInfo("dispatcher", "1");
            mockMetricNames.verify(() -> MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_SERVER_INFO));
        }
    }

    @Test
    public void setInstanceName_recordsSupportabilityMetric() {
        mockOutServices();
        NewRelicApiImplementation target = new NewRelicApiImplementation();

        try(MockedStatic<MetricNames> mockMetricNames = Mockito.mockStatic(MetricNames.class)) {
            target.setInstanceName("instance");
            mockMetricNames.verify(() -> MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_INSTANCE_NAME));
        }
    }

    @Test
    public void setUserId_recordsSupportabilityMetric() {
        mockOutServices();
        NewRelicApiImplementation target = new NewRelicApiImplementation();

        try(MockedStatic<MetricNames> mockMetricNames = Mockito.mockStatic(MetricNames.class)) {
            target.setUserId("instance");
            mockMetricNames.verify(() -> MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_USER_ID));
        }
    }

    @Test
    public void setErrorGroupCallback_setsCallback() {
        mockOutServices();
        NewRelicApiImplementation target = new NewRelicApiImplementation();

        try(MockedStatic<ErrorGroupCallbackHolder> mockCallbackHandler = Mockito.mockStatic(ErrorGroupCallbackHolder.class)) {
            target.setErrorGroupCallback(Mockito.mock(ErrorGroupCallback.class));
            mockCallbackHandler.verify(() -> ErrorGroupCallbackHolder.setErrorGroupCallback(Mockito.any(ErrorGroupCallback.class)));
        }
    }

    @Test
    public void noticeErrorShouldAcceptNumberAndBooleanAttributeType() {
        mockOutServices();

        Map<String, Object> attributes = new HashMap<>();
        Map<String, Object> expectedValues = new HashMap<>();

        attributes.put("MyNumber", 54);
        expectedValues.put("MyNumber", 54);

        attributes.put("MyAtomicInteger", new AtomicInteger(54));
        expectedValues.put("MyAtomicInteger", 54);

        attributes.put("MyAtomicLong", new AtomicLong(54));
        expectedValues.put("MyAtomicLong", 54L);

        attributes.put("MyAtomicBool", new AtomicBoolean(true));
        expectedValues.put("MyAtomicBool", true);

        attributes.put("MyBigDecimal", BigDecimal.valueOf(10.0000001));
        expectedValues.put("MyBigDecimal", BigDecimal.valueOf(10.0000001));

        attributes.put("MyBigInteger", BigInteger.valueOf(10000000L));
        expectedValues.put("MyBigInteger", BigInteger.valueOf(10000000L));

        // Invalid attribute values
        attributes.put("MyNaN", Double.NaN);
        attributes.put("MyPosInf", Double.POSITIVE_INFINITY);
        attributes.put("MyNegInf", Double.NEGATIVE_INFINITY);

        NewRelicApiImplementation target = new NewRelicApiImplementation();

        Exception exc = new Exception("~~ oops ~~");
        target.noticeError(exc, attributes);

        Mockito.verify(errorService).reportException(exc, expectedValues, false);

    }

    @Test
    public void noticeErrorShouldALimitAttributeCount() {
        mockOutServices();

        Map<String, Object> attributes = new HashMap<>();

        for (int i=0;i<ConfigConstant.MAX_USER_ATTRIBUTES;i++) {
            attributes.put("MyNumber"+i, i);
        }

        NewRelicApiImplementation target = new NewRelicApiImplementation();

        Exception exc = new Exception("~~ oops ~~");
        target.noticeError(exc, attributes);

        ArgumentCaptor<Map<String, ?>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(errorService).reportException(Mockito.eq(exc), mapCaptor.capture(), Mockito.eq(false));
        Assert.assertEquals(ConfigConstant.MAX_USER_ATTRIBUTES-1, mapCaptor.getValue().size()); // yes, minus 1
    }

    @Test
    public void noticeErrorShouldSerializeAttributesUsingToString() {
        mockOutServices();

        Map<String, Object> attributes = new HashMap<>();
        Map<String, Object> expectedValues = new HashMap<>();

        attributes.put("MyString", "foobar");
        expectedValues.put("MyString", "foobar");

        attributes.put("MyBoolean", true);
        expectedValues.put("MyBoolean", true);

        attributes.put("MyEnum", MyEnum.VALUE2);
        expectedValues.put("MyEnum", "VALUE2");

        NewRelicApiImplementation target = new NewRelicApiImplementation();

        Exception exc = new Exception("~~ oops ~~");
        target.noticeError(exc, attributes);

        Mockito.verify(errorService).reportException(exc, expectedValues, false);
    }

    @Test
    public void attributeSerializationHandlesExceptions() {
        mockOutServices();

        Object foo = new Object() {
            @Override
            public String toString() {
                return String.valueOf(1 / 0);
            }
        };

        Map<String, Object> attributes = new HashMap<>();
        Map<String, String> expectedValues = new HashMap<>();

        attributes.put("MyWhoops", foo);

        attributes.put("MyString", "foobar");
        expectedValues.put("MyString", "foobar");

        NewRelicApiImplementation target = new NewRelicApiImplementation();

        Exception exc = new Exception("~~ oops ~~");
        target.noticeError(exc, attributes);

        Mockito.verify(errorService).reportException(exc, expectedValues, false);
    }

    private void mockOutServices() {
        errorService = Mockito.mock(ErrorService.class);
        IRPMService rpmService = Mockito.mock(IRPMService.class);
        Mockito.when(rpmService.getErrorService()).thenReturn(errorService);

        RPMServiceManager rpmServiceManager = Mockito.mock(RPMServiceManager.class);
        Mockito.when(rpmServiceManager.getRPMService()).thenReturn(rpmService);

        MockServiceManager sm = new MockServiceManager();
        sm.setRPMServiceManager(rpmServiceManager);

        ServiceFactory.setServiceManager(sm);
    }

    private ErrorService errorService;
    private ServiceManager previousServiceManager;

    enum MyEnum {
        VALUE1,
        VALUE2,
        VALUE3
    }
}
