package com.todaysdrive.core.fortune;

import java.time.LocalDate;

/**
 * 출발 전에 보여 주는 오늘의 운세 카드.
 * 운세 문구, 행운 미션 1개, 운전 꿀팁 1개, 응원 한마디로 이루어진다.
 */
public final class FortuneCard {

    private final LocalDate date;
    private final Zodiac zodiac;
    private final FortuneGrade grade;
    private final String fortuneId;
    private final String fortuneText;
    private final MissionType mission;
    private final String missionTitle;
    private final String tip;
    private final String cheer;

    public FortuneCard(LocalDate date, Zodiac zodiac, FortuneGrade grade, String fortuneId,
                       String fortuneText, MissionType mission, String missionTitle,
                       String tip, String cheer) {
        this.date = date;
        this.zodiac = zodiac;
        this.grade = grade;
        this.fortuneId = fortuneId;
        this.fortuneText = fortuneText;
        this.mission = mission;
        this.missionTitle = missionTitle;
        this.tip = tip;
        this.cheer = cheer;
    }

    public LocalDate date() {
        return date;
    }

    /** 생년월일을 입력하지 않았으면 null(그날 모두에게 같은 운세). */
    public Zodiac zodiac() {
        return zodiac;
    }

    public FortuneGrade grade() {
        return grade;
    }

    public String fortuneId() {
        return fortuneId;
    }

    public String fortuneText() {
        return fortuneText;
    }

    public MissionType mission() {
        return mission;
    }

    public String missionTitle() {
        return missionTitle;
    }

    public String tip() {
        return tip;
    }

    public String cheer() {
        return cheer;
    }

    /** 카드 머리말. 예) "용띠 · 길(90점)" */
    public String headline() {
        String prefix = zodiac == null ? "오늘의 운세" : zodiac.label() + "띠";
        return prefix + " · " + grade.label() + "(" + grade.points() + "점)";
    }
}
