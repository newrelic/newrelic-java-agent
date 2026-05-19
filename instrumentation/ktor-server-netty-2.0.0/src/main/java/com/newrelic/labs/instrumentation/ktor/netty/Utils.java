/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.labs.instrumentation.ktor.netty;

import com.newrelic.instrumentation.labs.ktor.netty.CoroutineNameUtilsKt;
import io.ktor.http.HttpMethod;
import io.ktor.http.RequestConnectionPoint;
import io.ktor.server.application.Application;
import kotlin.coroutines.CoroutineContext;
import org.jetbrains.annotations.Nullable;

public class Utils {

	public static String getCoroutineName(CoroutineContext context) {
		@Nullable String name = CoroutineNameUtilsKt.getCoroutineName(context);
		if(name != null && !name.isEmpty()) return name;

		return null;
	}
	
	public static String getApplicationName(Application app) {
		if(app != null) {
			CoroutineContext ctx = app.getCoroutineContext();
			if(ctx != null) {
				return getCoroutineName(ctx);
			}
		}
		return null;
	}
	
	public static String getTransactionName(RequestConnectionPoint point) {
		StringBuffer sb = new StringBuffer();
		String uri = point.getUri();
		if(uri != null) {
			if(uri.startsWith("/")) uri = uri.substring(1);
			if(uri.isEmpty()) {
				sb.append("Root");
			} else {
				sb.append(uri);
			}
		}
		HttpMethod method = point.getMethod();
		if(method != null) {
			String value = method.getValue();
			if(value != null && !value.isEmpty()) {
				sb.append(" - {");
				sb.append(value);
				sb.append("}");
			}
		}
		return sb.toString();
	}

}
