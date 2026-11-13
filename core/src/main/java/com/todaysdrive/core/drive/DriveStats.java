package com.todaysdrive.core.drive;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.todaysdrive.core.link.Json.asBool;
import static com.todaysdrive.core.link.Json.asDouble;
import static com.todaysdrive.core.link.Json.asInt;
import static com.todaysdrive.core.link.Json.asLong;

/**
 * 하루 동안 모은 주행 판정 결과. 위치나 신호 원본은 담지 않고 횟수·거리·시간만 담는다.
 *
 * <p>차량 앱은 이 값을 기기 안(SQLite)에만 저장하고, 서버에는 점수 요약만 보낸다.
 */
public final class DriveStats {

    double distanceMeters;
    long drivingMillis;

    int turns;
    int turnsSignaled;
    int earlyEligible;
    int earlySignaled;
    int laneChangesSignaled;

    int harshAccel;
    int harshDecel;
    int harshStart;
    int harshTurn;
    int hardBrakes;

    double wiperMeters;
    int harshWhileWiper;

    long longestContinuousMillis;
    boolean restTakenInWindow;
    boolean exceededRestDeadline;

    int tripStarts;
    int tripStartsBuckled;

    // 앱을 다시 켜도 연속 주행 시간을 이어 가기 위한 값
    long continuousMillis;
    long lastEventMs = -1;
    long parkedSinceMs = -1;

    public DriveStats() {
    }

    public DriveStats copy() {
        return fromMap(toMap());
    }

    public double distanceMeters() {
        return distanceMeters;
    }

    public double distanceKm() {
        return distanceMeters / 1000.0;
    }

    public long drivingMillis() {
        return drivingMillis;
    }

    /** 교차로 회전 횟수 */
    public int turns() {
        return turns;
    }

    /** 방향지시등을 켜고 돈 회전 횟수 */
    public int turnsSignaled() {
        return turnsSignaled;
    }

    /** 미리 켜기 판정 대상(깜빡이를 켠 회전 + 깜빡이를 켠 차로 변경) */
    public int earlyEligible() {
        return earlyEligible;
    }

    /** 그중 핸들을 돌리기 3초 이상 전에 켠 횟수 */
    public int earlySignaled() {
        return earlySignaled;
    }

    public int laneChangesSignaled() {
        return laneChangesSignaled;
    }

    public int harshAccel() {
        return harshAccel;
    }

    public int harshDecel() {
        return harshDecel;
    }

    public int harshStart() {
        return harshStart;
    }

    /** 급하게 핸들을 꺾은 횟수(급좌우회전) */
    public int harshTurn() {
        return harshTurn;
    }

    /** 급감속 중 브레이크 페달이 밟혀 있던 횟수(급브레이크) */
    public int hardBrakes() {
        return hardBrakes;
    }

    /** 급가속·급감속·급출발·급회전 합계 */
    public int harshTotal() {
        return harshAccel + harshDecel + harshStart + harshTurn;
    }

    public double wiperMeters() {
        return wiperMeters;
    }

    public int harshWhileWiper() {
        return harshWhileWiper;
    }

    public long longestContinuousMillis() {
        return longestContinuousMillis;
    }

    /** 연속 주행이 1시간 30분을 넘긴 뒤, 2시간이 되기 전에 15분 이상 P단으로 쉬었는지 */
    public boolean restTakenInWindow() {
        return restTakenInWindow;
    }

    /** 쉬지 않고 2시간을 넘겨 달린 적이 있는지 */
    public boolean exceededRestDeadline() {
        return exceededRestDeadline;
    }

    public int tripStarts() {
        return tripStarts;
    }

    public int tripStartsBuckled() {
        return tripStartsBuckled;
    }

    public long continuousMillis() {
        return continuousMillis;
    }

    public long lastEventMs() {
        return lastEventMs;
    }

