package com.todaysdrive.core.drive;

import com.todaysdrive.core.RuleConfig;
import com.todaysdrive.core.fortune.FortuneCard;
import com.todaysdrive.core.fortune.FortuneGrade;
import com.todaysdrive.core.fortune.MissionType;
import com.todaysdrive.core.link.DailySummary;
import com.todaysdrive.core.report.DailySummaryFactory;
import com.todaysdrive.core.score.DailyResult;
import com.todaysdrive.core.score.ScoreCalculator;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 기획서 "이용 사례"를 차량 신호 시나리오로 재현해, 신호 → 판정 → 점수 → 리포트가 기획서 숫자와 같은지 확인한다.
 */
public class DemoScenariosTest {

    private final RuleConfig rules = RuleConfig.defaults();

    private DriveStats analyze(DemoScenarios scenario) {
        List<SignalEvent> events = scenario.events(1_000_000);
        DriveAnalyzer a = new DriveAnalyzer(rules);
        a.acceptAll(events);
        return a.closeOut(events.get(events.size() - 1).timeMs() + 1000);
    }

    private static FortuneCard card(FortuneGrade grade, MissionType mission) {
        return new FortuneCard(LocalDate.of(2026, 11, 10), null, grade, grade.name() + "-1", "운세",
                mission, mission.defaultTitle(), "깜빡이는 방향을 바꾸기 3초 전부터", "오늘도 수고했어요");
    }

    @Test
    public void 사례1_출근길() {
        DriveStats s = analyze(DemoScenarios.COMMUTE);
        assertEquals(12.4, s.distanceKm(), 0.3);
        assertEquals(5, s.turns());
        assertEquals(4, s.turnsSignaled());
        assertEquals(4, s.earlyEligible());
        assertEquals(1, s.earlySignaled());
        assertEquals(1, s.harshDecel());
        assertEquals(1, s.hardBrakes());
        assertEquals(1, s.harshTotal());

        DailySummary sum = new DailySummaryFactory(rules).create(
                card(FortuneGrade.GIL, MissionType.ALL_TURNS_SIGNALED), s, SignalSupport.all(),
                Collections.<DailySummary>emptyList(), 0);
        assertEquals(1200, sum.weightMilli());
        assertEquals(108, sum.score());
        assertEquals(1, sum.ticketsEarned());
        assertEquals("회전 5번 중 4번 깜빡이를 켰어요.", sum.goodPoint());
        assertEquals("다만 미리 켠 건 4번 중 1번이에요.", sum.badPoint());
    }

    @Test
    public void 사례2_서두른_날() {
        DriveStats s = analyze(DemoScenarios.ROUGH);
        assertEquals(5, s.turns());
        assertEquals(2, s.turnsSignaled());
        assertEquals(1, s.harshAccel());
        assertEquals(1, s.harshDecel());
        DailyResult r = new ScoreCalculator(rules).calculate(
                card(FortuneGrade.DAEGIL, MissionType.NO_HARD_BRAKE), s, SignalSupport.all());
        assertEquals(800, r.weight().totalMilli());
        assertEquals(80, r.score());
        assertEquals(0, r.scoreTickets());
    }

    @Test
    public void 빗길_퇴근() {
        DriveStats s = analyze(DemoScenarios.RAINY);
        assertEquals(4, s.turns());
        assertEquals(4, s.earlySignaled());
        assertEquals(0, s.harshTotal());
        assertTrue(s.wiperMeters() >= 5000);
        DailyResult r = new ScoreCalculator(rules).calculate(
                card(FortuneGrade.SOGIL, MissionType.EARLY_SIGNAL), s, SignalSupport.all());
        assertEquals(1550, r.weight().rawMilli());
        assertEquals(1500, r.weight().totalMilli());
        assertEquals(125, r.score()); // 80 × 1.5 + 미션 5점
        assertEquals(1, r.scoreTickets());
    }

    @Test
    public void 사례3_장거리() {
        DriveStats s = analyze(DemoScenarios.LONG_TRIP);
        assertEquals(25, s.turns());
        assertEquals(23, s.turnsSignaled());
        assertEquals(20, s.earlySignaled());
        assertEquals(9, s.harshDecel());
        assertEquals(0, s.harshTotal() - s.harshDecel());
        assertTrue(s.distanceKm() > 140);
        assertTrue(s.restTakenInWindow());

        DailySummary sum = new DailySummaryFactory(rules).create(
                card(FortuneGrade.SOGIL, MissionType.REST_BEFORE_2H), s, SignalSupport.all(),
                Collections.<DailySummary>emptyList(), 0);
        assertEquals(1350, sum.weightMilli());
        assertTrue(sum.missionAchieved());
        assertEquals(113, sum.score());
        assertEquals("2시간이 되기 전에 쉬어 갔어요.", sum.goodPoint());
    }
}
