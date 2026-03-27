package com.exproject.kraxmusicplayer.service.impl;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TranscodeJobTracker {
    private final AtomicInteger running = new AtomicInteger(0);

    public void jobStarted() {
        running.incrementAndGet();
    }

    public void jobFinished() {
        running.decrementAndGet();
        if (running.get() < 0) running.set(0);
    }

    public int getRunningJobs() {
        return running.get();
    }
}