/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.ws.soap.axiom;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPMessage;
import org.springframework.ws.soap.SoapMessage;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.springws20.SpringWSUtils;

@Weave
public class AxiomSoapMessage {

    public AxiomSoapMessage(SOAPFactory soapFactory, boolean payloadCaching, boolean langAttributeOnSoap11FaultString) {
        if (payloadCaching) {
            // When payloadCaching is false, the message can only be read once,
            // since the contents of the SOAP body are directly read from the socket stream.
            SpringWSUtils.nameTransaction(this);
            SpringWSUtils.addCustomAttributes((SoapMessage) this);
        }
    }

    public AxiomSoapMessage(SOAPMessage soapMessage, Attachments attachments, String soapAction,
            boolean payloadCaching, boolean langAttributeOnSoap11FaultString) {
        if (payloadCaching) {
            // When payloadCaching is false, the message can only be read once,
            // since the contents of the SOAP body are directly read from the socket stream.
            SpringWSUtils.nameTransaction(this);
            SpringWSUtils.addCustomAttributes((SoapMessage) this);
        }
    }

    public SOAPMessage getAxiomMessage() {
        return Weaver.callOriginal();
    }

}
