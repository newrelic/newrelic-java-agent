/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spy.memcached;

import com.newrelic.api.agent.Segment;
import net.spy.memcached.internal.GetFuture;

public class GetCompletionListener implements net.spy.memcached.internal.GetCompletionListener {

	private Segment segment;

	public GetCompletionListener(Segment segment) {
		this.segment = segment;
	}

	@Override
	public void onComplete(GetFuture<?> future) throws Exception {
		segment.endAsync();
	}

}
