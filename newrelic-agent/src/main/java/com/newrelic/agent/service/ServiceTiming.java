/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

import com.newrelic.agent.logging.IAgentLogger;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

/**
 * A utility class for keeping track of Service initialization and
 * start times for debugging & performance optimization usage.
 */
public class ServiceTiming {

    private static final Comparator<ServiceNameAndTime> serviceNameComparator = Comparator.comparing(ServiceNameAndTime::getServiceName);

    private static final Map<ServiceNameAndType, Long> serviceTimings = new LinkedHashMap<>();
    private static final Set<ServiceNameAndTime> serviceInitializationTimings = new TreeSet<>(serviceNameComparator);
    private static final Set<ServiceNameAndTime> serviceStartTimings = new TreeSet<>(serviceNameComparator);
    private static volatile long endTimeInNanos = 0;

    public static void addServiceInitialization(String serviceName) {
        if (serviceName == null) {
            return;
        }
        serviceTimings.put(new ServiceNameAndType(serviceName, Type.initialization), System.nanoTime());
    }

    public static void addServiceStart(String serviceName) {
        if (serviceName == null) {
            return;
        }
        serviceTimings.put(new ServiceNameAndType(serviceName, Type.start), System.nanoTime());
    }

    public static void setEndTime() {
        endTimeInNanos = System.nanoTime();
    }

    public static void logServiceTimings(IAgentLogger logger) {
        boolean startupTimingEnabled = ServiceFactory.getConfigService().getDefaultAgentConfig().isStartupTimingEnabled();
        if (!startupTimingEnabled || logger == null || endTimeInNanos == 0) {
            serviceTimings.clear();
            return;
        }

        ServiceNameAndType previousServiceNameAndType = null;
        Long previousServiceTime = null;
        for (Map.Entry<ServiceNameAndType, Long> entry : serviceTimings.entrySet()) {
            // First time around, set the "previous" values
            if (previousServiceNameAndType == null) {
                previousServiceNameAndType = entry.getKey();
                previousServiceTime = entry.getValue();
                continue;
            }

            // Record the timing for the service prior to this one
            long serviceTime = entry.getValue() - previousServiceTime;
            if (previousServiceNameAndType.type == Type.initialization) {
                serviceInitializationTimings.add(new ServiceNameAndTime(previousServiceNameAndType.serviceName, serviceTime));
            } else {
                serviceStartTimings.add(new ServiceNameAndTime(previousServiceNameAndType.serviceName, serviceTime));
            }

            previousServiceNameAndType = entry.getKey();
            previousServiceTime = entry.getValue();
        }

        if (previousServiceNameAndType != null && previousServiceTime != null) {
            // Record the final value
            long serviceTime = endTimeInNanos - previousServiceTime;
            if (previousServiceNameAndType.type == Type.initialization) {
                serviceInitializationTimings.add(new ServiceNameAndTime(previousServiceNameAndType.serviceName, serviceTime));
            } else {
                serviceStartTimings.add(new ServiceNameAndTime(previousServiceNameAndType.serviceName, serviceTime));
            }
        }

        for (ServiceNameAndTime entry : serviceInitializationTimings) {
            logger.log(Level.FINEST, "Service Initialization Timing: {0}:{1}ns", entry.serviceName, entry.time);
        }
        for (ServiceNameAndTime entry : serviceStartTimings) {
            logger.log(Level.FINEST, "Service Start Timing: {0}:{1}ns", entry.serviceName, entry.time);
        }

        // No need to hold on to these values anymore
        serviceTimings.clear();
    }

    // For testing
    public static Set<ServiceNameAndTime> getServiceInitializationTimings() {
        return serviceInitializationTimings;
    }

    // For testing
    public static Set<ServiceNameAndTime> getServiceStartTimings() {
        return serviceStartTimings;
    }

    public static class ServiceNameAndTime {
        private final String serviceName;
        private final Long time;

        public ServiceNameAndTime(String serviceName, Long time) {
            this.serviceName = serviceName;
            this.time = time;
        }

        public String getServiceName() {
            return serviceName;
        }

        public Long getTime() {
            return time;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ServiceNameAndTime that = (ServiceNameAndTime) o;
            if (!serviceName.equals(that.serviceName)) {
                return false;
            }
            if (!time.equals(that.time)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = serviceName.hashCode();
            result = 31 * result + time.hashCode();
            return result;
        }
    }

    private static class ServiceNameAndType {
        private final String serviceName;
        private final Type type;

        public ServiceNameAndType(String serviceName, Type type) {
            this.serviceName = serviceName;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ServiceNameAndType that = (ServiceNameAndType) o;
            if (!serviceName.equals(that.serviceName)) {
                return false;
            }
            if (type != that.type) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = serviceName.hashCode();
            result = 31 * result + type.hashCode();
            return result;
        }
    }

    private enum Type {
        initialization, start
    }

}
