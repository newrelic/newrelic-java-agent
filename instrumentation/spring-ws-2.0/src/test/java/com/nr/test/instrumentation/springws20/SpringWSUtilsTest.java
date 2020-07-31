/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.test.instrumentation.springws20;

import java.util.Iterator;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.apache.axiom.om.OMElement;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.axiom.AxiomSoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.PrivateApi;
import com.newrelic.agent.bridge.PublicApi;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import com.nr.instrumentation.springws20.SpringWSUtils;

public class SpringWSUtilsTest {
    @Mock
    PrivateApi mockPrivateApi;
    @Mock
    PublicApi mockPublicApi;

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
    public void testSaajSoapMessageNameTransaction() throws SOAPException {
        SOAPElement mockSOAPElement = Mockito.mock(SOAPElement.class);
        Iterator<SOAPElement> mockSOAPElementIterator = Mockito.mock(Iterator.class);
        SOAPBody mockSOAPBody = Mockito.mock(SOAPBody.class);
        SOAPEnvelope mockSOAPEnvelope = Mockito.mock(SOAPEnvelope.class);
        SOAPPart mockSOAPPart = Mockito.mock(SOAPPart.class);
        SOAPMessage mockSOAPMessage = Mockito.mock(SOAPMessage.class);
        SaajSoapMessage mockSaajSoapMessage = Mockito.mock(SaajSoapMessage.class);

        Mockito.when(mockSOAPElement.getLocalName()).thenReturn("TestSoapMethod");
        Mockito.when(mockSOAPElement.getNamespaceURI()).thenReturn("http://nrspringws.com/");
        Mockito.when(mockSOAPElementIterator.hasNext()).thenReturn(true, false);
        Mockito.when(mockSOAPElementIterator.next()).thenReturn(mockSOAPElement);
        Mockito.when(mockSOAPBody.getChildElements()).thenReturn(mockSOAPElementIterator);
        Mockito.when(mockSOAPEnvelope.getBody()).thenReturn(mockSOAPBody);
        Mockito.when(mockSOAPPart.getEnvelope()).thenReturn(mockSOAPEnvelope);
        Mockito.when(mockSOAPMessage.getSOAPPart()).thenReturn(mockSOAPPart);
        Mockito.when(mockSaajSoapMessage.getSaajMessage()).thenReturn(mockSOAPMessage);

        SpringWSUtils.nameTransaction(mockSaajSoapMessage);
        Mockito.verify(mockTransaction, Mockito.timeout(1)).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH,
                true, SpringWSUtils.TRANSACTION_CATEGORY, "{http://nrspringws.com/}TestSoapMethod");
        Mockito.verifyNoMoreInteractions(mockTransaction);
    }

    @Test
    public void testAxiomSoapMessageNameTransaction() throws SOAPException {
        OMElement mockOMElement = Mockito.mock(OMElement.class);
        org.apache.axiom.soap.SOAPBody mockSOAPBody = Mockito.mock(org.apache.axiom.soap.SOAPBody.class);
        org.apache.axiom.soap.SOAPEnvelope mockSOAPEnvelope = Mockito.mock(org.apache.axiom.soap.SOAPEnvelope.class);
        org.apache.axiom.soap.SOAPMessage mockSOAPMessage = Mockito.mock(org.apache.axiom.soap.SOAPMessage.class);
        AxiomSoapMessage axiomSoapMessage = Mockito.mock(AxiomSoapMessage.class);

        Mockito.when(mockOMElement.getLocalName()).thenReturn("TestSoapMethod");
        Mockito.when(mockOMElement.getNamespaceURI()).thenReturn("http://nrspringws.com/");
        Mockito.when(mockSOAPBody.getFirstElement()).thenReturn(mockOMElement);
        Mockito.when(mockSOAPEnvelope.getBody()).thenReturn(mockSOAPBody);
        Mockito.when(mockSOAPMessage.getSOAPEnvelope()).thenReturn(mockSOAPEnvelope);
        Mockito.when(axiomSoapMessage.getAxiomMessage()).thenReturn(mockSOAPMessage);

        SpringWSUtils.nameTransaction(axiomSoapMessage);
        Mockito.verify(mockTransaction, Mockito.timeout(1)).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH,
                true, SpringWSUtils.TRANSACTION_CATEGORY, "{http://nrspringws.com/}TestSoapMethod");
        Mockito.verifyNoMoreInteractions(mockTransaction);
    }

    @Test
    public void testAddCustomAttributes() {
        SoapVersion mockSoapVersion = Mockito.mock(SoapVersion.class);
        SoapMessage mockSoapMessage = Mockito.mock(SoapMessage.class);
        Mockito.when(mockSoapVersion.getContentType()).thenReturn("application/soap+xml");
        Mockito.when(mockSoapVersion.toString()).thenReturn("1.2");
        Mockito.when(mockSoapMessage.getSoapAction()).thenReturn("MySoapAction");
        Mockito.when(mockSoapMessage.getVersion()).thenReturn(mockSoapVersion);

        SpringWSUtils.addCustomAttributes(mockSoapMessage);
        Mockito.verify(mockPrivateApi, Mockito.times(1)).addCustomAttribute(SpringWSUtils.PARAM_CATEGORY + "Content",
                "application/soap+xml");
        Mockito.verify(mockPrivateApi, Mockito.times(1)).addCustomAttribute(
                SpringWSUtils.PARAM_CATEGORY + "SOAP Version", "1.2");

        Mockito.verify(mockPrivateApi, Mockito.times(1)).addCustomAttribute(
                SpringWSUtils.PARAM_CATEGORY + "SOAP Action", "MySoapAction");
    }
}
