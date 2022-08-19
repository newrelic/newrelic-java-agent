/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.webservices.javax;

import javax.xml.transform.Source;
import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceProvider;

@WebServiceProvider(portName = "SimpleClientPort", serviceName = "SimpleClientService",
        targetNamespace = "http://webservices.instrumentation.agent.newrelic.com/",
        wsdlLocation = "com/newrelic/agent/instrumentation/webservices/javax/SimpleClientService.wsdl")
public class SimpleClientServiceImpl implements Provider<Source> {

    @Override
    public Source invoke(Source data) {
        return data;
    }
}
