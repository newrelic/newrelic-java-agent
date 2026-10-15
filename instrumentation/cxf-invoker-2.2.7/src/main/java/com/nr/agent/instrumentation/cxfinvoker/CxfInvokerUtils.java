/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.cxfinvoker;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.newrelic.api.agent.NewRelic;

public class CxfInvokerUtils {

    private static final Pattern SERVICE_CLASSNAME_NORMALIZATION_REGEX = Pattern.compile("\\$Proxy\\d+");
    private CxfInvokerUtils() {
    }

    /**
     * This is used to build the CXF transaction name if the endpoint info is null.
     * It also removes the number portion of the "$ProxyXXXX" segment from the class name
     * to prevent MGI. Ex:
     * com.sun.proxy.$Proxy243.Service/doSomething -> com.sun.proxy.$Proxy.Service/doSomething
     */
    public static String buildCXFTransactionName(String serviceClassName, String methodName) {
        return SERVICE_CLASSNAME_NORMALIZATION_REGEX.matcher(serviceClassName).replaceFirst("\\$Proxy") +
                "/" + methodName;
    }

    public static String getCXFRequestUri(String address, Method method) {
        try {
            address = new URI(address).getPath();
        } catch (URISyntaxException e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST,
                    "CxfInvokerUtils: could not parse CXF endpoint address [{0}] as a URI; " +
                            "using the endpoint address for transaction naming.", address);
        }
        StringBuilder path = new StringBuilder();
        if (!address.startsWith("/")) {
            path.append('/');
        }

        path.append(address);
        if (!address.endsWith("/")) {
            path.append('/');
        }

        path.append(method.getName());
        return path.toString();
    }
}
