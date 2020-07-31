/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.codehaus.groovy.grails.web.servlet.mvc;

import groovy.lang.GroovyObject;

import java.util.Map;

import org.springframework.web.servlet.ModelAndView;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.BaseClass)
public class AbstractGrailsControllerHelper {

    @Trace
    protected ModelAndView executeAction(GroovyObject controller, String actionName, String viewName,
            GrailsWebRequest webRequest, Map params) {
        AgentBridge.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true,
                "GrailsController", webRequest.getControllerName(), actionName);
        return Weaver.callOriginal();
    }

}
