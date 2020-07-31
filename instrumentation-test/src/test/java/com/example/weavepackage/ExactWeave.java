/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.example.weavepackage;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "com.example.weavepackage.ExactOriginal")
public class ExactWeave {
    @NewField
    private String weaved = "weaved ";

    public String exactMethod() {
        return weaved + Weaver.callOriginal();
    }
}
