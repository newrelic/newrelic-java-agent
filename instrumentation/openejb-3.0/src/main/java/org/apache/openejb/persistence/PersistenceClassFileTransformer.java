/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.openejb.persistence;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.security.ProtectionDomain;

@Weave(originalName = "org.apache.openejb.persistence.PersistenceUnitInfoImpl$PersistenceClassFileTransformer")
public class PersistenceClassFileTransformer {
    public byte[] transform(ClassLoader classLoader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className != null && className.startsWith("com/newrelic/")) {
            return null;
        }
        return Weaver.callOriginal();
    }
}
