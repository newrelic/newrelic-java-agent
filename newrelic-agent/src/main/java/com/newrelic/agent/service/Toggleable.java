package com.newrelic.agent.service;

/**
 * Interface that can be implemented by services that support being toggled on or off dynamically.
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
}