    public long parkedSinceMs() {
        return parkedSinceMs;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("distanceMeters", distanceMeters);
        m.put("drivingMillis", drivingMillis);
        m.put("turns", turns);
        m.put("turnsSignaled", turnsSignaled);
        m.put("earlyEligible", earlyEligible);
        m.put("earlySignaled", earlySignaled);
        m.put("laneChangesSignaled", laneChangesSignaled);
        m.put("harshAccel", harshAccel);
        m.put("harshDecel", harshDecel);
        m.put("harshStart", harshStart);
        m.put("harshTurn", harshTurn);
        m.put("hardBrakes", hardBrakes);
        m.put("wiperMeters", wiperMeters);
        m.put("harshWhileWiper", harshWhileWiper);
        m.put("longestContinuousMillis", longestContinuousMillis);
        m.put("restTakenInWindow", restTakenInWindow);
        m.put("exceededRestDeadline", exceededRestDeadline);
        m.put("tripStarts", tripStarts);
        m.put("tripStartsBuckled", tripStartsBuckled);
        m.put("continuousMillis", continuousMillis);
        m.put("lastEventMs", lastEventMs);
        m.put("parkedSinceMs", parkedSinceMs);
        return m;
    }

    public static DriveStats fromMap(Map<String, Object> m) {
        DriveStats s = new DriveStats();
        if (m == null) {
            return s;
        }
        s.distanceMeters = asDouble(m.get("distanceMeters"), 0);
        s.drivingMillis = asLong(m.get("drivingMillis"), 0);
        s.turns = asInt(m.get("turns"), 0);
        s.turnsSignaled = asInt(m.get("turnsSignaled"), 0);
        s.earlyEligible = asInt(m.get("earlyEligible"), 0);
        s.earlySignaled = asInt(m.get("earlySignaled"), 0);
        s.laneChangesSignaled = asInt(m.get("laneChangesSignaled"), 0);
        s.harshAccel = asInt(m.get("harshAccel"), 0);
        s.harshDecel = asInt(m.get("harshDecel"), 0);
        s.harshStart = asInt(m.get("harshStart"), 0);
        s.harshTurn = asInt(m.get("harshTurn"), 0);
        s.hardBrakes = asInt(m.get("hardBrakes"), 0);
        s.wiperMeters = asDouble(m.get("wiperMeters"), 0);
        s.harshWhileWiper = asInt(m.get("harshWhileWiper"), 0);
        s.longestContinuousMillis = asLong(m.get("longestContinuousMillis"), 0);
        s.restTakenInWindow = asBool(m.get("restTakenInWindow"), false);
        s.exceededRestDeadline = asBool(m.get("exceededRestDeadline"), false);
        s.tripStarts = asInt(m.get("tripStarts"), 0);
        s.tripStartsBuckled = asInt(m.get("tripStartsBuckled"), 0);
        s.continuousMillis = asLong(m.get("continuousMillis"), 0);
        s.lastEventMs = asLong(m.get("lastEventMs"), -1);
        s.parkedSinceMs = asLong(m.get("parkedSinceMs"), -1);
        return s;
    }

    /** 테스트와 화면 미리보기용으로 값을 직접 채우는 빌더 */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final DriveStats s = new DriveStats();

        public Builder distanceKm(double km) {
            s.distanceMeters = km * 1000.0;
            return this;
        }

        public Builder drivingMinutes(long minutes) {
            s.drivingMillis = minutes * 60_000L;
            return this;
        }

        public Builder turns(int turns, int signaled) {
            s.turns = turns;
            s.turnsSignaled = signaled;
            return this;
        }

        public Builder early(int eligible, int early) {
            s.earlyEligible = eligible;
            s.earlySignaled = early;
            return this;
        }

        public Builder harsh(int accel, int decel, int start, int turn) {
            s.harshAccel = accel;
            s.harshDecel = decel;
            s.harshStart = start;
            s.harshTurn = turn;
            return this;
        }

        public Builder hardBrakes(int n) {
            s.hardBrakes = n;
            return this;
        }

        public Builder wiper(double km, int harshWhileWiper) {
            s.wiperMeters = km * 1000.0;
            s.harshWhileWiper = harshWhileWiper;
            return this;
        }

        public Builder rest(long longestContinuousMinutes, boolean restTaken, boolean exceeded) {
            s.longestContinuousMillis = longestContinuousMinutes * 60_000L;
            s.restTakenInWindow = restTaken;
            s.exceededRestDeadline = exceeded;
            return this;
        }

        public Builder tripStarts(int starts, int buckled) {
            s.tripStarts = starts;
            s.tripStartsBuckled = buckled;
            return this;
        }

        public DriveStats build() {
            return s.copy();
        }
    }
}
