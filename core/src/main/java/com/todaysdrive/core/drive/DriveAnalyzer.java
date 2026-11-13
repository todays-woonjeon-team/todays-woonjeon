package com.todaysdrive.core.drive;

import com.todaysdrive.core.RuleConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 차량 신호를 시간 순서대로 받아 하루 주행 판정을 쌓는다.
 *
 * <p>신호는 값이 바뀔 때 오는 콜백만 받는다(주기적으로 읽지 않음). 그래서 두 신호 사이에는
 * 직전 값이 그대로 유지됐다고 보고 거리·시간을 계산한다.
 *
 * <p>판정하는 것
 * <ul>
 *   <li>교차로 회전: 낮은 속도에서 핸들을 크게 돌리고 진행 방향이 크게 바뀐 경우.
 *       회전 직전·도중에 같은 방향 깜빡이가 켜져 있었는지, 핸들을 돌리기 3초 이상 전에 켰는지</li>
 *   <li>차로 변경: 깜빡이를 켠 경우만 미리 켜기 집계에 넣는다(감점 없음)</li>
 *   <li>급가속·급감속·급출발: 1초 동안의 속도 변화</li>
 *   <li>급회전: 일정 속도 이상에서 짧은 시간에 진행 방향이 크게 바뀐 경우</li>
 *   <li>빗길: 와이퍼를 켜고 달린 거리와 그동안의 급가속·급감속</li>
 *   <li>휴식: 연속 주행 시간과 P단 정차 시간</li>
 * </ul>
 *
 * <p>스레드 안전하지 않다. 한 스레드(차량 앱은 메인 스레드)에서만 호출한다.
 */
public final class DriveAnalyzer {

    private static final long HISTORY_MILLIS = 6_000;
    /** 꺼진 방향지시등을 차로 변경으로 확정하기 전에 기다리는 시간(회전 판정이 먼저 가져갈 수 있도록) */
    private static final long SIGNAL_SETTLE_MILLIS = 10_000;
    private static final double MOVING_KMH = 0.5;
    private static final double TRIP_START_KMH = 5.0;

    private final RuleConfig rules;
    private DriveStats stats;

    private long lastTime = -1;
    private double speedKmh;
    private GearState gear = GearState.UNKNOWN;
    private double steeringDeg;
    private boolean brakePressed;
    private boolean wiperOn;
    private Boolean seatbeltBuckled; // null = 신호를 받지 못함

    /** 추정한 진행 방향(도). 회전 판정용 */
    private double headingDeg;
    /** 급회전 기준 속도를 넘는 동안만 쌓은 진행 방향(도). 교차로 회전이 급회전으로 잡히지 않게 한다 */
    private double fastHeadingDeg;

    private final ArrayDeque<Sample> speedHistory = new ArrayDeque<>();
    private final ArrayDeque<Sample> fastHeadingHistory = new ArrayDeque<>();

    private boolean inHarshAccel;
    private boolean inHarshDecel;
    private boolean inHarshTurn;

    private Episode episode;
    private SignalInterval activeSignal;
    private final List<SignalInterval> closedSignals = new ArrayList<>();

    private boolean awaitingTripStart = true;

    public DriveAnalyzer(RuleConfig rules) {
        this(rules, new DriveStats());
    }

    private DriveAnalyzer(RuleConfig rules, DriveStats stats) {
        this.rules = rules;
        this.stats = stats;
    }

    /** 저장해 둔 오늘 기록에서 이어서 판정한다(앱 재시작, 시동 재시작). */
    public static DriveAnalyzer resume(RuleConfig rules, DriveStats saved) {
        DriveAnalyzer a = new DriveAnalyzer(rules, saved.copy());
        a.lastTime = saved.lastEventMs;
        if (saved.parkedSinceMs >= 0) {
            a.gear = GearState.PARK;
        }
        return a;
    }

    // ───────────────────────────── 입력 ─────────────────────────────

