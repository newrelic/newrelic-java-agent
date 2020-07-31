/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.codehaus.groovy.grails.web.servlet.mvc;

import org.springframework.web.servlet.ModelAndView;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface)
public abstract class GrailsControllerHelper {

    @Trace
    public ModelAndView handleURI(String uri, GrailsWebRequest webRequest) {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true,
                "GrailsController", webRequest.getControllerName(), "/", webRequest.getActionName());
        return Weaver.callOriginal();
    }
}
