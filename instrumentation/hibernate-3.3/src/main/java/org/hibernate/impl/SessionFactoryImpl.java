/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.hibernate.impl;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Settings;
import org.hibernate.engine.Mapping;
import org.hibernate.event.EventListeners;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.weave.instrumentation.hibernate33.StatisticsSampler;

@Weave
public abstract class SessionFactoryImpl implements SessionFactory {

    public SessionFactoryImpl(Configuration cfg, Mapping mapping, Settings settings, EventListeners listeners,
            SessionFactoryObserver observer) {
        if (NewRelic.getAgent().getConfig().getValue("instrumentation.hibernate.stats_sampler.enabled", false)) {
            new StatisticsSampler(this);
        }
    }

}
