package com.clinic.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Test clock that can be moved, so time-dependent rules (24-hour cancellation,
 * 1-hour lead time, no-show after the slot ends) can be tested exactly at their boundaries.
 */
public class MutableClock extends Clock {

    private Instant instant;

    public MutableClock(LocalDateTime start) {
        setTo(start);
    }

    public void setTo(LocalDateTime time) {
        this.instant = time.toInstant(ZoneOffset.UTC);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