    public void accept(SignalEvent event) {
        long t = event.timeMs();
        if (lastTime >= 0) {
            if (t < lastTime) {
                t = lastTime; // 순서가 살짝 어긋난 콜백은 직전 시각으로 본다
            }
            long gap = t - lastTime;
            if (gap >= rules.restMinMillis && gear != GearState.PARK) {
                // 신호가 오래 끊김 = 시동이 꺼져 있었음. 그 사이는 주행이 아니라 휴식으로 본다.
                finishEpisode(lastTime);
                onRest(gap);
                speedKmh = 0;
                speedHistory.clear();
                fastHeadingHistory.clear();
            } else {
                integrate(lastTime, t);
            }
        }
        lastTime = t;
        stats.lastEventMs = t;

        switch (event.signal()) {
            case SPEED:
                onSpeed(t, event.value());
                break;
            case STEERING:
                onSteering(t, event.value());
                break;
            case GEAR:
                onGear(t, event.gearValue());
                break;
            case TURN_SIGNAL:
                onTurnSignal(t, event.turnSignalValue());
                break;
            case BRAKE:
                brakePressed = event.isOn();
                break;
            case WIPER:
                wiperOn = event.isOn();
                break;
            case SEATBELT:
                seatbeltBuckled = event.isOn();
                break;
            default:
                break;
        }
        checkHarshTurn(t);
        flushSignals(t, false);
    }

    public void acceptAll(Iterable<SignalEvent> events) {
        for (SignalEvent e : events) {
            accept(e);
        }
    }

    /**
     * 지금 시각까지 반영한 오늘 기록을 돌려준다. P단이면 진행 중인 판정도 모두 마무리한다.
     * 주차 리포트를 만들 때 부른다.
     */
    public DriveStats closeOut(long nowMs) {
        if (lastTime >= 0 && nowMs > lastTime) {
            long gap = nowMs - lastTime;
            if (gap >= rules.restMinMillis && gear != GearState.PARK) {
                finishEpisode(lastTime);
                onRest(gap);
                speedKmh = 0;
            } else {
                integrate(lastTime, nowMs);
            }
            lastTime = nowMs;
            stats.lastEventMs = nowMs;
        }
        if (isParked()) {
            finishEpisode(nowMs);
            flushSignals(nowMs, true);
            if (stats.parkedSinceMs >= 0) {
                onRest(nowMs - stats.parkedSinceMs);
            }
        }
        return stats.copy();
    }

    /** 날짜가 바뀌면 부른다. 어제 기록을 마무리해 돌려주고, 연속 주행 시간은 이어 간다. */
    public DriveStats startNewDay(long nowMs) {
        DriveStats finished = closeOut(nowMs);
        DriveStats next = new DriveStats();
        next.continuousMillis = stats.continuousMillis;
        next.lastEventMs = stats.lastEventMs;
        next.parkedSinceMs = stats.parkedSinceMs;
        stats = next;
        return finished;
    }

    /** 마무리하지 않은 현재 기록(복사본) */
    public DriveStats peek() {
        return stats.copy();
    }

    public boolean isParked() {
        return gear == GearState.PARK || (gear == GearState.UNKNOWN && speedKmh < MOVING_KMH);
    }

    public double speedKmh() {
        return speedKmh;
    }

    public GearState gear() {
        return gear;
    }

    // ───────────────────────────── 시간 적분 ─────────────────────────────

    private void integrate(long from, long to) {
        long dt = to - from;
        if (dt <= 0) {
            return;
        }
        double sec = dt / 1000.0;
        double meters = speedKmh / 3.6 * sec;
        stats.distanceMeters += meters;
        if (wiperOn) {
            stats.wiperMeters += meters;
        }
        if (isDriving()) {
            stats.drivingMillis += dt;
            stats.continuousMillis += dt;
            if (stats.continuousMillis > stats.longestContinuousMillis) {
                stats.longestContinuousMillis = stats.continuousMillis;
            }
            if (stats.continuousMillis > rules.restDeadlineMillis) {
                stats.exceededRestDeadline = true;
            }
        }
        double yaw = yawRateDegPerSec() * sec;
        headingDeg += yaw;
        if (speedKmh > rules.harshTurnMinSpeedKmh) {
            fastHeadingDeg += yaw;
        }
    }

