/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores the response information for a reinstrumentation request.
 */
public class ReinstrumentResult {

    protected static final String ERROR_KEY = "errors";
    protected static final String PCS_SPECIFIED_KEY = "pointcuts_specified";
    protected static final String RETRANSFORM_INIT_KEY = "retransform_init";

    /**
     * List of any errors which occurred when parsing the xml and working to retransform the code.
     */
    private final List<String> errorMessages = new ArrayList<>();

    /**
     * The number of point cuts parsed out of the xml.
     */
    private int pointCutsSpecified = 0;

    /**
     * The classes which were sent for retransformation.
     */
    private Set<String> retranformedInitializedClasses = new HashSet<>();

    public ReinstrumentResult() {
        super();
    }

    public Map<String, Object> getStatusMap() {
        Map<String, Object> statusMap = new HashMap<>();
        if (errorMessages.size() > 0) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> it = errorMessages.iterator();
            while (it.hasNext()) {
                sb.append(it.next());
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            statusMap.put(ERROR_KEY, sb.toString());
        }
        statusMap.put(PCS_SPECIFIED_KEY, pointCutsSpecified);

        if (retranformedInitializedClasses.size() > 0) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> it = retranformedInitializedClasses.iterator();
            while (it.hasNext()) {
                sb.append(it.next());
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            statusMap.put(RETRANSFORM_INIT_KEY, sb.toString());
        }
        return statusMap;
    }

    /**
     * Sets the field errorMessages.
     *
     * @param pErrorMessages the errorMessages to set
     */
    public void addErrorMessage(String pErrorMessages) {
        errorMessages.add(pErrorMessages);
    }

    /**
     * Sets the field pointCutsSpecified.
     *
     * @param pPointCutsSpecified the pointCutsSpecified to set
     */
    public void setPointCutsSpecified(int pPointCutsSpecified) {
        pointCutsSpecified = pPointCutsSpecified;
    }

    /**
     * Sets the field retranformedInitializedClasses.
     *
     * @param pRetranformedInitializedClasses the retranformedInitializedClasses to set
     */
    public void setRetranformedInitializedClasses(Set<String> pRetranformedInitializedClasses) {
        retranformedInitializedClasses = pRetranformedInitializedClasses;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(PCS_SPECIFIED_KEY);
        sb.append(":");
        sb.append(pointCutsSpecified);
        sb.append(", ");
        if ((errorMessages != null) && (errorMessages.size() > 0)) {
            sb.append(",");
            sb.append(ERROR_KEY);
            sb.append(":[");
            for (String msg : errorMessages) {
                sb.append(" ");
                sb.append(msg);
            }
            sb.append("]");
        }
        if (retranformedInitializedClasses != null && retranformedInitializedClasses.size() > 0) {
            sb.append(", ");
            sb.append(RETRANSFORM_INIT_KEY);
            sb.append(":[");
            for (String msg : retranformedInitializedClasses) {
                sb.append(" ");
                sb.append(msg);
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
