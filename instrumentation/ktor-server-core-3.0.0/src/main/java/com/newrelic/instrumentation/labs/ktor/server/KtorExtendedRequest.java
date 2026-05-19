/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.instrumentation.labs.ktor.server;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;

import io.ktor.http.Headers;
import io.ktor.http.Parameters;
import io.ktor.http.RequestConnectionPoint;
import io.ktor.server.application.ApplicationCall;
import io.ktor.server.request.ApplicationRequest;
import io.ktor.server.request.RequestCookies;
import io.ktor.util.AttributeKey;
import io.ktor.util.Attributes;

public class KtorExtendedRequest extends ExtendedRequest {
	
	public ApplicationCall call = null;
	
	public KtorExtendedRequest(ApplicationCall c) {
		call = c;
	}

	@Override
	public String getRequestURI() {
		ApplicationRequest appRequest = call.getRequest();
		if(appRequest != null) {
			return appRequest.getLocal().getUri();
		}
		return null;
	}

	@Override
	public String getRemoteUser() {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getParameterNames() {
		ApplicationRequest appRequest = call.getRequest();
		if(appRequest != null) {
			Set<String> names = appRequest.getQueryParameters().names();
			return Collections.enumeration(names);
		}
		
		return Collections.emptyEnumeration();
	}

	@Override
	public String[] getParameterValues(String name) {
		ApplicationRequest appRequest = call.getRequest();
		if(appRequest != null) {
			Parameters params = appRequest.getQueryParameters();
			List<String> values = params.getAll(name);
			String[] valuesArray = new String[values.size()];
			values.toArray(valuesArray);
			return valuesArray;
		}
		return new String[0];
	}

	@Override
	public Object getAttribute(String name) {
		Attributes attributes = call.getAttributes();
		if(attributes != null) {
			AttributeKey<?> key = new AttributeKey<>(name);
			return attributes.get(key);
		}
		return null;
	}

	@Override
	public String getCookieValue(String name) {
		ApplicationRequest appRequest = call.getRequest();
		if(appRequest != null) {
			RequestCookies cookies = appRequest.getCookies();
			Map<String, String> rawCookies = cookies.getRawCookies();
			return rawCookies.get(name);
		}
		return null;
	}

	@Override
	public HeaderType getHeaderType() {
		return HeaderType.HTTP;
	}

	@Override
	public String getHeader(String name) {
		ApplicationRequest appRequest = call.getRequest();
		if(appRequest != null) {
			Headers headers = appRequest.getHeaders();
			if(headers != null) {
				return headers.get(name);
			}
		}
		return null;
	}

	@Override
	public String getMethod() {
		ApplicationRequest appRequest = call.getRequest();
		if(appRequest != null) {
			RequestConnectionPoint local = appRequest.getLocal();
			if(local != null) {
				return local.getMethod().toString();
			}
		}
		return null;
	}

}