    private boolean isDriving() {
        switch (gear) {
            case DRIVE:
            case REVERSE:
            case NEUTRAL:
                return true;
            case UNKNOWN:
                return speedKmh >= MOVING_KMH;
            default:
                return false;
        }
    }

    /** 자전거 모델로 추정한 회전 속도(도/초). 핸들 각도 → 바퀴 각도 → 진행 방향 변화 */
    private double yawRateDegPerSec() {
        if (speedKmh <= 0) {
            return 0;
        }
        double wheelRad = Math.toRadians(steeringDeg / rules.steeringRatio);
        double yawRad = (speedKmh / 3.6) / rules.wheelbaseMeters * Math.tan(wheelRad);
        return Math.toDegrees(yawRad);
    }

    // ───────────────────────────── 속도 ─────────────────────────────

    private void onSpeed(long t, double kmh) {
        double before = valueAt(speedHistory, t - 1000, speedKmh);
        speedKmh = kmh;
        push(speedHistory, t, kmh);
        if (episode != null) {
            episode.maxSpeed = Math.max(episode.maxSpeed, kmh);
        }
        if (activeSignal != null) {
            activeSignal.maxSpeed = Math.max(activeSignal.maxSpeed, kmh);
        }
        if (gear == GearState.PARK) {
            return;
        }
        if (awaitingTripStart && kmh >= TRIP_START_KMH) {
            awaitingTripStart = false;
            stats.tripStarts++;
            if (Boolean.TRUE.equals(seatbeltBuckled)) {
                stats.tripStartsBuckled++;
            }
        }

        double delta = kmh - before; // 1초 동안의 속도 변화(km/h)
        boolean harshStartNow = before <= rules.harshStartMaxSpeedKmh
                && delta >= rules.harshStartKmhPerSec;
        boolean harshAccelNow = before >= rules.harshAccelMinSpeedKmh
                && delta >= rules.harshAccelKmhPerSec;
        if (harshStartNow || harshAccelNow) {
            if (!inHarshAccel) {
                inHarshAccel = true;
                if (harshStartNow) {
                    stats.harshStart++;
                } else {
                    stats.harshAccel++;
                }
                countIfRaining();
            }
        } else if (delta < rules.harshAccelKmhPerSec * rules.harshReleaseRatio) {
            inHarshAccel = false;
        }

        if (-delta >= rules.harshDecelKmhPerSec) {
            if (!inHarshDecel) {
                inHarshDecel = true;
                stats.harshDecel++;
                if (brakePressed) {
                    stats.hardBrakes++;
                }
                countIfRaining();
            }
        } else if (-delta < rules.harshDecelKmhPerSec * rules.harshReleaseRatio) {
            inHarshDecel = false;
        }
    }

    private void countIfRaining() {
        if (wiperOn) {
            stats.harshWhileWiper++;
        }
    }

    // ───────────────────────────── 급회전 ─────────────────────────────

    private void checkHarshTurn(long t) {
        push(fastHeadingHistory, t, fastHeadingDeg);
        if (speedKmh <= rules.harshTurnMinSpeedKmh || gear == GearState.PARK) {
            return;
        }
        double before = valueAt(fastHeadingHistory, t - rules.harshTurnWindowMillis, fastHeadingDeg);
        double change = Math.abs(fastHeadingDeg - before);
        if (change >= rules.harshTurnHeadingDeg) {
            if (!inHarshTurn) {
                inHarshTurn = true;
                stats.harshTurn++;
            }
        } else if (change < rules.harshTurnHeadingDeg * rules.harshReleaseRatio) {
            inHarshTurn = false;
        }
    }

    // ───────────────────────────── 핸들·회전 ─────────────────────────────

