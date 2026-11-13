package com.todaysdrive.core.score;

import com.todaysdrive.core.RuleConfig;
import com.todaysdrive.core.drive.DriveStats;
import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.fortune.FortuneCard;
import com.todaysdrive.core.fortune.FortuneGrade;
import com.todaysdrive.core.fortune.MissionType;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 기획서에 나온 계산 예시를 그대로 확인한다. */
public class ScoreCalculatorTest {

    private final ScoreCalculator calc = new ScoreCalculator(RuleConfig.defaults());

    static FortuneCard card(FortuneGrade grade, MissionType mission) {
        return new FortuneCard(LocalDate.of(2026, 11, 10), null, grade, grade.name() + "-1", "운세",
                mission, mission.defaultTitle(), "꿀팁", "응원");
    }

    @Test
    public void 사례1_길90점_가중치1_2면_108점_티켓1장() {
        DriveStats s = DriveStats.builder().distanceKm(12.5).turns(5, 4).early(4, 1).harsh(0, 1, 0, 0).build();
        DailyResult r = calc.calculate(card(FortuneGrade.GIL, MissionType.ALL_TURNS_SIGNALED), s, SignalSupport.all());
        assertEquals(1200, r.weight().totalMilli());
        assertFalse(r.missionAchieved()); // 5번 중 4번이라 아깝게 놓침
        assertEquals(108, r.score());
        assertEquals(1, r.scoreTickets());
    }

    @Test
    public void 사례2_대길이어도_거칠게_몰면_80점_티켓없음() {
        DriveStats s = DriveStats.builder().distanceKm(10.4).turns(5, 2).early(2, 0).harsh(1, 1, 0, 0).build();
        DailyResult r = calc.calculate(card(FortuneGrade.DAEGIL, MissionType.NO_HARD_BRAKE), s, SignalSupport.all());
        assertEquals(800, r.weight().totalMilli());
        assertEquals(80, r.score());
        assertEquals(0, r.scoreTickets());
    }

    @Test
    public void 사례3_소길80점_가중치1_35_미션5점_113점() {
        DriveStats s = DriveStats.builder().distanceKm(150).drivingMinutes(130)
                .turns(25, 23).early(23, 20).harsh(0, 9, 0, 0).rest(100, true, false).build();
        DailyResult r = calc.calculate(card(FortuneGrade.SOGIL, MissionType.REST_BEFORE_2H), s, SignalSupport.all());
        assertEquals(1350, r.weight().totalMilli());
        assertTrue(r.missionAchieved());
        assertEquals(113, r.score());
        assertEquals(1, r.scoreTickets());
    }

    @Test
    public void 점수_범위_예시() {
        assertEquals(120, ScoreCalculator.scoreOf(80, 1500, false));  // 소길이어도 운전을 잘하면 120점
        assertEquals(50, ScoreCalculator.scoreOf(100, 500, false));   // 대길이어도 거칠게 몰면 50점
        assertEquals(155, ScoreCalculator.scoreOf(100, 1500, true));  // 최고점
        assertEquals(95, ScoreCalculator.scoreOf(90, 1050, false));   // 94.5 → 95 (반올림)
    }

    @Test
    public void 하루_3km_미만이면_점수를_계산하지_않는다() {
        DriveStats s = DriveStats.builder().distanceKm(2.9).turns(2, 2).build();
        DailyResult r = calc.calculate(card(FortuneGrade.DAEGIL, MissionType.ALL_TURNS_SIGNALED), s, SignalSupport.all());
        assertFalse(r.scored());
        assertEquals(0, r.scoreTickets());
    }

    @Test
    public void 미션_달성하면_5점() {
        DriveStats s = DriveStats.builder().distanceKm(12).turns(4, 4).early(4, 1).harsh(0, 1, 0, 0).build();
        DailyResult r = calc.calculate(card(FortuneGrade.GIL, MissionType.ALL_TURNS_SIGNALED), s, SignalSupport.all());
        // 1.0 + 0.2(100%) + 0 + 0.1 = 1.3 → 90 × 1.3 = 117 + 5
        assertEquals(1300, r.weight().totalMilli());
        assertEquals(122, r.score());
        assertEquals(1, r.scoreTickets());
    }

    @Test
    public void 미션_판정() {
        MissionJudge judge = new MissionJudge(RuleConfig.defaults());
        assertFalse(judge.achieved(MissionType.ALL_TURNS_SIGNALED, DriveStats.builder().build())); // 회전 0번은 미달성
        assertTrue(judge.achieved(MissionType.EARLY_SIGNAL, DriveStats.builder().early(5, 4).build()));
        assertFalse(judge.achieved(MissionType.NO_HARD_BRAKE, DriveStats.builder().harsh(0, 1, 0, 0).build()));
        assertTrue(judge.achieved(MissionType.SMOOTH_START, DriveStats.builder().tripStarts(2, 0).build()));
        assertFalse(judge.achieved(MissionType.SEATBELT_BEFORE_START, DriveStats.builder().tripStarts(2, 1).build()));
        assertTrue(judge.achieved(MissionType.SEATBELT_BEFORE_START, DriveStats.builder().tripStarts(2, 2).build()));
    }
}
