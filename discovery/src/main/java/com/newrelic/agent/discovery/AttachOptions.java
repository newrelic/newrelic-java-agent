package com.newrelic.agent.discovery;


public interface AttachOptions {
    /**
     * True if only the information about running java processes should be printed.
     */
    boolean isList();

    /**
     * The pid of the java process to attach to, or null or all processes.
     */
    String getPid();

    /**
     * The app name to use if a pid was specified.  May be null.
     */
    String getAppName();

    /**
     * Returns the account license key or null if none was specified using -license.
     */
    String getLicenseKey();

    /**
     * Returns a json serializer.
     */
    JsonSerializer getSerializer();

    /**
     * Returns true if output should be printed as json.
     */
    boolean isJsonFormat();
}
