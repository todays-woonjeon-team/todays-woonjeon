package com.todaysdrive.core.fortune;

import com.todaysdrive.core.util.StableHash;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 날짜와 띠로 그날의 운세 카드를 고른다.
 *
 * <p>같은 날짜·같은 띠·같은 미션 목록이면 어느 기기에서든 같은 카드가 나온다.
 * 생년월일을 넣지 않은 사용자는 그날 모두 같은 운세를 받는다.
 */
public final class FortuneTeller {

    /** 등급 비율. 대길 20%, 길 40%, 소길 40% */
    private static final int[] GRADE_WEIGHTS = {2, 4, 4};

    private final PhraseBook book;

    public FortuneTeller(PhraseBook book) {
        this.book = book;
    }

    public FortuneCard cardFor(LocalDate date, Zodiac zodiac, Set<MissionType> missionPool) {
        if (missionPool == null || missionPool.isEmpty()) {
            throw new IllegalArgumentException("미션 목록이 비어 있습니다");
        }
        String key = date + "|" + (zodiac == null ? "ALL" : zodiac.name());

        FortuneGrade grade = pickGrade(key);
        List<String> phrases = book.fortunes(grade);
        int phraseIndex = StableHash.index(key + "|fortune", phrases.size());

        List<MissionType> missions = new ArrayList<>(missionPool); // EnumSet이면 선언 순서
        MissionType mission = missions.get(StableHash.index(key + "|mission", missions.size()));

        String tip = book.tips().get(StableHash.index(key + "|tip", book.tips().size()));
        String cheer = book.cheers().get(StableHash.index(key + "|cheer", book.cheers().size()));

        String fortuneId = grade.name() + "-" + (phraseIndex + 1);
        return new FortuneCard(date, zodiac, grade, fortuneId, phrases.get(phraseIndex),
                mission, book.missionTitle(mission), tip, cheer);
    }

    private static FortuneGrade pickGrade(String key) {
        int total = 0;
        for (int w : GRADE_WEIGHTS) {
            total += w;
        }
        int roll = StableHash.index(key + "|grade", total);
        FortuneGrade[] grades = FortuneGrade.values();
        for (int i = 0; i < grades.length; i++) {
            if (roll < GRADE_WEIGHTS[i]) {
                return grades[i];
            }
            roll -= GRADE_WEIGHTS[i];
        }
        return FortuneGrade.SOGIL;
    }
}
