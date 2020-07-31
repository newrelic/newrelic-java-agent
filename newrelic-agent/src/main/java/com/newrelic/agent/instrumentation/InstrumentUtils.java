/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class InstrumentUtils {

    /**
     * Private constructor since this is a utility class.
     */
    private InstrumentUtils() {
        super();
    }

    /**
     * Gets the uri as a string without any query parameters.
     * 
     * @param theUri The uri to convert.
     * @return The uri minus the query parameters.
     */
    public static String getURI(URI theUri) {
        if (theUri == null) {
            return "";
        }
        return getURI(theUri.getScheme(), theUri.getHost(), theUri.getPort(), theUri.getPath());
    }

    /**
     * Takes in a URL and returns the associated uri minus the query parameters.
     * 
     * @param theUrl The URL to be converted.
     * @return The converted URI.
     */
    public static String getURI(URL theUrl) {
        if (theUrl == null) {
            return "";
        }
        try {
            return getURI(theUrl.toURI());
        } catch (URISyntaxException e) {
            return getURI(theUrl.getProtocol(), theUrl.getHost(), theUrl.getPort(), theUrl.getPath());
        }
    }

    /**
     * Build the uri.
     * 
     * @return The uri.
     */
    public static String getURI(String scheme, String host, int port, String path) {
        StringBuilder sb = new StringBuilder();
        if (scheme != null) {
            sb.append(scheme);
            sb.append("://");
        }
        if (host != null) {
            sb.append(host);
            if (port >= 0) {
                sb.append(":");
                sb.append(port);
            }
        }
        if (path != null) {
            sb.append(path);
        }
        return sb.toString();
    }

    /**
     * Set a final value and reset the accessibility
     * 
     * @param context
     * @param field
     * @param newValue
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static void setFinal(Object context, Field field, Object newValue) throws SecurityException,
            NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        boolean wasAccessible = modifiersField.isAccessible();
        // int modifiers = field.getModifiers();
        modifiersField.setAccessible(true);
        // modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(context, newValue);

        // modifiersField.setInt(field, modifiers);
        modifiersField.setAccessible(wasAccessible);
    }

}
