package com.todaysdrive.core.link;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.todaysdrive.core.link.Json.asBool;
import static com.todaysdrive.core.link.Json.asDouble;
import static com.todaysdrive.core.link.Json.asInt;
import static com.todaysdrive.core.link.Json.asList;
import static com.todaysdrive.core.link.Json.asLong;
import static com.todaysdrive.core.link.Json.asString;

/**
 * 하루 결과 요약. 차량 앱이 저장하고, 사용자가 연동에 동의했을 때만 서버에 올린다.
 *
 * <p>담는 것: 점수, 티켓 수, 잘한 점·아쉬운 점, 주행 거리·시간, 주간 기록용 횟수.
 * 담지 않는 것: 위치, 주행 원본 신호, 생년월일, 이름·연락처.
 */
public final class DailySummary {

    private final String date;
    private final boolean scored;
    private final int score;
    private final int weightMilli;
    private final String fortuneGrade;
    private final String fortuneId;
    private final String mission;
    private final String missionTitle;
    private final boolean missionAchieved;
    private final int scoreTickets;
    private final int streakDays;
    private final int streakBonus;
    private final String goodPoint;
    private final String badPoint;
    private final String cheer;
    private final String weeklyNote;
    private final List<String> weightLines;
    private final double distanceKm;
    private final int driveMinutes;
    private final int turns;
    private final int turnsSignaled;
    private final int earlyEligible;
    private final int earlySignaled;
    private final int harshCount;
    private final long updatedAt;

    private DailySummary(Builder b) {
        date = b.date;
        scored = b.scored;
        score = b.score;
        weightMilli = b.weightMilli;
        fortuneGrade = b.fortuneGrade;
        fortuneId = b.fortuneId;
        mission = b.mission;
        missionTitle = b.missionTitle;
        missionAchieved = b.missionAchieved;
        scoreTickets = b.scoreTickets;
        streakDays = b.streakDays;
        streakBonus = b.streakBonus;
        goodPoint = b.goodPoint;
        badPoint = b.badPoint;
        cheer = b.cheer;
        weeklyNote = b.weeklyNote;
        weightLines = Collections.unmodifiableList(new ArrayList<>(b.weightLines));
        distanceKm = Math.round(b.distanceKm * 10) / 10.0;
        driveMinutes = b.driveMinutes;
        turns = b.turns;
        turnsSignaled = b.turnsSignaled;
        earlyEligible = b.earlyEligible;
        earlySignaled = b.earlySignaled;
        harshCount = b.harshCount;
        updatedAt = b.updatedAt;
    }

    public static Builder builder(String date) {
        return new Builder(date);
    }

    public String date() {
        return date;
    }

    public LocalDate localDate() {
        return LocalDate.parse(date);
    }

    public boolean scored() {
        return scored;
    }

    public int score() {
        return score;
    }

    public int weightMilli() {
        return weightMilli;
    }

    public String fortuneGrade() {
        return fortuneGrade;
    }

    public String fortuneId() {
        return fortuneId;
    }

    public String mission() {
        return mission;
    }

    public String missionTitle() {
        return missionTitle;
    }

    public boolean missionAchieved() {
        return missionAchieved;
    }

    public int scoreTickets() {
        return scoreTickets;
    }

    public int streakDays() {
        return streakDays;
    }

    public int streakBonus() {
        return streakBonus;
    }

    /** 그날 받은 티켓 합계(점수 티켓 + 연속 달성 보너스) */
    public int ticketsEarned() {
        return scoreTickets + streakBonus;
    }

    public String goodPoint() {
        return goodPoint;
    }

    public String badPoint() {
        return badPoint;
    }

    public String cheer() {
        return cheer;
    }

    public String weeklyNote() {
        return weeklyNote;
    }

    public List<String> weightLines() {
        return weightLines;
    }

    public double distanceKm() {
        return distanceKm;
    }

    public int driveMinutes() {
        return driveMinutes;
    }

    public int turns() {
        return turns;
    }

    public int turnsSignaled() {
        return turnsSignaled;
    }

    public int earlyEligible() {
        return earlyEligible;
    }

    public int earlySignaled() {
        return earlySignaled;
    }

    public int harshCount() {
        return harshCount;
    }

    public long updatedAt() {
        return updatedAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", date);
        m.put("scored", scored);
        m.put("score", score);
        m.put("weightMilli", weightMilli);
        m.put("fortuneGrade", fortuneGrade);
        m.put("fortuneId", fortuneId);
        m.put("mission", mission);
        m.put("missionTitle", missionTitle);
        m.put("missionAchieved", missionAchieved);
        m.put("scoreTickets", scoreTickets);
        m.put("streakDays", streakDays);
        m.put("streakBonus", streakBonus);
        m.put("goodPoint", goodPoint);
        m.put("badPoint", badPoint);
        m.put("cheer", cheer);
        m.put("weeklyNote", weeklyNote);
        m.put("weightLines", new ArrayList<>(weightLines));
        m.put("distanceKm", distanceKm);
        m.put("driveMinutes", driveMinutes);
        m.put("turns", turns);
        m.put("turnsSignaled", turnsSignaled);
        m.put("earlyEligible", earlyEligible);
        m.put("earlySignaled", earlySignaled);
        m.put("harshCount", harshCount);
        m.put("updatedAt", updatedAt);
        return m;
    }

