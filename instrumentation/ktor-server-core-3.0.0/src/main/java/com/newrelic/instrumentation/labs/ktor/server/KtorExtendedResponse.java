/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.instrumentation.labs.ktor.server;

import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.HeaderType;

import io.ktor.http.HttpStatusCode;
import io.ktor.server.application.ApplicationCall;
import io.ktor.server.response.ApplicationResponse;
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
			ResponseHeaders headers = response.getHeaders();
			if(headers != null) {
				return headers.get("Content-Type");
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
			ResponseHeaders headers = response.getHeaders();
			if(headers != null) {
				String value = headers.get("Content-Length");
				if(value != null) {
					try {
						return Long.parseLong(value);
					} catch (NumberFormatException ignored) {
					}
				}
			}
		}
		return 0;
	}

}
