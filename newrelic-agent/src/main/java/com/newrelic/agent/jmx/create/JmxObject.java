/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.create;

import java.text.MessageFormat;
import java.util.logging.Level;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.newrelic.agent.Agent;

/**
 * Base class for jmx objects.
 * 
 * @since Mar 13, 2013
 */
public class JmxObject {

    /**
     * The object name for the jmx metric.
     */
    private final String objectNameString;

    /** This is the object name to be used. */
    private final ObjectName objectName;

    /**
     * 
     * Creates this JmxObject.
     * 
     * @param pObjectName The object name.
     * @param safeName The safeName for the metric.
     * @throws MalformedObjectNameException Thrown if a problem with the object name.
     */
    public JmxObject(String pObjectName, String safeName) throws MalformedObjectNameException {
        super();
        objectNameString = pObjectName;
        objectName = setObjectName(safeName);
    }

    private ObjectName setObjectName(String safeName) throws MalformedObjectNameException {
        try {

            return new ObjectName(safeName);
        } catch (MalformedObjectNameException e) {
            if (!objectNameString.equals(safeName)) {
                safeName = safeName + '(' + objectNameString + ')';
            }
            if (Agent.LOG.isFineEnabled()) {
                Agent.LOG.severe(MessageFormat.format("Skipping bad Jmx object name : {0}.  {1}", safeName,
                        e.toString()));
                Agent.LOG.log(Level.FINER, "Jmx config error", e);
            }
            throw e;
        }
    }

    /**
     * String representation of this JmxObject.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("object_name: ").append(objectNameString);
        return sb.toString();
    }

    /**
     * Gets the field objectNameString.
     * 
     * @return the objectNameString
     */
    public String getObjectNameString() {
        return objectNameString;
    }

    /**
     * Gets the field objectName.
     * 
     * @return the objectName
     */
    public ObjectName getObjectName() {
        return objectName;
    }

}