    private void onSteering(long t, double deg) {
        steeringDeg = deg;
        double abs = Math.abs(deg);
        int dir = direction(deg);

        if (activeSignal != null && activeSignal.firstSteerAt < 0
                && dir == activeSignal.dir && abs >= rules.laneSteerDeg) {
            activeSignal.firstSteerAt = t;
        }

        if (episode == null) {
            if (abs >= rules.steerStartDeg) {
                startEpisode(t, dir, abs);
            }
        } else if (abs < rules.steerEndDeg || dir != episode.dir) {
            finishEpisode(t);
            if (abs >= rules.steerStartDeg) {
                startEpisode(t, dir, abs);
            }
        } else {
            episode.peak = Math.max(episode.peak, abs);
        }
    }

    private void startEpisode(long t, int dir, double abs) {
        episode = new Episode(t, dir, abs, speedKmh, headingDeg);
        episode.sawReverse = gear == GearState.REVERSE;
    }

    private void finishEpisode(long t) {
        Episode ep = episode;
        if (ep == null) {
            return;
        }
        episode = null;
        double headingChange = Math.abs(headingDeg - ep.headingStart);
        boolean isTurn = !ep.sawReverse
                && ep.peak >= rules.turnSteerDeg
                && ep.maxSpeed >= rules.turnMinSpeedKmh
                && ep.maxSpeed <= rules.turnMaxSpeedKmh
                && headingChange >= rules.turnHeadingDeg;
        if (!isTurn) {
            return;
        }
        stats.turns++;
        SignalInterval signal = findSignal(ep.dir, ep.start - rules.signalGraceMillis, t);
        if (signal != null) {
            signal.claimed = true;
            stats.turnsSignaled++;
            stats.earlyEligible++;
            if (ep.start - signal.onAt >= rules.earlySignalMillis) {
                stats.earlySignaled++;
            }
        }
    }

    // ───────────────────────────── 기어·휴식 ─────────────────────────────

    private void onGear(long t, GearState next) {
        GearState prev = gear;
        gear = next;
        if (episode != null && next == GearState.REVERSE) {
            episode.sawReverse = true;
        }
        if (next == GearState.PARK && prev != GearState.PARK) {
            stats.parkedSinceMs = t;
            awaitingTripStart = true;
            inHarshAccel = false;
            inHarshDecel = false;
            finishEpisode(t);
        } else if (next != GearState.PARK && prev == GearState.PARK) {
            if (stats.parkedSinceMs >= 0) {
                onRest(t - stats.parkedSinceMs);
            }
            stats.parkedSinceMs = -1;
        }
    }

    /** restMillis 동안 차가 서 있었다. 15분 이상이면 휴식으로 인정하고 연속 주행 시간을 0으로 돌린다. */
    private void onRest(long restMillis) {
        if (restMillis < rules.restMinMillis) {
            return;
        }
        if (stats.continuousMillis > rules.longDriveMillis
                && stats.continuousMillis <= rules.restDeadlineMillis) {
            stats.restTakenInWindow = true;
        }
        stats.continuousMillis = 0;
        awaitingTripStart = true;
    }

    // ───────────────────────────── 방향지시등 ─────────────────────────────

    private void onTurnSignal(long t, TurnSignalState state) {
        int dir = state.direction();
        if (activeSignal != null && activeSignal.dir == dir) {
            return; // 같은 상태가 다시 들어옴
        }
        if (activeSignal != null) {
            activeSignal.offAt = t;
            closedSignals.add(activeSignal);
            activeSignal = null;
        }
        if (dir == 0) {
            return; // 꺼짐 또는 비상등
        }
        SignalInterval last = closedSignals.isEmpty() ? null : closedSignals.get(closedSignals.size() - 1);
        if (last != null && last.dir == dir && t - last.offAt <= rules.blinkMergeMillis) {
            // 램프 깜빡임처럼 잠깐 꺼졌다 켜진 것은 한 번 켠 것으로 이어 붙인다
            closedSignals.remove(closedSignals.size() - 1);
            last.offAt = -1;
            activeSignal = last;
            return;
        }
        activeSignal = new SignalInterval(t, dir, speedKmh);
        if (direction(steeringDeg) == dir && Math.abs(steeringDeg) >= rules.laneSteerDeg) {
            activeSignal.firstSteerAt = t;
        }
    }

