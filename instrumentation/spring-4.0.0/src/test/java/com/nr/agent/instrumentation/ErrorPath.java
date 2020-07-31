/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class ErrorPath {

    @RequestMapping(value = "/errorPath", method = RequestMethod.GET)
    public Object testError() {
        System.out.printf("throwing exception");
        throw new RuntimeException("test");
    }

    @RequestMapping(value = "/error.pathBad")
    @ExceptionHandler(RuntimeException.class)
    public void conflict() {
    }
}
