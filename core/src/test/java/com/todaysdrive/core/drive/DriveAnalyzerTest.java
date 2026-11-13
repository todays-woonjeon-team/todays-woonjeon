package com.todaysdrive.core.drive;

import com.todaysdrive.core.RuleConfig;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DriveAnalyzerTest {

    private static final long MIN = 60_000L;
    private final RuleConfig rules = RuleConfig.defaults();

    private DriveStats run(ScenarioBuilder b) {
        DriveAnalyzer a = new DriveAnalyzer(rules);
        a.acceptAll(b.build());
        return a.closeOut(b.timeMs());
    }

    private static ScenarioBuilder start(double kmh) {
        ScenarioBuilder b = new ScenarioBuilder(0);
        b.gear(GearState.PARK).seatbelt(true).gear(GearState.DRIVE).speedTo(kmh, 4);
        return b;
    }

    private static void park(ScenarioBuilder b) {
        b.speedTo(0, 3).hold(1000).gear(GearState.PARK);
    }

    // ── 거리·시간 ──

    @Test
    public void 거리는_속도를_시간으로_쌓아_계산() {
        ScenarioBuilder b = new ScenarioBuilder(0);
        b.gear(GearState.DRIVE).steer(0);
        b.speedTo(36, 36);   // 1초 만에 36km/h(=10m/s)
        b.hold(100_000);     // 100초 = 1000m
        DriveStats s = run(b);
        assertEquals(1.0 + 0.005, s.distanceKm(), 0.01);
        assertEquals(101_000, s.drivingMillis());
    }

    // ── 회전과 방향지시등 ──

    private static void turn(ScenarioBuilder b, int dir, TurnSignalState signal, long leadMs) {
        b.speedTo(20, 3);
        if (signal != null) {
            b.signal(signal);
        }
        b.hold(leadMs).turn(dir, 360, 4000).hold(500);
        if (signal != null) {
            b.signal(TurnSignalState.NONE);
        }
        b.speedTo(40, 4).hold(15_000);
    }

    @Test
    public void 깜빡이를_3초_전에_켠_회전은_미리_켜기로_센다() {
        ScenarioBuilder b = start(40);
        turn(b, -1, TurnSignalState.LEFT, 4000);
        park(b);
        DriveStats s = run(b);
        assertEquals(1, s.turns());
        assertEquals(1, s.turnsSignaled());
        assertEquals(1, s.earlyEligible());
        assertEquals(1, s.earlySignaled());
        assertEquals(0, s.harshTurn());
    }

    @Test
    public void 늦게_켠_회전은_점등만_인정() {
        ScenarioBuilder b = start(40);
        turn(b, 1, TurnSignalState.RIGHT, 1000);
        park(b);
        DriveStats s = run(b);
        assertEquals(1, s.turnsSignaled());
        assertEquals(0, s.earlySignaled());
    }

    @Test
    public void 반대쪽_깜빡이나_비상등은_인정하지_않는다() {
        ScenarioBuilder b = start(40);
        turn(b, 1, TurnSignalState.LEFT, 4000);
        turn(b, -1, TurnSignalState.HAZARD, 4000);
        turn(b, -1, null, 4000);
        park(b);
        DriveStats s = run(b);
        assertEquals(3, s.turns());
        assertEquals(0, s.turnsSignaled());
    }

    @Test
    public void 깜빡임으로_끊겨_들어오는_신호도_한_번_켠_것으로_본다() {
        ScenarioBuilder b = start(40).speedTo(20, 3);
        for (int i = 0; i < 6; i++) { // 0.4초 켜짐, 0.4초 꺼짐을 반복
            b.signal(TurnSignalState.LEFT).hold(400).signal(TurnSignalState.NONE).hold(400);
        }
        b.signal(TurnSignalState.LEFT).turn(-1, 360, 4000).signal(TurnSignalState.NONE);
        b.speedTo(40, 4).hold(15_000);
        park(b);
        DriveStats s = run(b);
        assertEquals(1, s.turnsSignaled());
        assertEquals(1, s.earlySignaled());
    }

    @Test
    public void 빠른_속도의_완만한_곡선과_후진_주차는_회전이_아니다() {
        ScenarioBuilder b = start(60);
        b.turn(1, 60, 6000);           // 60km/h에서 핸들 60도: 곡선 도로
        b.hold(5000).speedTo(0, 3).gear(GearState.REVERSE).speedTo(8, 2).turn(-1, 450, 4000);
        park(b);
        DriveStats s = run(b);
        assertEquals(0, s.turns());
    }

    // ── 차로 변경 ──

    @Test
    public void 깜빡이를_켠_차로_변경은_미리_켜기_집계에만_들어간다() {
        ScenarioBuilder b = start(60).hold(5000);
        b.signal(TurnSignalState.RIGHT).hold(4000).turn(1, 8, 3000).hold(500).signal(TurnSignalState.NONE);
        b.hold(15_000);
        b.signal(TurnSignalState.LEFT).hold(1000).turn(-1, 8, 3000).hold(500).signal(TurnSignalState.NONE);
        b.hold(15_000);
        b.turn(1, 8, 3000).hold(15_000); // 깜빡이 없이 차로 변경 → 집계하지 않음(감점 없음)
        park(b);
        DriveStats s = run(b);
        assertEquals(0, s.turns());
        assertEquals(2, s.laneChangesSignaled());
        assertEquals(2, s.earlyEligible());
        assertEquals(1, s.earlySignaled());
    }

    // ── 급조작 ──

    @Test
    public void 급출발_급가속_급감속은_한_번씩만_센다() {
        ScenarioBuilder b = new ScenarioBuilder(0);
        b.gear(GearState.DRIVE);
        b.speedTo(30, 10).hold(3000);                // 급출발
        b.speedTo(60, 8).hold(3000);                 // 급가속
        b.brake(true).speedTo(30, 12).brake(false);  // 급감속(브레이크)
        b.hold(3000).speedTo(10, 10).hold(3000);     // 급감속(브레이크 신호 없음)
        b.speedTo(50, 3).hold(3000).speedTo(20, 4);  // 부드러운 가감속
        park(b);
        DriveStats s = run(b);
        assertEquals(1, s.harshStart());
        assertEquals(1, s.harshAccel());
        assertEquals(2, s.harshDecel());
        assertEquals(1, s.hardBrakes());
    }

    @Test
    public void 빠른_속도에서_핸들을_급하게_꺾으면_급회전() {
        ScenarioBuilder b = start(50).hold(3000);
        b.turn(1, 270, 3000).hold(5000);
        park(b);
        DriveStats s = run(b);
        assertEquals(1, s.harshTurn());
        assertEquals(0, s.turns()); // 40km/h 초과라 교차로 회전으로는 치지 않음
    }

    // ── 빗길 ──

    @Test
    public void 와이퍼를_켠_거리와_그동안의_급조작() {
        ScenarioBuilder b = start(50).wiper(true);
        b.cruiseKm(6);
        b.brake(true).speedTo(30, 10).brake(false).speedTo(50, 4);
        b.wiper(false).cruiseKm(1);
        park(b);
        DriveStats s = run(b);
        assertEquals(6.0, s.wiperMeters() / 1000.0, 0.2);
        assertEquals(1, s.harshWhileWiper());
    }

    // ── 휴식 ──

    @Test
    public void 한시간40분_주행_후_20분_쉬면_휴식_인정() {
        ScenarioBuilder b = start(90).hold(100 * MIN);
        b.speedTo(0, 3).gear(GearState.PARK).pause(20 * MIN).gear(GearState.DRIVE).speedTo(60, 4).hold(10 * MIN);
        park(b);
        DriveStats s = run(b);
        assertTrue(s.longestContinuousMillis() > 100 * MIN);
        assertTrue(s.restTakenInWindow());
        assertFalse(s.exceededRestDeadline());
    }

    @Test
    public void 십분만_쉬면_휴식이_아니고_이어서_2시간을_넘기면_실패() {
        ScenarioBuilder b = start(90).hold(100 * MIN);
        b.speedTo(0, 3).gear(GearState.PARK).pause(10 * MIN).gear(GearState.DRIVE).speedTo(60, 4).hold(30 * MIN);
        park(b);
        DriveStats s = run(b);
        assertFalse(s.restTakenInWindow());
        assertTrue(s.exceededRestDeadline());
    }

    @Test
    public void 시동을_끈_채_신호가_오래_끊기면_휴식으로_본다() {
        ScenarioBuilder b = new ScenarioBuilder(0);
        b.speedTo(80, 4).hold(95 * MIN).speedTo(0, 4); // 기어 신호가 없는 차
        b.pause(30 * MIN).speedTo(40, 4).hold(MIN).speedTo(0, 4);
        DriveStats s = run(b);
        assertTrue(s.restTakenInWindow());
        assertEquals(2, s.tripStarts());
    }

    @Test
    public void 앱을_다시_켜도_연속_주행_시간을_이어_간다() {
        DriveAnalyzer first = new DriveAnalyzer(rules);
        ScenarioBuilder b = start(90).hold(80 * MIN);
        first.acceptAll(b.build());
        DriveStats saved = first.peek();

        DriveAnalyzer resumed = DriveAnalyzer.resume(rules, DriveStats.fromMap(saved.toMap()));
        long t = b.timeMs() + 1000;
        resumed.accept(SignalEvent.gear(t, GearState.DRIVE));
        for (int i = 1; i <= 50 * 60; i++) { // 이어서 50분 동안 1초마다 속도 신호
            resumed.accept(SignalEvent.speed(t + i * 1000L, 90));
        }
        DriveStats s = resumed.closeOut(t + 50 * MIN);
        assertTrue(s.exceededRestDeadline());
    }

    @Test
    public void 날짜가_바뀌면_기록은_새로_시작하고_연속_주행은_이어_간다() {
        DriveAnalyzer a = new DriveAnalyzer(rules);
        ScenarioBuilder b = start(60).hold(30 * MIN);
        a.acceptAll(b.build());
        DriveStats yesterday = a.startNewDay(b.timeMs());
        assertTrue(yesterday.distanceKm() > 25);
        DriveStats today = a.peek();
        assertEquals(0, today.distanceMeters(), 0.0);
        assertTrue(today.continuousMillis() >= 30 * MIN);
    }

    // ── 안전벨트 ──

    @Test
    public void 출발할_때마다_안전벨트를_확인() {
        ScenarioBuilder b = new ScenarioBuilder(0);
        b.gear(GearState.PARK).seatbelt(true).gear(GearState.DRIVE).speedTo(30, 3).hold(MIN);
        park(b);
        b.seatbelt(false).gear(GearState.DRIVE).speedTo(30, 3).hold(MIN);
        park(b);
        DriveStats s = run(b);
        assertEquals(2, s.tripStarts());
        assertEquals(1, s.tripStartsBuckled());
    }

    @Test
    public void 시나리오_CSV로_저장했다_읽어도_같은_결과() throws Exception {
        List<SignalEvent> events = DemoScenarios.COMMUTE.events(0);
        String csv = ScenarioCsv.write(events);
        List<SignalEvent> back = ScenarioCsv.read(new java.io.ByteArrayInputStream(
                csv.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertEquals(events.size(), back.size());

        DriveAnalyzer a = new DriveAnalyzer(rules);
        a.acceptAll(back);
        DriveStats s = a.closeOut(back.get(back.size() - 1).timeMs());
        assertEquals(5, s.turns());
        assertEquals(4, s.turnsSignaled());
    }
}
