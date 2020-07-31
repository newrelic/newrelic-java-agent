/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.application.PriorityApplicationName;
import com.newrelic.agent.service.Service;

import java.util.List;

public interface RPMServiceManager extends Service {

    void setConnectionConfigListener(ConnectionConfigListener listener);

    void addConnectionListener(ConnectionListener listener);

    void removeConnectionListener(ConnectionListener listener);

    /**
     * Get the default RPM service for the JVM.
     * 
     * @return the default RPM service
     */
    IRPMService getRPMService();

    /**
     * Get the RPM service for the given application name.
     * 
     * @param appName the name of the application the RPM service is reporting data for (must be interned)
     * @return the RPM service, or null if does not exist
     */
    IRPMService getRPMService(String appName);

    /**
     * Get the RPM service for the given application name, creating if it does not exist.
     * 
     * @param appName the name of the application the RPM service is reporting data for (must be interned)
     * @return the RPM service
     */
    IRPMService getOrCreateRPMService(String appName);

    /**
     * Get the RPM service for the given application name, creating if it does not exist.
     * 
     * @param appName the name of the application the RPM service is reporting data for (must be interned)
     * @return the RPM service
     */
    IRPMService getOrCreateRPMService(PriorityApplicationName appName);

    /**
     * Get all the RPM services.
     */
    List<IRPMService> getRPMServices();

}
