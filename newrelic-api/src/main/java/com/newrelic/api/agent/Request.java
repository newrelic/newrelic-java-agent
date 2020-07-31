/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import java.util.Enumeration;

/**
 * Represents a web request.
 * 
 * The implementation of this interface does not need to be servlet specific, but the API is based on the servlet spec.
 *
 * @see NewRelic#setRequestAndResponse(Request, Response)
 * @deprecated since 3.21.0; use {@link com.newrelic.api.agent.ExtendedRequest} instead.
 */
public interface Request extends InboundHeaders {
    /**
     * Returns the part of this request's URL from the protocol name up to the query string in the first line of the
     * HTTP request.
     * 
     * @return Request URL from the protocol name to query string.
     * @since 2.21.0
     */
    String getRequestURI();

    /**
     * Returns the login of the user making this request, if the user has been authenticated, or <code>null</code> if
     * the user has not been authenticated.
     * 
     * @return Login of the user making this request.
     * @since 2.21.0
     */
    String getRemoteUser();

    /**
     * Returns an <code>Enumeration</code> of <code>String</code> objects containing the names of the parameters
     * contained in this request. If the request has no parameters, the method returns an empty <code>Enumeration</code>
     * or <code>null</code>.
     * 
     * @return An <code>Enumeration</code> of <code>String</code> objects containing the names of the parameters
     *         contained in this request.
     * @since 2.21.0
     */
    @SuppressWarnings("rawtypes")
    Enumeration getParameterNames();

    /**
     * Returns an array of <code>String</code> objects containing all of the values the given request parameter has, or
     * <code>null</code> if the parameter does not exist. If the parameter has a single value, the array has a length of
     * 1.
     * 
     * @param name The name of the attribute.
     * @return All values of the given input request parameter, or <code>null</code> if the input name does not exist.
     * @since 2.21.0
     */
    String[] getParameterValues(String name);

    /**
     * Returns the value of the named attribute as an <code>Object</code>, or <code>null</code> if no attribute of the
     * given name exists.
     * 
     * @param name The name of the attribute to return.
     * @return Value of the named input attribute, or <code>null</code> if no attribute with the given input name
     *         exists.
     * @since 2.21.0
     */
    Object getAttribute(String name);

    /**
     * Returns the value for the cookie with the given name, or <code>null</code> if the cookie does not exist.
     * 
     * @param name The name of the cookie
     * @return The value of the cookie or <code>null</code> if the cookie does not exist.
     * @since 3.1.0
     */
    String getCookieValue(String name);
}
