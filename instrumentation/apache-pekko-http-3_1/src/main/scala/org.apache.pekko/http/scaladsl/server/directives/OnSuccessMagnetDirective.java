/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.http.scaladsl.server.directives;

import org.apache.pekko.http.scaladsl.server.util.Tupler;
import com.agent.instrumentation.org.apache.pekko.http.Function0Wrapper;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Function0;
import scala.concurrent.Future;

@Weave(type = MatchType.BaseClass, originalName = "org.apache.pekko.http.scaladsl.server.directives.OnSuccessMagnet$")
public class OnSuccessMagnetDirective {

    public <T> OnSuccessMagnet apply(Function0<Future<T>> f, Tupler<T> tupler) {
        f = new Function0Wrapper(f);
        return Weaver.callOriginal();
    }

}
