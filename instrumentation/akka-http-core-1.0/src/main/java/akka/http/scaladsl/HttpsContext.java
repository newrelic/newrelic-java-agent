/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl;

import com.newrelic.api.agent.weaver.Weave;

// The only reason this class exists is to prevent this module from matching on later versions of
// akka-http-core, causing multiple modules to apply to the same framework version.  It has no other purpose.
@Weave
public abstract class HttpsContext {

}
