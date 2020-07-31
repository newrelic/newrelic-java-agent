/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

public class TransactionTiming {

    static final float UNASSIGNED_FLOAT = Float.NEGATIVE_INFINITY;

    /**
     * Required. Duration of a transaction. Does not include queue time.
     */
    private final float duration;
    private final float totalTime;
    private final float timeToFirstByte;
    private final float timeToLastByte;
    private final float queueDuration;
    private final float gcCumulative;
    private final CountedDuration external;
    private final CountedDuration database;

    private TransactionTiming(Builder builder) {
        this.duration = builder.duration;
        this.totalTime = builder.totalTime;
        this.timeToFirstByte = builder.timeToFirstByte;
        this.timeToLastByte = builder.timeToLastByte;
        this.queueDuration = builder.queueDuration;
        this.gcCumulative = builder.gcCumulative;
        this.external = builder.external;
        this.database = builder.database;
    }

    public float getDuration() {
        return duration;
    }

    public float getTotalTime() {
        return totalTime;
    }

    public float getTimeToFirstByte() {
        return timeToFirstByte;
    }

    public float getTimeToLastByte() {
        return timeToLastByte;
    }

    public float getQueueDuration() {
        return queueDuration;
    }

    public static Builder builder(){
        return new Builder();
    }

    public float getExternalCallCount() {
        return external.getCallCount();
    }

    public float getExternalDuration() {
        return external.getDuration();
    }

    public float getDatabaseCallCount() {
        return database.getCallCount();
    }

    public float getDatabaseDuration() {
        return database.getDuration();
    }

    public float getGcCumulative() {
        return gcCumulative;
    }

    public static class Builder {
        private float duration;
        private float totalTime;
        private float timeToFirstByte = UNASSIGNED_FLOAT;
        private float timeToLastByte = UNASSIGNED_FLOAT;
        private float queueDuration = UNASSIGNED_FLOAT;
        public float gcCumulative = UNASSIGNED_FLOAT;
        public CountedDuration external = CountedDuration.UNASSIGNED;
        public CountedDuration database = CountedDuration.UNASSIGNED;

        public Builder duration(float duration){
            this.duration = duration;
            return this;
        }

        public Builder totalTime(float totalTime){
            this.totalTime = totalTime;
            return this;
        }

        public Builder timeToFirstByte(float timeToFirstByte){
            this.timeToFirstByte = timeToFirstByte;
            return this;
        }

        public Builder timeToLastByte(float timeToLastByte){
            this.timeToLastByte = timeToLastByte;
            return this;
        }

        public Builder queueDuration(float queueDuration) {
            this.queueDuration = queueDuration;
            return this;
        }

        public Builder gcCumulative(float gcCumulative){
            this.gcCumulative = gcCumulative;
            return this;
        }

        public Builder external(CountedDuration external){
            this.external = external;
            return this;
        }

        public Builder database(CountedDuration database){
            this.database = database;
            return this;
        }

        public TransactionTiming build() {
            return new TransactionTiming(this);
        }
    }
}
