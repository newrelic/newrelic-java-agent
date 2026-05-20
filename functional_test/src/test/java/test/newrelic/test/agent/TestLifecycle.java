/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseListener;
import javax.faces.lifecycle.Lifecycle;

/**
 * Test class to exercise LifecyclePointCut for instrumentation testing.
 */
public class TestLifecycle extends Lifecycle {
    @Override
    public void addPhaseListener(PhaseListener phaseListener) {
    }

    @Override
    public void execute(FacesContext facesContext) throws FacesException {
    }

    @Override
    public PhaseListener[] getPhaseListeners() {
        return new PhaseListener[0];
    }

    @Override
    public void removePhaseListener(PhaseListener phaseListener) {
    }

    @Override
    public void render(FacesContext facesContext) throws FacesException {
    }
}