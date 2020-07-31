/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.springws20;

import java.util.Iterator;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPProcessingException;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.axiom.AxiomSoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.TransactionNamePriority;

public class SpringWSUtils {
    public static final String TRANSACTION_CATEGORY = "SOAP";
    public static final String PARAM_CATEGORY = "library.SpringWS.soap.";

    public static void nameTransaction(SaajSoapMessage message) {
        SOAPElement bodyElement = getSOAPBodyElement(message);
        if (bodyElement != null) {
            String method = bodyElement.getLocalName();
            String targetNameSpace = bodyElement.getNamespaceURI();
            String transactionName = "{" + targetNameSpace + "}" + method;
            AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true,
                    TRANSACTION_CATEGORY, transactionName);
        }
    }

    public static void nameTransaction(AxiomSoapMessage message) {
        OMElement bodyElement = getOMEBodyElement(message);
        if (bodyElement != null) {
            String method = bodyElement.getLocalName();
            String targetNameSpace = bodyElement.getNamespaceURI();
            String transactionName = "{" + targetNameSpace + "}" + method;
            AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true,
                    TRANSACTION_CATEGORY, transactionName);
        }
    }

    public static SOAPElement getSOAPBodyElement(SaajSoapMessage message) {
        try {
            SOAPMessage saajMessage = message.getSaajMessage();
            SOAPEnvelope envelope = saajMessage.getSOAPPart().getEnvelope();
            if (envelope != null) {
                SOAPBody body = envelope.getBody();
                if (body != null) {
                    SOAPElement bodyElement = getFirstBodyElement(body);
                    return bodyElement;

                }
            }
        } catch (SOAPException e) {
        }
        return null;
    }

    public static OMElement getOMEBodyElement(AxiomSoapMessage message) {
        try {
            org.apache.axiom.soap.SOAPMessage axiomMessage = message.getAxiomMessage();
            org.apache.axiom.soap.SOAPEnvelope envelope = axiomMessage.getSOAPEnvelope();
            if (envelope != null) {
                org.apache.axiom.soap.SOAPBody body = envelope.getBody();
                if (body != null) {
                    OMElement bodyElement = body.getFirstElement();
                    return bodyElement;
                }
            }
        } catch (SOAPProcessingException e) {
        }
        return null;
    }

    public static SOAPElement getFirstBodyElement(SOAPBody body) {
        for (Iterator<?> iterator = body.getChildElements(); iterator.hasNext();) {
            Object child = iterator.next();
            if (child instanceof SOAPElement) {
                return (SOAPElement) child;
            }
        }
        return null;
    }

    public static void addCustomAttributes(SoapMessage message) {
        SoapVersion version = message.getVersion();
        String contentType = version.getContentType();
        String soapAction = message.getSoapAction();
        AgentBridge.privateApi.addCustomAttribute(PARAM_CATEGORY + "SOAP Version", version.toString());
        AgentBridge.privateApi.addCustomAttribute(PARAM_CATEGORY + "Content", contentType);
        AgentBridge.privateApi.addCustomAttribute(PARAM_CATEGORY + "SOAP Action", soapAction);
    }
}
