/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.cxf27;

import java.net.URI;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.model.MessageInfo;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.TransactionNamePriority;

public class CxfSoapUtils {
    public static final String PARAM_CATEGORY = "library.CXF.soap.";
    public static final String TRANSACTION_CATEGORY = "SOAP";

    /**
     * Notice soap faults
     * 
     * @param message
     */
    public static void handleFault(SoapMessage message) {
        Fault fault = (Fault) message.getContent(Exception.class);
        AgentBridge.privateApi.reportException(fault);
    }

    /**
     * Name the soap transaction after its endpoint+operation
     * 
     * @param soapMessage
     */
    public static void nameTransaction(SoapMessage soapMessage) {
        URI serviceURI = (URI) soapMessage.get("javax.xml.ws.wsdl.description");
        if (null != serviceURI) {
            String servicePath = serviceURI.getPath();
            String operationName = ((QName) soapMessage.get("javax.xml.ws.wsdl.operation")).getLocalPart();
            String transactionName = servicePath + "/" + operationName;

            // Example Tx Name: "WebTransaction/SOAP/path/to/endpoint/operationName"
            AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true,
                    TRANSACTION_CATEGORY, transactionName);
        }
    }

    /**
     * Custom Parameters to attach to faults or transactions
     * 
     * @param soapMessage
     */
    public static void addCustomAttributes(SoapMessage soapMessage) {
        SoapVersion soapVersion = soapMessage.getVersion();
        Exchange exchange = soapMessage.getExchange();
        if (null != soapMessage.get("org.apache.cxf.service.model.MessageInfo")) {
            String typeName = ((MessageInfo) soapMessage.get("org.apache.cxf.service.model.MessageInfo")).getType().name();
            AgentBridge.privateApi.addCustomAttribute(PARAM_CATEGORY + "Type", typeName);
            String messageName = ((MessageInfo) soapMessage.get("org.apache.cxf.service.model.MessageInfo")).getName().getLocalPart();
            AgentBridge.privateApi.addCustomAttribute(PARAM_CATEGORY + "Message Name", messageName);
        }
        if (null != soapMessage.get("javax.xml.ws.wsdl.description")) {
            URI wsdlUri = (URI) soapMessage.get("javax.xml.ws.wsdl.description");
            AgentBridge.privateApi.addCustomAttribute(PARAM_CATEGORY + "WSDL", wsdlUri.getPath() + "?"
                    + wsdlUri.getQuery());
        }
        AgentBridge.privateApi.addCustomAttribute(PARAM_CATEGORY + "Content", soapMessage.get("Content-Type") + "");
        AgentBridge.privateApi.addCustomAttribute(PARAM_CATEGORY + "SOAP Version", soapVersion.getVersion() + "");
        AgentBridge.privateApi.addCustomAttribute(PARAM_CATEGORY + "Encoding",
                soapMessage.get("org.apache.cxf.message.Message.ENCODING") + "");
        AgentBridge.privateApi.addCustomAttribute(PARAM_CATEGORY + "Synchronous?", exchange.isSynchronous() + "");
        AgentBridge.privateApi.addCustomAttribute(PARAM_CATEGORY + "Type", exchange.isOneWay() ? "one-way"
                : "request-respond");
    }
}