    public String toJson() {
        return Json.write(toMap());
    }

    public static DailySummary fromMap(Map<String, Object> m) {
        Builder b = new Builder(asString(m.get("date"), "1970-01-01"));
        b.scored = asBool(m.get("scored"), false);
        b.score = asInt(m.get("score"), 0);
        b.weightMilli = asInt(m.get("weightMilli"), 1000);
        b.fortuneGrade = asString(m.get("fortuneGrade"), "");
        b.fortuneId = asString(m.get("fortuneId"), "");
        b.mission = asString(m.get("mission"), "");
        b.missionTitle = asString(m.get("missionTitle"), "");
        b.missionAchieved = asBool(m.get("missionAchieved"), false);
        b.scoreTickets = asInt(m.get("scoreTickets"), 0);
        b.streakDays = asInt(m.get("streakDays"), 0);
        b.streakBonus = asInt(m.get("streakBonus"), 0);
        b.goodPoint = asString(m.get("goodPoint"), "");
        b.badPoint = asString(m.get("badPoint"), "");
        b.cheer = asString(m.get("cheer"), "");
        b.weeklyNote = asString(m.get("weeklyNote"), "");
        for (Object line : asList(m.get("weightLines"))) {
            b.weightLines.add(String.valueOf(line));
        }
        b.distanceKm = asDouble(m.get("distanceKm"), 0);
        b.driveMinutes = asInt(m.get("driveMinutes"), 0);
        b.turns = asInt(m.get("turns"), 0);
        b.turnsSignaled = asInt(m.get("turnsSignaled"), 0);
        b.earlyEligible = asInt(m.get("earlyEligible"), 0);
        b.earlySignaled = asInt(m.get("earlySignaled"), 0);
        b.harshCount = asInt(m.get("harshCount"), 0);
        b.updatedAt = asLong(m.get("updatedAt"), 0);
        return b.build();
    }

    public static DailySummary fromJson(String json) {
        return fromMap(Json.parseObject(json));
    }

    public static final class Builder {
        private final String date;
        private boolean scored;
        private int score;
        private int weightMilli = 1000;
        private String fortuneGrade = "";
        private String fortuneId = "";
        private String mission = "";
        private String missionTitle = "";
        private boolean missionAchieved;
        private int scoreTickets;
        private int streakDays;
        private int streakBonus;
        private String goodPoint = "";
        private String badPoint = "";
        private String cheer = "";
        private String weeklyNote = "";
        private final List<String> weightLines = new ArrayList<>();
        private double distanceKm;
        private int driveMinutes;
        private int turns;
        private int turnsSignaled;
        private int earlyEligible;
        private int earlySignaled;
        private int harshCount;
        private long updatedAt;

        private Builder(String date) {
            this.date = date;
        }

        public Builder score(boolean scored, int score, int weightMilli) {
            this.scored = scored;
            this.score = score;
            this.weightMilli = weightMilli;
            return this;
        }

        public Builder fortune(String grade, String fortuneId) {
            this.fortuneGrade = grade;
            this.fortuneId = fortuneId;
            return this;
        }

        public Builder mission(String mission, String title, boolean achieved) {
            this.mission = mission;
            this.missionTitle = title;
            this.missionAchieved = achieved;
            return this;
        }

        public Builder tickets(int scoreTickets, int streakDays, int streakBonus) {
            this.scoreTickets = scoreTickets;
            this.streakDays = streakDays;
            this.streakBonus = streakBonus;
            return this;
        }

        public Builder report(String good, String bad, String cheer, String weeklyNote) {
            this.goodPoint = good;
            this.badPoint = bad;
            this.cheer = cheer;
            this.weeklyNote = weeklyNote;
            return this;
        }

        public Builder weightLines(List<String> lines) {
            this.weightLines.clear();
            this.weightLines.addAll(lines);
            return this;
        }

        public Builder drive(double distanceKm, int driveMinutes) {
            this.distanceKm = distanceKm;
            this.driveMinutes = driveMinutes;
            return this;
        }

        public Builder counts(int turns, int turnsSignaled, int earlyEligible, int earlySignaled, int harshCount) {
            this.turns = turns;
            this.turnsSignaled = turnsSignaled;
            this.earlyEligible = earlyEligible;
            this.earlySignaled = earlySignaled;
            this.harshCount = harshCount;
            return this;
        }

        public Builder updatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public DailySummary build() {
            return new DailySummary(this);
        }
    }
}
