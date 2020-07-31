/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.openjpa.enhance;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

@Weave(originalName = "org.apache.openjpa.enhance.PCClassFileTransformer")
public class PCClassFileTransformer_Weaved {
    public byte[] transform(ClassLoader loader, String className, Class redef, ProtectionDomain domain, byte[] bytes)
            throws IllegalClassFormatException {
        if (className != null && className.startsWith("com/newrelic/")) {
            return null;
        }
        return Weaver.callOriginal();
    }

}
