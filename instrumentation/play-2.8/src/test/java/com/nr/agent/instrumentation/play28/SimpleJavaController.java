/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.play28;

import javax.inject.Inject;

import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

@With(SimpleJavaAction.class)
public class SimpleJavaController extends Controller {
    private HttpExecutionContext ec;

    @Inject
    public SimpleJavaController(HttpExecutionContext ec) {
        this.ec = ec;
    }

    public Result hello() {
        return ok("hello world").as("text/plain");
    }

    public Result index() {
        return ok("Ahoy ahoy").as("text/plain");
    }

    public Result simple() {
        return ok("Simple test").as("text/plain");
    }

}
