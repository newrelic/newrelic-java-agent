/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.WeaveIntoAllMethods;
import com.newrelic.api.agent.weaver.WeaveWithAnnotation;
import com.newrelic.api.agent.weaver.Weaver;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.invoke.MethodHandles;

import static com.nr.agent.instrumentation.SpringControllerUtility.processAnnotations;

@WeaveWithAnnotation(annotationClasses = {
        "org.springframework.stereotype.Controller",
        "org.springframework.web.bind.annotation.RestController" },
        type = MatchType.ExactClass)
public class SpringController_Instrumentation {

    @WeaveWithAnnotation(annotationClasses = {
            "org.springframework.web.bind.annotation.RequestMapping",
            "org.springframework.web.bind.annotation.PatchMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.DeleteMapping" })
    @WeaveIntoAllMethods
    @Trace
    private static void requestMapping() {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null) {
            RequestMapping rootPathMapping = Weaver.getClassAnnotation(RequestMapping.class);
            String rootPath = null;
            if (rootPathMapping != null) {
                rootPath = SpringControllerUtility.getPathValue(rootPathMapping.value(), rootPathMapping.path());
            }

            // the ordering of the following is important. RequestMapping overrides the new annotations. Then it goes:
            // PUT, DELETE, POST, GET
            if (Weaver.getMethodAnnotation(RequestMapping.class) != null) {
                RequestMapping methodPathMapping = Weaver.getMethodAnnotation(RequestMapping.class);
                String methodPath = SpringControllerUtility.getPathValue(methodPathMapping.value(),
                        methodPathMapping.path());
                processAnnotations(transaction, methodPathMapping.method(), rootPath, methodPath, MethodHandles.lookup().lookupClass());
            } else if (Weaver.getMethodAnnotation(PutMapping.class) != null) {
                PutMapping methodPathMapping = Weaver.getMethodAnnotation(PutMapping.class);
                String methodPath = SpringControllerUtility.getPathValue(methodPathMapping.value(),
                        methodPathMapping.path());
                processAnnotations(transaction, new RequestMethod[] { RequestMethod.PUT }, rootPath, methodPath, MethodHandles.lookup().lookupClass());
            } else if (Weaver.getMethodAnnotation(DeleteMapping.class) != null) {
                DeleteMapping methodPathMapping = Weaver.getMethodAnnotation(DeleteMapping.class);
                String methodPath = SpringControllerUtility.getPathValue(methodPathMapping.value(),
                        methodPathMapping.path());
                processAnnotations(transaction, new RequestMethod[] { RequestMethod.DELETE }, rootPath, methodPath, MethodHandles.lookup().lookupClass());
            } else if (Weaver.getMethodAnnotation(PostMapping.class) != null) {
                PostMapping methodPathMapping = Weaver.getMethodAnnotation(PostMapping.class);
                String methodPath = SpringControllerUtility.getPathValue(methodPathMapping.value(),
                        methodPathMapping.path());
                processAnnotations(transaction, new RequestMethod[] { RequestMethod.POST }, rootPath, methodPath, MethodHandles.lookup().lookupClass());
            } else if (Weaver.getMethodAnnotation(PatchMapping.class) != null) {
                PatchMapping methodPathMapping = Weaver.getMethodAnnotation(PatchMapping.class);
                String methodPath = SpringControllerUtility.getPathValue(methodPathMapping.value(),
                        methodPathMapping.path());
                processAnnotations(transaction, new RequestMethod[] { RequestMethod.PATCH }, rootPath, methodPath, MethodHandles.lookup().lookupClass());
            } else if (Weaver.getMethodAnnotation(GetMapping.class) != null) {
                GetMapping methodPathMapping = Weaver.getMethodAnnotation(GetMapping.class);
                String methodPath = SpringControllerUtility.getPathValue(methodPathMapping.value(),
                        methodPathMapping.path());
                processAnnotations(transaction, new RequestMethod[] { RequestMethod.GET }, rootPath, methodPath, MethodHandles.lookup().lookupClass());
            }
        }
    }

}
