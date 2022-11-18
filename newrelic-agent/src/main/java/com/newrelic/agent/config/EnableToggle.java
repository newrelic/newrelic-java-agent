package com.newrelic.agent.config;

/**
 * Configuration specific interface that allows the enabled flag for the target configuration
 * to be toggled on and off at runtime.
 * <p>
 * Usually used in conjunction with services or components that adhere to the
 * {@link com.newrelic.agent.service.Toggleable Toggleable} interface.
 */
public interface EnableToggle {
    /**
     * Set the enabled status of the service.
     *
     * @param isEnabled <code>true</code> to set the service flag to enabled, else <code>false</code>.
     */
    void setEnabled(boolean isEnabled);

    /**
     * Check if the service is enabled.
     *
     * @return <code>true</code> if the service is enabled, else <code>false</code>.
     */
    boolean isEnabled();
}
