package com.todaysdrive.core.report;

import com.todaysdrive.core.RuleConfig;
import com.todaysdrive.core.drive.DriveStats;
import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.fortune.FortuneCard;
import com.todaysdrive.core.link.DailySummary;
import com.todaysdrive.core.reward.TicketPolicy;
import com.todaysdrive.core.score.DailyResult;
import com.todaysdrive.core.score.ScoreCalculator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 운세 카드와 오늘 주행 기록으로 주차 리포트(하루 결과 요약)를 만든다.
 * 차량 앱이 P단에 들어갈 때마다 다시 만들어 저장한다.
 */
public final class DailySummaryFactory {

    private final ScoreCalculator scores;
    private final ReportWriter writer;
    private final TicketPolicy tickets = new TicketPolicy();

    public DailySummaryFactory(RuleConfig rules) {
        this.scores = new ScoreCalculator(rules);
        this.writer = new ReportWriter(rules);
    }

    public DailyResult score(FortuneCard card, DriveStats stats, SignalSupport support) {
        return scores.calculate(card, stats, support);
    }

    /**
     * @param history 지난 날의 요약(오늘 날짜가 섞여 있어도 무시함). 연속 달성과 지난주 비교에 쓴다.
     */
    public DailySummary create(FortuneCard card, DriveStats stats, SignalSupport support,
                               List<DailySummary> history, long nowMs) {
        LocalDate today = card.date();
        DailyResult result = scores.calculate(card, stats, support);

        Map<LocalDate, Integer> past = new HashMap<>();
        List<DailySummary> all = new ArrayList<>();
        for (DailySummary d : history) {
            LocalDate date = d.localDate();
            if (date.equals(today)) {
                continue;
            }
            all.add(d);
            if (d.scored()) {
                past.put(date, d.score());
            }
        }
        int streak = result.scored() ? tickets.streakIncludingToday(today, result.score(), past) : 0;
        int bonus = tickets.streakBonus(streak);

        String good = writer.goodPoint(stats, result);
        String bad = writer.badPoint(stats, result);

        DailySummary.Builder b = DailySummary.builder(today.toString())
                .score(result.scored(), result.score(),
                        result.scored() ? result.weight().totalMilli() : 1000)
                .fortune(card.grade().name(), card.fortuneId())
                .mission(card.mission().name(), card.missionTitle(), result.missionAchieved())
                .tickets(result.scoreTickets(), streak, bonus)
                .drive(stats.distanceKm(), (int) (stats.drivingMillis() / 60_000L))
                .counts(stats.turns(), stats.turnsSignaled(), stats.earlyEligible(),
                        stats.earlySignaled(), stats.harshTotal())
                .updatedAt(nowMs);
        if (result.scored()) {
            b.weightLines(result.weight().lines());
        }

        DailySummary draft = b.report(good, bad, card.cheer(), "").build();
        all.add(draft);
        String weekly = WeeklyStats.compareText(
                WeeklyStats.weekEnding(today, all),
                WeeklyStats.weekEnding(today.minusDays(7), all));
        return b.report(good, bad, card.cheer(), weekly).build();
    }
}
