/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.flywaydb.core.internal.callback;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.FlywayUtils;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.Event;

import java.util.logging.Level;

@Weave(type = MatchType.ExactClass, originalName = "org.flywaydb.core.internal.callback.DefaultCallbackExecutor")
public class DefaultCallbackExecutor_Instrumentation {

    private MigrationInfo migrationInfo;

    public void onEachMigrateOrUndoEvent(Event event) {
        if (FlywayUtils.isTargetEvent(event)) {
            if (NewRelic.getAgent().getLogger().isLoggable(Level.FINEST)) {
                NewRelic.getAgent().getLogger().log(Level.FINEST, "Adding custom Flyway migration event: {0}", event);
            }
            FlywayUtils.submitFlywayEvent(event, migrationInfo);
        }

        Weaver.callOriginal();
    }

    public void setMigrationInfo(MigrationInfo migrationInfo) {
        this.migrationInfo = migrationInfo;
    }
}
