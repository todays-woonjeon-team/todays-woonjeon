package com.todaysdrive.core.report;

import com.todaysdrive.core.RuleConfig;
import com.todaysdrive.core.drive.DriveStats;
import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.fortune.FortuneCard;
import com.todaysdrive.core.fortune.FortuneGrade;
import com.todaysdrive.core.fortune.MissionType;
import com.todaysdrive.core.link.DailySummary;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DailySummaryFactoryTest {

    private final DailySummaryFactory factory = new DailySummaryFactory(RuleConfig.defaults());
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    private static FortuneCard card(LocalDate date, FortuneGrade grade) {
        return new FortuneCard(date, null, grade, grade.name() + "-1", "운세", MissionType.NO_HARD_BRAKE,
                "급브레이크 없는 하루", "꿀팁", "오늘도 수고했어요");
    }

    private static DriveStats goodDrive() {
        return DriveStats.builder().distanceKm(15).drivingMinutes(30).turns(4, 4).early(4, 4).build();
    }

    @Test
    public void 일곱째_날에_연속_달성_티켓() {
        List<DailySummary> history = new ArrayList<>();
        for (int i = 6; i >= 1; i--) {
            LocalDate d = TODAY.minusDays(i);
            history.add(factory.create(card(d, FortuneGrade.GIL), goodDrive(), SignalSupport.all(), history, 0));
        }
        DailySummary today = factory.create(card(TODAY, FortuneGrade.GIL), goodDrive(), SignalSupport.all(), history, 0);
        // 90 × 1.5 + 5 = 140점 → 점수 티켓 2장 + 연속 보너스 1장
        assertEquals(140, today.score());
        assertEquals(7, today.streakDays());
        assertEquals(1, today.streakBonus());
        assertEquals(3, today.ticketsEarned());
        assertEquals("회전 4번 모두 깜빡이를 미리 켰어요.", today.goodPoint());
        assertEquals("아쉬운 점이 없는 하루였어요.", today.badPoint());
        assertEquals("오늘도 수고했어요", today.cheer());
    }

    @Test
    public void 단거리는_점수_없이_운세만() {
        DriveStats s = DriveStats.builder().distanceKm(1.2).build();
        DailySummary sum = factory.create(card(TODAY, FortuneGrade.DAEGIL), s, SignalSupport.all(),
                Collections.<DailySummary>emptyList(), 0);
        assertFalse(sum.scored());
        assertEquals(0, sum.ticketsEarned());
        assertTrue(sum.goodPoint().contains("1.2km"));
    }

    @Test
    public void 지난주와_비교() {
        List<DailySummary> history = new ArrayList<>();
        DriveStats lateSignals = DriveStats.builder().distanceKm(15).turns(4, 4).early(4, 1).build();
        for (int i = 13; i >= 7; i--) {
            LocalDate d = TODAY.minusDays(i);
            history.add(factory.create(card(d, FortuneGrade.GIL), lateSignals, SignalSupport.all(), history, 0));
        }
        DriveStats earlySignals = DriveStats.builder().distanceKm(15).turns(5, 5).early(5, 4).build();
        DailySummary today = factory.create(card(TODAY, FortuneGrade.GIL), earlySignals, SignalSupport.all(), history, 0);
        assertEquals("미리 켜기 비율이 지난주 25%에서 80%로 올랐어요.", today.weeklyNote());
    }
}
