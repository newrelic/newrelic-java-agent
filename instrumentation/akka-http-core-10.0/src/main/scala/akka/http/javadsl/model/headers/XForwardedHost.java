/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.javadsl.model.headers;

import com.newrelic.api.agent.weaver.SkipIfPresent;
import com.newrelic.api.agent.weaver.Weaver;

// This class only exists to ensure that this weave module doesn't match for akka-http 10.0.11 or higher
@SkipIfPresent(originalName = "akka.http.javadsl.model.headers.XForwardedHost")
public class XForwardedHost {

    public static XForwardedHost create(akka.http.javadsl.model.Host host) {
        return Weaver.callOriginal();
    }

}
