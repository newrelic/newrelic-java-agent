/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spy.memcached;

import com.newrelic.api.agent.Segment;
import net.spy.memcached.internal.OperationFuture;

public class OperationCompletionListener implements net.spy.memcached.internal.OperationCompletionListener {


	private Segment segment;

	public OperationCompletionListener(Segment segment) {
		this.segment = segment;
	}

	@Override
	public void onComplete(OperationFuture<?> future) throws Exception {
		segment.endAsync();
	}

}
