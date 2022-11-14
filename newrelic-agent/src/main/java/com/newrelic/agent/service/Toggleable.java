package com.newrelic.agent.service;

/**
 * Interface that can be implemented by services that support being toggled on or off dynamically.
 * A service that implements this interface should follow these rules:
 * <ul>
 *     <li>If the service is enabled via configuration, it should not start actively
 *     running/processing until it receives the proper start command from the NR server</li>
 *     <li>If the service is disabled via configuration, it should be toggleable at all and will
 *     ignore all start/stop commands</li>
 * </ul>
 */
public interface Toggleable {

    /**
     * Toggle the service on.
     */
    void toggleOn();

    /**
     * Toggle the service off
     */
    void toggleOff();

    /**
     *
     * @return <code>true</code> if the service is running/processing, else <code>false</code>
     */
    boolean isRunning();
}
