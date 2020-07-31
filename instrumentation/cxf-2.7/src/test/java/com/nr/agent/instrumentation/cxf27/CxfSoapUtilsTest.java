/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.cxf27;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.PrivateApi;
import com.newrelic.agent.bridge.PublicApi;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;

public class CxfSoapUtilsTest {
    @Mock
    PrivateApi mockPrivateApi;
    @Mock
    PublicApi mockPublicApi;
    @Mock
    SoapMessage mockSoapMessage;
    @Mock
    Transaction mockTransaction;

    @Before
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);
        AgentBridge.privateApi = mockPrivateApi;
        AgentBridge.publicApi = mockPublicApi;
        AgentBridge.agent = Mockito.mock(Agent.class);

        Mockito.when(AgentBridge.agent.getTransaction()).thenReturn(mockTransaction);
    }

    @Test
    public void testHandleFault() {
        Fault aFault = new Fault(new Exception("A mock fault"));
        Mockito.when(mockSoapMessage.getContent(Exception.class)).thenReturn(aFault);
        CxfSoapUtils.handleFault(mockSoapMessage);
        Mockito.verify(mockPrivateApi, Mockito.times(1)).reportException(aFault);
        Mockito.verifyNoMoreInteractions(mockPublicApi);
    }

    @Test
    public void testNameTransaction() throws URISyntaxException {
        URI fakeEndpoint = new URI("http://localhost:8080/path/to/endpoint");
        QName fakeOperation = new QName("http://namespaceuri.com/not/a/real/namespace", "aSoapOperation");
        Mockito.when(mockSoapMessage.get("javax.xml.ws.wsdl.description")).thenReturn(fakeEndpoint);
        Mockito.when(mockSoapMessage.get("javax.xml.ws.wsdl.operation")).thenReturn(fakeOperation);

        CxfSoapUtils.nameTransaction(mockSoapMessage);
        Mockito.verify(mockTransaction, Mockito.timeout(1)).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH,
                true, CxfSoapUtils.TRANSACTION_CATEGORY, "/path/to/endpoint/aSoapOperation");

        Mockito.verifyNoMoreInteractions(mockTransaction);
    }

    @Test
    public void testAddCustomParams() {
        Exchange mockExchange = Mockito.mock(Exchange.class);
        Mockito.when(mockExchange.isSynchronous()).thenReturn(true);
        Mockito.when(mockExchange.isOneWay()).thenReturn(false);
        SoapVersion mockSoapVersion = Mockito.mock(SoapVersion.class);
        Mockito.when(mockSoapVersion.getVersion()).thenReturn(66.6);

        Mockito.when(mockSoapMessage.getExchange()).thenReturn(mockExchange);
        Mockito.when(mockSoapMessage.getVersion()).thenReturn(mockSoapVersion);
        Mockito.when(mockSoapMessage.get("Content-Type")).thenReturn("message-content");
        Mockito.when(mockSoapMessage.get("org.apache.cxf.message.Message.ENCODING")).thenReturn("UTF-X");

        CxfSoapUtils.addCustomAttributes(mockSoapMessage);
        Mockito.verify(mockPrivateApi, Mockito.times(1)).addCustomAttribute(CxfSoapUtils.PARAM_CATEGORY + "Content",
                "message-content");
        Mockito.verify(mockPrivateApi, Mockito.times(1)).addCustomAttribute(CxfSoapUtils.PARAM_CATEGORY + "Encoding",
                "UTF-X");
        Mockito.verify(mockPrivateApi, Mockito.times(1)).addCustomAttribute(CxfSoapUtils.PARAM_CATEGORY + "Type",
                "request-respond");
        Mockito.verify(mockPrivateApi, Mockito.times(1)).addCustomAttribute(
                CxfSoapUtils.PARAM_CATEGORY + "Synchronous?", "true");
        Mockito.verify(mockPrivateApi, Mockito.times(1)).addCustomAttribute(
                CxfSoapUtils.PARAM_CATEGORY + "SOAP Version", "66.6");
    }

}
