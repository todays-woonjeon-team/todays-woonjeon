package com.todaysdrive.core.reward;

import org.junit.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RewardTest {

    private final TicketPolicy policy = new TicketPolicy();

    @Test
    public void 점수별_티켓_하루_최대_2장() {
        assertEquals(0, policy.scoreTickets(99));
        assertEquals(1, policy.scoreTickets(100));
        assertEquals(1, policy.scoreTickets(129));
        assertEquals(2, policy.scoreTickets(130));
        assertEquals(2, policy.scoreTickets(155));
    }

    @Test
    public void 칠일_연속_100점이면_1장_추가() {
        LocalDate today = LocalDate.of(2026, 11, 10);
        Map<LocalDate, Integer> past = new HashMap<>();
        for (int i = 1; i <= 6; i++) {
            past.put(today.minusDays(i), 100 + i);
        }
        int streak = policy.streakIncludingToday(today, 104, past);
        assertEquals(7, streak);
        assertEquals(1, policy.streakBonus(streak));
        assertEquals(0, policy.streakBonus(6));
        assertEquals(1, policy.streakBonus(14));

        past.put(today.minusDays(3), 95); // 중간에 한 번 끊김
        assertEquals(3, policy.streakIncludingToday(today, 104, past));
        assertEquals(0, policy.streakIncludingToday(today, 99, past));
    }

    @Test
    public void 룰렛은_상품표_비율대로_당첨() {
        PrizeTable table = PrizeTable.defaults();
        assertEquals(100, table.totalWeight());
        Roulette roulette = new Roulette(table, new Random(42));
        int[] counts = new int[table.prizes().size()];
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            counts[roulette.spinIndex()]++;
        }
        for (int i = 0; i < counts.length; i++) {
            double expected = table.probability(table.prizes().get(i));
            assertEquals(expected, (double) counts[i] / n, 0.01);
        }
        assertEquals("커피 쿠폰 25%", table.probabilityLines().get(0));
    }

    @Test
    public void 티켓_장부() {
        TicketLedger ledger = new TicketLedger();
        ledger.grant("2026-11-09", 1);
        ledger.grant("2026-11-10", 2);
        ledger.grant("2026-11-10", 2); // 같은 날을 다시 올려도 두 번 쌓이지 않음
        assertEquals(3, ledger.balance());

        Prize coffee = PrizeTable.defaults().byId("coffee");
        ledger.spend("s1", coffee, 1000, "car");
        ledger.spend("s2", coffee, 2000, "phone");
        ledger.spend("s3", coffee, 3000, "phone");
        assertEquals(0, ledger.balance());
        try {
            ledger.spend("s4", coffee, 4000, "car");
            fail();
        } catch (IllegalStateException expected) {
            // 남은 티켓 없음
        }

        TicketLedger copy = TicketLedger.fromMap(ledger.toMap());
        assertEquals(3, copy.granted());
        assertEquals(3, copy.used());
        assertEquals("s3", copy.spins().get(0).id); // 최근 순
        assertTrue(copy.spins().get(0).win);
    }
}
