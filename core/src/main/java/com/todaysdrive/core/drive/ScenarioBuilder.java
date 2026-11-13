package com.todaysdrive.core.drive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 시연·테스트용 주행 시나리오를 만든다. 실제 차량 신호처럼 시간 순서의 {@link SignalEvent} 목록을 낸다.
 *
 * <pre>
 * new ScenarioBuilder(0)
 *     .gear(GearState.PARK).seatbelt(true).gear(GearState.DRIVE)
 *     .speedTo(50, 4)          // 초당 4km/h씩 50km/h까지
 *     .cruiseKm(2)             // 2km 정속
 *     .speedTo(20, 3)
 *     .signal(TurnSignalState.LEFT).hold(4000)
 *     .turn(-1, 360, 4000)     // 왼쪽으로 핸들 360도까지 돌렸다 풀기
 *     .signal(TurnSignalState.NONE)
 *     .build();
 * </pre>
 */
public final class ScenarioBuilder {

    private static final long SPEED_STEP_MS = 500;
    private static final long STEER_STEP_MS = 200;
    private static final long SENSOR_PERIOD_MS = 1000;

    private final List<SignalEvent> events = new ArrayList<>();
    private long t;
    private double speed;
    private double meters;

    public ScenarioBuilder(long startMs) {
        this.t = startMs;
    }

    public long timeMs() {
        return t;
    }

    public double distanceKm() {
        return meters / 1000.0;
    }

    public ScenarioBuilder gear(GearState gear) {
        events.add(SignalEvent.gear(t, gear));
        return this;
    }

    public ScenarioBuilder seatbelt(boolean buckled) {
        events.add(SignalEvent.seatbelt(t, buckled));
        return this;
    }

    public ScenarioBuilder wiper(boolean on) {
        events.add(SignalEvent.wiper(t, on));
        return this;
    }

    public ScenarioBuilder brake(boolean pressed) {
        events.add(SignalEvent.brake(t, pressed));
        return this;
    }

    public ScenarioBuilder signal(TurnSignalState state) {
        events.add(SignalEvent.turnSignal(t, state));
        return this;
    }

    public ScenarioBuilder steer(double degrees) {
        events.add(SignalEvent.steering(t, degrees));
        return this;
    }

    /** 아무 신호 없이 시간만 흐름(주차 중, 시동 꺼짐) */
    public ScenarioBuilder pause(long ms) {
        t += ms;
        return this;
    }

    /** 지금 속도를 유지하며 1초마다 속도 신호를 낸다. */
    public ScenarioBuilder hold(long ms) {
        long end = t + ms;
        while (t < end) {
            long step = Math.min(SENSOR_PERIOD_MS, end - t);
            advance(step);
            events.add(SignalEvent.speed(t, speed));
        }
        return this;
    }

    /** 초당 kmhPerSec씩 target까지 속도를 바꾼다. */
    public ScenarioBuilder speedTo(double target, double kmhPerSec) {
        double perStep = kmhPerSec * SPEED_STEP_MS / 1000.0;
        while (Math.abs(target - speed) > 1e-9) {
            double next = speed < target ? Math.min(target, speed + perStep) : Math.max(target, speed - perStep);
            meters += (speed + next) / 2 / 3.6 * (SPEED_STEP_MS / 1000.0);
            t += SPEED_STEP_MS;
            speed = next;
            events.add(SignalEvent.speed(t, speed));
        }
        return this;
    }

    /** 지금 속도를 유지하며 시나리오 시각이 timeMs가 될 때까지 달린다. */
    public ScenarioBuilder holdUntil(long timeMs) {
        return timeMs > t ? hold(timeMs - t) : this;
    }

    public double speedKmh() {
        return speed;
    }

    /** 지금 속도로 km만큼 달린다. */
    public ScenarioBuilder cruiseKm(double km) {
        if (speed <= 0) {
            throw new IllegalStateException("정속 주행 전에 속도를 올려야 합니다");
        }
        long ms = Math.round(km * 1000.0 / (speed / 3.6) * 1000.0);
        return hold(ms);
    }

    /** 누적 거리가 totalKm가 될 때까지 지금 속도로 달린다. */
    public ScenarioBuilder cruiseUntilKm(double totalKm) {
        double remain = totalKm - distanceKm();
        return remain > 0 ? cruiseKm(remain) : this;
    }

    /**
     * 핸들을 dir 방향(왼쪽 -1, 오른쪽 +1)으로 peakDeg까지 돌렸다가 0으로 되돌린다.
     * 속도는 그대로 유지한다.
     */
    public ScenarioBuilder turn(int dir, double peakDeg, long durationMs) {
        long steps = Math.max(2, durationMs / STEER_STEP_MS);
        long sinceSpeed = 0;
        for (long i = 1; i <= steps; i++) {
            advance(STEER_STEP_MS);
            double frac = (double) i / steps;
            double profile = frac <= 0.5 ? frac * 2 : (1 - frac) * 2;
            double deg = i == steps ? 0 : dir * peakDeg * profile;
            events.add(SignalEvent.steering(t, deg));
            sinceSpeed += STEER_STEP_MS;
            if (sinceSpeed >= SENSOR_PERIOD_MS) {
                sinceSpeed = 0;
                events.add(SignalEvent.speed(t, speed));
            }
        }
        return this;
    }

    private void advance(long ms) {
        meters += speed / 3.6 * (ms / 1000.0);
        t += ms;
    }

    public List<SignalEvent> build() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }
}
