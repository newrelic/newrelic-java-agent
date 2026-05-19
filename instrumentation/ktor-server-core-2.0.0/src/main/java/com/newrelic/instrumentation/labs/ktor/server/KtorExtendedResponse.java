/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.instrumentation.labs.ktor.server;

import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.HeaderType;

import io.ktor.http.Cookie;
import io.ktor.http.HttpStatusCode;
import io.ktor.server.application.ApplicationCall;
import io.ktor.server.response.ApplicationResponse;
import io.ktor.server.response.ResponseCookies;
import io.ktor.server.response.ResponseHeaders;

public class KtorExtendedResponse extends ExtendedResponse {
	
	private ApplicationCall call = null;
	
	public KtorExtendedResponse(ApplicationCall c) {
		call = c;
	}

	@Override
	public int getStatus() throws Exception {
		ApplicationResponse response = call.getResponse();
		if(response != null) {
			HttpStatusCode status = response.status();
			if(status != null) {
				return status.getValue();
			}
		}
		return 0;
	}

	@Override
	public String getStatusMessage() throws Exception {
		ApplicationResponse response = call.getResponse();
		if(response != null) {
			HttpStatusCode status = response.status();
			if(status != null) {
				return status.getDescription();
			}
		}
		return null;
	}

	@Override
	public String getContentType() {
		ApplicationResponse response = call.getResponse();
		if(response != null) {
			ResponseCookies cookies = response.getCookies();
			if(cookies != null) {
				Cookie cookie = cookies.get("Content-Length");
				if(cookie != null) {
					String value = cookie.getValue();
					if(value != null) {
						return value;
					}
				}
			}
		}
		return null;
	}

	@Override
	public HeaderType getHeaderType() {
		return HeaderType.HTTP;
	}

	@Override
	public void setHeader(String name, String value) {
		ApplicationResponse response = call.getResponse();
		if(response != null) {
			ResponseHeaders headers = response.getHeaders();
			headers.append(name, value, true);
		}

	}

	@Override
	public long getContentLength() {
		ApplicationResponse response = call.getResponse();
		if(response != null) {
			ResponseCookies cookies = response.getCookies();
			if(cookies != null) {
				Cookie cookie = cookies.get("Content-Length");
				if(cookie != null) {
					String value = cookie.getValue();
					if(value != null) {
						try {
							int length = Integer.parseInt(value);
							return length;
						} catch (NumberFormatException ignored) {
						}
					}
				}
			}
		}
		return 0;
	}

}
