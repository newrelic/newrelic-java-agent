/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import com.newrelic.agent.IgnoreSilentlyException;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class ProfileSession {

    private final ProfileSampler profileSampler = new ProfileSampler();
    private final IProfile profile;
    private final List<IProfile> profiles = new ArrayList<>();
    private final ProfilerService profilerService;
    private final AtomicBoolean done = new AtomicBoolean(false);
    private final AtomicReference<ScheduledFuture<?>> profileHandle = new AtomicReference<>();
    private final AtomicReference<ScheduledFuture<?>> timeoutHandle = new AtomicReference<>();

    public ProfileSession(ProfilerService profilerService, ProfilerParameters profilerParameters) {
        this.profilerService = profilerService;
        profile = createProfile(profilerParameters);
        profile.start();
        profiles.add(profile);
    }

    private IProfile createProfile(ProfilerParameters profilerParameters) {
        return new Profile(profilerParameters);
    }

    void start() {
        long samplePeriodInMillis = profile.getProfilerParameters().getSamplePeriodInMillis();
        long durationInMillis = profile.getProfilerParameters().getDurationInMillis();
        if (samplePeriodInMillis == durationInMillis) {
            getLogger().info("Starting single sample profiling session");
            startSingleSample();
        } else {
            getLogger().info(
                    MessageFormat.format("Starting profiling session. Duration: {0} ms, sample period: {1} ms", durationInMillis, samplePeriodInMillis));
            startMultiSample(samplePeriodInMillis, durationInMillis);
        }
    }

    private void startMultiSample(long samplePeriodInMillis, long durationInMillis) {
        ScheduledExecutorService scheduler = profilerService.getScheduledExecutorService();
        ScheduledFuture<?> handle = scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    profileSampler.sampleStackTraces(profiles);
                } catch (Throwable t) {
                    String msg = MessageFormat.format("An error occurred collecting a thread sample: {0}",
                            t.toString());
                    if (getLogger().isLoggable(Level.FINER)) {
                        getLogger().log(Level.SEVERE, msg, t);
                    } else {
                        getLogger().severe(msg);
                    }
                }
            }
        }, 0L, samplePeriodInMillis, TimeUnit.MILLISECONDS);
        profileHandle.set(handle);
        handle = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                profileHandle.get().cancel(false);
                if (!done.getAndSet(true)) {
                    report();
                }
                sessionCompleted();
            }
        }, durationInMillis, TimeUnit.MILLISECONDS);
        timeoutHandle.set(handle);
    }

    private void startSingleSample() {
        ScheduledExecutorService scheduler = profilerService.getScheduledExecutorService();
        ScheduledFuture<?> handle = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    profileSampler.sampleStackTraces(profiles);
                } catch (Throwable t) {
                    String msg = MessageFormat.format("An error occurred collecting a thread sample: {0}", t.toString());
                    if (getLogger().isLoggable(Level.FINER)) {
                        getLogger().log(Level.SEVERE, msg, t);
                    } else {
                        getLogger().severe(msg);
                    }
                }
                if (!done.getAndSet(true)) {
                    report();
                }
                sessionCompleted();
            }
        }, 0L, TimeUnit.MILLISECONDS);
        profileHandle.set(handle);
    }

    private void report() {
        try {
            profile.end();
            profile.markInstrumentedMethods();
            getLogger().info(MessageFormat.format("Profiler finished with {0} samples", profile.getSampleCount()));
        } catch (Throwable e) {
            getLogger().log(Level.SEVERE, "Error finishing profile - no profiles will be sent", e);
            return;
        }

        final List<ProfileData> data = new ArrayList<>();
        data.addAll(profiles);
        try {
            List<Long> ids = ServiceFactory.getRPMService().sendProfileData(data);
            getLogger().info(MessageFormat.format("Server profile ids: {0}", ids));
        } catch (IgnoreSilentlyException e) {
        } catch (Throwable e) {
            // HttpError/LicenseException handled here
            String msg = MessageFormat.format("Unable to send profile data: {0}", e.toString());
            if (getLogger().isLoggable(Level.FINER)) {
                getLogger().log(Level.SEVERE, msg, e);
            } else {
                getLogger().severe(msg);
            }
        }
    }

    private void sessionCompleted() {
        profilerService.sessionCompleted(this);
    }

    void stop(final boolean shouldReport) {
        if (done.getAndSet(true)) {
            return;
        }
        getLogger().log(Level.INFO, "Stopping profiling session");
        ScheduledFuture<?> handle = profileHandle.get();
        if (handle != null) {
            handle.cancel(false);
        }
        handle = timeoutHandle.get();
        if (handle != null) {
            handle.cancel(false);
        }
        profilerService.getScheduledExecutorService().schedule(new Runnable() {
            @Override
            public void run() {
                if (shouldReport) {
                    report();
                }
                sessionCompleted();
            }
        }, 0L, TimeUnit.MILLISECONDS);
    }

    public boolean isDone() {
        return done.get();
    }

    public Long getProfileId() {
        return profile.getProfileId();
    }

    public IProfile getProfile() {
        return profile;
    }

    private IAgentLogger getLogger() {
        return profilerService.getLogger();
    }

}
