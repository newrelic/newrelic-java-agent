/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.mongodb;

import com.mongodb.event.CommandListener;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongodb.NewRelicCommandListener;

import java.util.concurrent.atomic.AtomicBoolean;

@Weave(type = MatchType.ExactClass, originalName = "com/mongodb/MongoClientOptions")
public class MongoClientOptions_Instrumentation {

    @Weave(type = MatchType.ExactClass, originalName = "com/mongodb/MongoClientOptions$Builder")
    public abstract static class Builder {

        @NewField
        private final AtomicBoolean listenerAdded = new AtomicBoolean(false);

        public abstract Builder addCommandListener(final CommandListener commandListener);

        public MongoClientOptions_Instrumentation build() {
            if (listenerAdded.compareAndSet(false, true)) {
                addCommandListener(new NewRelicCommandListener());
            }
            return Weaver.callOriginal();
        }

    }
}