    private SignalInterval findSignal(int dir, long from, long to) {
        SignalInterval best = null;
        if (activeSignal != null && activeSignal.overlaps(dir, from, to)) {
            best = activeSignal;
        }
        for (SignalInterval s : closedSignals) {
            if (s.overlaps(dir, from, to) && (best == null || s.onAt > best.onAt)) {
                best = s;
            }
        }
        return best;
    }

    /** 회전에 쓰이지 않은 깜빡이 구간을 차로 변경으로 판정하고 정리한다. */
    private void flushSignals(long now, boolean force) {
        Iterator<SignalInterval> it = closedSignals.iterator();
        while (it.hasNext()) {
            SignalInterval s = it.next();
            if (!force) {
                if (now - s.offAt < SIGNAL_SETTLE_MILLIS) {
                    continue;
                }
                if (episode != null && s.offAt >= episode.start - rules.signalGraceMillis) {
                    continue; // 진행 중인 회전이 이 깜빡이를 쓸 수 있음
                }
            }
            if (!s.claimed) {
                countLaneChange(s);
            }
            it.remove();
        }
    }

    private void countLaneChange(SignalInterval s) {
        if (s.maxSpeed < rules.laneChangeMinSpeedKmh
                || s.firstSteerAt < 0
                || s.offAt - s.onAt < rules.laneSignalMinMillis) {
            return;
        }
        stats.laneChangesSignaled++;
        stats.earlyEligible++;
        if (s.firstSteerAt - s.onAt >= rules.earlySignalMillis) {
            stats.earlySignaled++;
        }
    }

    // ───────────────────────────── 도우미 ─────────────────────────────

    private static int direction(double steeringDeg) {
        return steeringDeg < 0 ? -1 : (steeringDeg > 0 ? 1 : 0);
    }

    private static void push(ArrayDeque<Sample> history, long t, double value) {
        history.addLast(new Sample(t, value));
        // 기준 시각보다 오래된 값은 하나만 남긴다(그 시각의 값을 알아야 하므로)
        while (history.size() >= 2) {
            Sample first = history.pollFirst();
            Sample second = history.peekFirst();
            if (second.time > t - HISTORY_MILLIS) {
                history.addFirst(first);
                break;
            }
        }
    }

    /** target 시각에 유지되고 있던 값. 기록이 그보다 짧으면 가장 오래된 값 */
    private static double valueAt(ArrayDeque<Sample> history, long target, double fallback) {
        if (history.isEmpty()) {
            return fallback;
        }
        Iterator<Sample> it = history.descendingIterator();
        while (it.hasNext()) {
            Sample s = it.next();
            if (s.time <= target) {
                return s.value;
            }
        }
        return history.peekFirst().value;
    }

    private static final class Sample {
        final long time;
        final double value;

        Sample(long time, double value) {
            this.time = time;
            this.value = value;
        }
    }

    /** 핸들을 한 방향으로 돌렸다가 되돌리기까지 */
    private static final class Episode {
        final long start;
        final int dir;
        final double headingStart;
        double peak;
        double maxSpeed;
        boolean sawReverse;

        Episode(long start, int dir, double peak, double speed, double headingStart) {
            this.start = start;
            this.dir = dir;
            this.peak = peak;
            this.maxSpeed = speed;
            this.headingStart = headingStart;
        }
    }

    /** 방향지시등을 켜 둔 구간 */
    private static final class SignalInterval {
        final long onAt;
        final int dir;
        long offAt = -1;
        long firstSteerAt = -1;
        double maxSpeed;
        boolean claimed;

        SignalInterval(long onAt, int dir, double speed) {
            this.onAt = onAt;
            this.dir = dir;
            this.maxSpeed = speed;
        }

        boolean overlaps(int direction, long from, long to) {
            return dir == direction && !claimed && onAt <= to && (offAt < 0 || offAt >= from);
        }
    }
}
