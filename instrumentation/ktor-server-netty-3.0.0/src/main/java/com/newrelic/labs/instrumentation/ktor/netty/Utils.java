/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.labs.instrumentation.ktor.netty;

import com.newrelic.instrumentation.labs.ktor.netty.CoroutineNameUtilsKt;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import kotlin.coroutines.CoroutineContext;
import org.jetbrains.annotations.Nullable;

public class Utils {

	public static String getCoroutineName(CoroutineContext context) {
		@Nullable String name = CoroutineNameUtilsKt.getCoroutineName(context);
		if(name != null && !name.isEmpty()) return name;

		return null;
	}
	
	public static String getTransactionName(HttpRequest point) {
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
			String value = method.name();
			if(value != null && !value.isEmpty()) {
				sb.append(" - {");
				sb.append(value);
				sb.append("}");
			}
		}
		return sb.toString();
	}

}
