/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.can;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.net.InetSocketAddress;

@Weave(originalName = "spray.can.Http")
public class Http_Instrumentation {

    @Weave(originalName = "spray.can.Http$Connect")
    public static class Connect {

        @NewField
        public Token token;

        public boolean sslEncryption() {
            return Weaver.callOriginal();
        }

        public InetSocketAddress remoteAddress() {
            return Weaver.callOriginal();
        }

    }

}
