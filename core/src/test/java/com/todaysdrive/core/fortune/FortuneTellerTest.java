package com.todaysdrive.core.fortune;

import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.drive.VehicleSignal;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FortuneTellerTest {

    private final PhraseBook book = PhraseBook.loadDefault();
    private final FortuneTeller teller = new FortuneTeller(book);
    private final Set<MissionType> allMissions = MissionType.poolFor(SignalSupport.all(), false);

    @Test
    public void 같은_날짜와_띠면_어느_기기에서든_같은_카드() {
        LocalDate day = LocalDate.of(2026, 11, 10);
        FortuneCard car = new FortuneTeller(PhraseBook.loadDefault()).cardFor(day, Zodiac.DRAGON, allMissions);
        FortuneCard phone = new FortuneTeller(PhraseBook.loadDefault()).cardFor(day, Zodiac.DRAGON, allMissions);
        assertEquals(car.fortuneId(), phone.fortuneId());
        assertEquals(car.fortuneText(), phone.fortuneText());
        assertEquals(car.mission(), phone.mission());
        assertEquals(car.tip(), phone.tip());
        assertEquals(car.cheer(), phone.cheer());
    }

    @Test
    public void 띠가_다르면_운세가_달라질_수_있다() {
        int differentDays = 0;
        LocalDate day = LocalDate.of(2026, 11, 1);
        for (int i = 0; i < 30; i++) {
            FortuneCard a = teller.cardFor(day.plusDays(i), Zodiac.RAT, allMissions);
            FortuneCard b = teller.cardFor(day.plusDays(i), Zodiac.HORSE, allMissions);
            if (!a.fortuneId().equals(b.fortuneId())) {
                differentDays++;
            }
        }
        assertTrue(differentDays > 15);
    }

    @Test
    public void 좋은_등급만_나오고_비율은_대략_2대4대4() {
        Map<FortuneGrade, Integer> counts = new EnumMap<>(FortuneGrade.class);
        LocalDate day = LocalDate.of(2026, 1, 1);
        int n = 3000;
        for (int i = 0; i < n; i++) {
            FortuneGrade g = teller.cardFor(day.plusDays(i), null, allMissions).grade();
            counts.merge(g, 1, Integer::sum);
        }
        assertEquals(3, counts.size());
        assertRatio(counts.get(FortuneGrade.DAEGIL), n, 0.20);
        assertRatio(counts.get(FortuneGrade.GIL), n, 0.40);
        assertRatio(counts.get(FortuneGrade.SOGIL), n, 0.40);
    }

    private static void assertRatio(int count, int total, double expected) {
        double ratio = (double) count / total;
        assertTrue("비율 " + ratio + " (기대 " + expected + ")", Math.abs(ratio - expected) < 0.04);
    }

    @Test
    public void 안전벨트_신호가_없는_차에는_안전벨트_미션을_내지_않는다() {
        SignalSupport noBelt = SignalSupport.of(VehicleSignal.SPEED, VehicleSignal.GEAR,
                VehicleSignal.STEERING, VehicleSignal.TURN_SIGNAL, VehicleSignal.BRAKE);
        Set<MissionType> pool = MissionType.poolFor(noBelt, false);
        LocalDate day = LocalDate.of(2026, 11, 1);
        for (int i = 0; i < 200; i++) {
            MissionType m = teller.cardFor(day.plusDays(i), null, pool).mission();
            assertNotEquals(MissionType.SEATBELT_BEFORE_START, m);
            assertNotEquals(MissionType.REST_BEFORE_2H, m);
        }
    }

    @Test
    public void 장거리_예정일에만_휴식_미션이_후보가_된다() {
        assertTrue(!MissionType.poolFor(SignalSupport.all(), false).contains(MissionType.REST_BEFORE_2H));
        assertTrue(MissionType.poolFor(SignalSupport.all(), true).contains(MissionType.REST_BEFORE_2H));
    }

    @Test
    public void 기본_문구_파일을_읽는다() {
        assertTrue(book.size() >= 54);
        for (FortuneGrade g : FortuneGrade.values()) {
            assertTrue(book.fortunes(g).size() >= 10);
        }
        assertEquals("급브레이크 없는 하루", book.missionTitle(MissionType.NO_HARD_BRAKE));
    }

    @Test
    public void 문구_파일_형식이_틀리면_줄_번호를_알려준다() {
        String bad = "FORTUNE\tDAEGIL\t좋은 날\nFORTUNE\tUNKNOWN\t문구\n";
        try {
            PhraseBook.load(new ByteArrayInputStream(bad.getBytes(StandardCharsets.UTF_8)));
            fail();
        } catch (IOException e) {
            assertTrue(e.getMessage().startsWith("2번째 줄"));
        }
    }

    @Test
    public void 띠_계산() {
        assertEquals(Zodiac.RAT, Zodiac.fromYear(2020));
        assertEquals(Zodiac.DRAGON, Zodiac.fromYear(2024));
        assertEquals(Zodiac.HORSE, Zodiac.fromYear(2026));
        assertEquals(Zodiac.RABBIT, Zodiac.fromBirthDate(LocalDate.of(2000, 1, 15)));
        assertEquals(Zodiac.DRAGON, Zodiac.fromBirthDate(LocalDate.of(2000, 2, 10)));
    }

    @Test
    public void 카드_머리말() {
        FortuneCard card = teller.cardFor(LocalDate.of(2026, 11, 10), Zodiac.DRAGON, allMissions);
        assertTrue(card.headline().startsWith("용띠 · "));
        FortuneCard common = teller.cardFor(LocalDate.of(2026, 11, 10), null, allMissions);
        assertTrue(common.headline().startsWith("오늘의 운세 · "));
    }
}
