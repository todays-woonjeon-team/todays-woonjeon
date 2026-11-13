package com.todaysdrive.core.score;

import com.todaysdrive.core.RuleConfig;
import com.todaysdrive.core.drive.DriveStats;
import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.drive.VehicleSignal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 기획서 "주행 평가 항목" 표의 구간을 하나씩 확인한다. */
public class WeightCalculatorTest {

    private final WeightCalculator calc = new WeightCalculator(RuleConfig.defaults());
    private final SignalSupport all = SignalSupport.all();

    private int delta(DriveStats s, WeightItem.Kind kind) {
        WeightItem item = calc.calculate(s, all).item(kind);
        return item == null ? Integer.MIN_VALUE : item.deltaMilli();
    }

    private static DriveStats.Builder longDay() {
        return DriveStats.builder().distanceKm(20);
    }

    @Test
    public void 회전_시_방향지시등_구간() {
        assertEquals(200, delta(longDay().turns(5, 5).build(), WeightItem.Kind.TURN_SIGNAL));
        assertEquals(100, delta(longDay().turns(5, 4).build(), WeightItem.Kind.TURN_SIGNAL));   // 80%
        assertEquals(100, delta(longDay().turns(25, 19).build(), WeightItem.Kind.TURN_SIGNAL)); // 76%
        assertEquals(0, delta(longDay().turns(4, 3).build(), WeightItem.Kind.TURN_SIGNAL));     // 75%
        assertEquals(0, delta(longDay().turns(2, 1).build(), WeightItem.Kind.TURN_SIGNAL));     // 50%
        assertEquals(-200, delta(longDay().turns(5, 2).build(), WeightItem.Kind.TURN_SIGNAL));  // 40%
    }

    @Test
    public void 회전이_없으면_방향지시등_항목을_빼고_계산() {
        WeightBreakdown w = calc.calculate(longDay().build(), all);
        assertNull(w.item(WeightItem.Kind.TURN_SIGNAL));
        assertNull(w.item(WeightItem.Kind.EARLY_SIGNAL));
    }

    @Test
    public void 미리_켜기_구간() {
        assertEquals(100, delta(longDay().turns(5, 5).early(5, 4).build(), WeightItem.Kind.EARLY_SIGNAL)); // 80%
        assertEquals(50, delta(longDay().turns(2, 2).early(2, 1).build(), WeightItem.Kind.EARLY_SIGNAL));  // 50%
        assertEquals(0, delta(longDay().turns(4, 4).early(4, 1).build(), WeightItem.Kind.EARLY_SIGNAL));   // 25%
    }

    @Test
    public void 부드러운_주행_구간_10km당() {
        assertEquals(200, delta(DriveStats.builder().distanceKm(12).build(), WeightItem.Kind.SMOOTH));
        assertEquals(100, delta(DriveStats.builder().distanceKm(12.5).harsh(0, 1, 0, 0).build(), WeightItem.Kind.SMOOTH));
        assertEquals(0, delta(DriveStats.builder().distanceKm(10).harsh(1, 1, 0, 0).build(), WeightItem.Kind.SMOOTH));
        assertEquals(-100, delta(DriveStats.builder().distanceKm(10).harsh(1, 1, 1, 1).build(), WeightItem.Kind.SMOOTH));
        assertEquals(-300, delta(DriveStats.builder().distanceKm(10).harsh(2, 2, 1, 0).build(), WeightItem.Kind.SMOOTH));
        // 오래 달린 사람이 불리하지 않음: 150km에 9번 = 10km당 0.6번
        assertEquals(100, delta(DriveStats.builder().distanceKm(150).harsh(0, 9, 0, 0).build(), WeightItem.Kind.SMOOTH));
    }

    @Test
    public void 빗길_운전() {
        assertEquals(50, delta(longDay().wiper(5, 0).build(), WeightItem.Kind.RAIN));
        assertEquals(0, delta(longDay().wiper(6, 1).build(), WeightItem.Kind.RAIN));
        assertEquals(Integer.MIN_VALUE, delta(longDay().wiper(4.9, 0).build(), WeightItem.Kind.RAIN));
    }

    @Test
    public void 장거리_휴식() {
        assertEquals(50, delta(longDay().rest(100, true, false).build(), WeightItem.Kind.REST));
        assertEquals(0, delta(longDay().rest(130, true, true).build(), WeightItem.Kind.REST));
        assertEquals(0, delta(longDay().rest(100, false, false).build(), WeightItem.Kind.REST));
        assertEquals(Integer.MIN_VALUE, delta(longDay().rest(80, false, false).build(), WeightItem.Kind.REST));
    }

    @Test
    public void 단거리는_방향지시등_항목을_절반만_반영() {
        DriveStats s = DriveStats.builder().distanceKm(8).turns(3, 3).early(3, 3).build();
        WeightBreakdown w = calc.calculate(s, all);
        assertEquals(100, w.item(WeightItem.Kind.TURN_SIGNAL).deltaMilli());
        assertTrue(w.item(WeightItem.Kind.TURN_SIGNAL).halved());
        assertEquals(50, w.item(WeightItem.Kind.EARLY_SIGNAL).deltaMilli());
        assertEquals(200, w.item(WeightItem.Kind.SMOOTH).deltaMilli()); // 급조작 항목은 그대로
        assertEquals(1350, w.totalMilli());

        DriveStats bad = DriveStats.builder().distanceKm(8).turns(4, 1).build();
        assertEquals(-100, calc.calculate(bad, all).item(WeightItem.Kind.TURN_SIGNAL).deltaMilli());
    }

    @Test
    public void 가중치는_0_5에서_1_5로_제한() {
        DriveStats best = DriveStats.builder().distanceKm(20).turns(5, 5).early(5, 5)
                .wiper(10, 0).rest(100, true, false).build();
        WeightBreakdown w = calc.calculate(best, all);
        assertEquals(1600, w.rawMilli());
        assertEquals(1500, w.totalMilli());
        assertTrue(w.clamped());

        DriveStats worst = DriveStats.builder().distanceKm(10).turns(5, 0).harsh(5, 5, 0, 0).build();
        assertEquals(500, calc.calculate(worst, all).totalMilli());
    }

    @Test
    public void 지원하지_않는_신호의_항목은_빠진다() {
        SignalSupport speedOnly = SignalSupport.of(VehicleSignal.SPEED, VehicleSignal.GEAR);
        DriveStats s = longDay().turns(5, 0).wiper(10, 0).build();
        WeightBreakdown w = calc.calculate(s, speedOnly);
        assertNull(w.item(WeightItem.Kind.TURN_SIGNAL));
        assertNull(w.item(WeightItem.Kind.RAIN));
        assertEquals(1200, w.totalMilli());
    }

    @Test
    public void 리포트_문구() {
        WeightBreakdown w = calc.calculate(DriveStats.builder().distanceKm(12.5).turns(5, 4).build(), all);
        assertEquals("회전 시 방향지시등 · 5번 중 4번(80%) · +0.1", w.item(WeightItem.Kind.TURN_SIGNAL).line());
        assertEquals("1.35", WeightBreakdown.format(1350));
        assertEquals("-0.2", WeightBreakdown.formatDelta(-200));
        assertEquals("+0.05", WeightBreakdown.formatDelta(50));
        assertFalse(w.clamped());
    }
}
