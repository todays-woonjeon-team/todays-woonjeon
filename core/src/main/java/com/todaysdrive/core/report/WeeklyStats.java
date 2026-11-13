package com.todaysdrive.core.report;

import com.todaysdrive.core.link.DailySummary;

import java.time.LocalDate;
import java.util.Collection;

/** 일주일 단위 기록. 주간 기록 화면과 "지난주와 비교" 문구에 쓴다. */
public final class WeeklyStats {

    private final LocalDate from;
    private final LocalDate to;
    private int scoredDays;
    private int scoreSum;
    private int turns;
    private int turnsSignaled;
    private int earlyEligible;
    private int earlySignaled;
    private int tickets;
    private double distanceKm;

    private WeeklyStats(LocalDate from, LocalDate to) {
        this.from = from;
        this.to = to;
    }

    /** endInclusive를 마지막 날로 하는 7일 */
    public static WeeklyStats weekEnding(LocalDate endInclusive, Collection<DailySummary> days) {
        WeeklyStats w = new WeeklyStats(endInclusive.minusDays(6), endInclusive);
        for (DailySummary d : days) {
            LocalDate date = d.localDate();
            if (date.isBefore(w.from) || date.isAfter(w.to)) {
                continue;
            }
            if (d.scored()) {
                w.scoredDays++;
                w.scoreSum += d.score();
            }
            w.turns += d.turns();
            w.turnsSignaled += d.turnsSignaled();
            w.earlyEligible += d.earlyEligible();
            w.earlySignaled += d.earlySignaled();
            w.tickets += d.ticketsEarned();
            w.distanceKm += d.distanceKm();
        }
        return w;
    }

    public LocalDate from() {
        return from;
    }

    public LocalDate to() {
        return to;
    }

    public int scoredDays() {
        return scoredDays;
    }

    /** 점수를 계산한 날의 평균. 없으면 -1 */
    public int averageScore() {
        return scoredDays == 0 ? -1 : Math.round((float) scoreSum / scoredDays);
    }

    /** 회전 시 점등률(%). 회전이 없으면 -1 */
    public int signalRatePct() {
        return turns == 0 ? -1 : Math.round(turnsSignaled * 100f / turns);
    }

    /** 미리 켜기 비율(%). 대상이 없으면 -1 */
    public int earlyRatePct() {
        return earlyEligible == 0 ? -1 : Math.round(earlySignaled * 100f / earlyEligible);
    }

    public int tickets() {
        return tickets;
    }

    public double distanceKm() {
        return distanceKm;
    }

    /**
     * 지난주와 비교한 한 줄. 미리 켜기 비율이 10%p 이상 달라졌으면 그것을, 아니면 평균 점수 변화를 말한다.
     * 비교할 기록이 없으면 빈 문자열.
     */
    public static String compareText(WeeklyStats thisWeek, WeeklyStats lastWeek) {
        int nowEarly = thisWeek.earlyRatePct();
        int prevEarly = lastWeek.earlyRatePct();
        if (nowEarly >= 0 && prevEarly >= 0 && Math.abs(nowEarly - prevEarly) >= 10) {
            return "미리 켜기 비율이 지난주 " + prevEarly + "%에서 " + nowEarly + "%로 "
                    + (nowEarly > prevEarly ? "올랐어요." : "내려갔어요.");
        }
        int now = thisWeek.averageScore();
        int prev = lastWeek.averageScore();
        if (now < 0 || prev < 0) {
            return "";
        }
        int diff = now - prev;
        if (diff == 0) {
            return "지난주와 평균 점수가 같아요(" + now + "점).";
        }
        return "지난주보다 평균 점수가 " + Math.abs(diff) + "점 " + (diff > 0 ? "올랐어요." : "내려갔어요.");
    }
}
