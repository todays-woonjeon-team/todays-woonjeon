package com.todaysdrive.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 판정 기준값 모음. 모든 값은 {@code rules.properties}로 바꿀 수 있다.
 *
 * <p>급가속·급감속·급출발·급회전 기준은 한국교통안전공단이 사업용 차량에 쓰는 위험운전행동 기준을
 * 참고한 초기값이다. 승용차에 맞게 에뮬레이터 테스트를 하면서 조정한다.
 * 키 목록과 근거는 docs/scoring-rules.md 참고.
 */
public final class RuleConfig {

    private static final long MINUTE = 60_000L;

    // ── 급가속·급출발·급감속 (1초 동안의 속도 변화) ──
    public final double harshAccelKmhPerSec;
    public final double harshAccelMinSpeedKmh;
    public final double harshStartKmhPerSec;
    public final double harshStartMaxSpeedKmh;
    public final double harshDecelKmhPerSec;
    /** 급조작이 끝났다고 보는 비율. 변화량이 기준의 70% 아래로 내려가야 다음 급조작을 센다. */
    public final double harshReleaseRatio;

    // ── 급회전(급하게 핸들을 꺾음): 일정 속도 이상에서 짧은 시간에 진행 방향이 크게 바뀜 ──
    public final double harshTurnMinSpeedKmh;
    public final double harshTurnHeadingDeg;
    public final long harshTurnWindowMillis;

    // ── 진행 방향 추정용 차량 값 (핸들 각도 → 바퀴 각도 → 회전 속도) ──
    public final double steeringRatio;
    public final double wheelbaseMeters;

    // ── 교차로 회전 판정 ──
    public final double steerStartDeg;
    public final double steerEndDeg;
    public final double turnSteerDeg;
    public final double turnHeadingDeg;
    public final double turnMinSpeedKmh;
    public final double turnMaxSpeedKmh;
    public final long signalGraceMillis;
    public final long earlySignalMillis;
    public final long blinkMergeMillis;

    // ── 차로 변경(방향지시등을 켠 경우만 미리 켜기 집계) ──
    public final double laneChangeMinSpeedKmh;
    public final double laneSteerDeg;
    public final long laneSignalMinMillis;

    // ── 빗길·휴식 ──
    public final double rainMinMeters;
    public final long longDriveMillis;
    public final long restDeadlineMillis;
    public final long restMinMillis;

    // ── 점수 ──
    public final double minScoreMeters;
    public final double shortTripMeters;

    private RuleConfig(Properties p) {
        harshAccelKmhPerSec = num(p, "harsh.accel.kmhPerSec", 6.0);
        harshAccelMinSpeedKmh = num(p, "harsh.accel.minSpeedKmh", 6.0);
        harshStartKmhPerSec = num(p, "harsh.start.kmhPerSec", 8.0);
        harshStartMaxSpeedKmh = num(p, "harsh.start.maxSpeedKmh", 5.0);
        harshDecelKmhPerSec = num(p, "harsh.decel.kmhPerSec", 9.0);
        harshReleaseRatio = num(p, "harsh.releaseRatio", 0.7);

        harshTurnMinSpeedKmh = num(p, "harsh.turn.minSpeedKmh", 25.0);
        harshTurnHeadingDeg = num(p, "harsh.turn.headingDeg", 60.0);
        harshTurnWindowMillis = (long) num(p, "harsh.turn.windowMillis", 4000);

        steeringRatio = num(p, "vehicle.steeringRatio", 14.0);
        wheelbaseMeters = num(p, "vehicle.wheelbaseMeters", 2.7);

        steerStartDeg = num(p, "turn.steerStartDeg", 20.0);
        steerEndDeg = num(p, "turn.steerEndDeg", 10.0);
        turnSteerDeg = num(p, "turn.minPeakSteerDeg", 90.0);
        turnHeadingDeg = num(p, "turn.minHeadingDeg", 45.0);
        turnMinSpeedKmh = num(p, "turn.minSpeedKmh", 8.0);
        turnMaxSpeedKmh = num(p, "turn.maxSpeedKmh", 40.0);
        signalGraceMillis = (long) num(p, "signal.graceMillis", 2000);
        earlySignalMillis = (long) num(p, "signal.earlyMillis", 3000);
        blinkMergeMillis = (long) num(p, "signal.blinkMergeMillis", 1200);

        laneChangeMinSpeedKmh = num(p, "lane.minSpeedKmh", 30.0);
        laneSteerDeg = num(p, "lane.minSteerDeg", 5.0);
        laneSignalMinMillis = (long) num(p, "lane.minSignalMillis", 1000);

        rainMinMeters = num(p, "rain.minMeters", 5000);
        longDriveMillis = (long) (num(p, "rest.longDriveMinutes", 90) * MINUTE);
        restDeadlineMillis = (long) (num(p, "rest.deadlineMinutes", 120) * MINUTE);
        restMinMillis = (long) (num(p, "rest.minRestMinutes", 15) * MINUTE);

        minScoreMeters = num(p, "score.minMeters", 3000);
        shortTripMeters = num(p, "score.shortTripMeters", 10000);
    }

    public static RuleConfig defaults() {
        return new RuleConfig(new Properties());
    }

    public static RuleConfig from(Properties properties) {
        return new RuleConfig(properties);
    }

    public static RuleConfig load(InputStream in) throws IOException {
        Properties p = new Properties();
        p.load(in);
        return new RuleConfig(p);
    }

    private static double num(Properties p, String key, double fallback) {
        String v = p.getProperty(key);
        if (v == null || v.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("rules.properties 값 오류: " + key + "=" + v, e);
        }
    }
}
