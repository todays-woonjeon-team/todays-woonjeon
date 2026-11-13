package com.todaysdrive.core.score;

import com.todaysdrive.core.RuleConfig;
import com.todaysdrive.core.drive.DriveStats;
import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.fortune.FortuneCard;
import com.todaysdrive.core.reward.TicketPolicy;

/**
 * 오늘의 점수 = 운세 점수(80~100) × 주행 가중치(0.5~1.5) + 미션 달성 5점.
 *
 * <p>예) 길(90) × 1.2 = 108점, 소길(80) × 1.35 + 5 = 113점, 대길(100) × 0.8 = 80점
 */
public final class ScoreCalculator {

    private final RuleConfig rules;
    private final WeightCalculator weights;
    private final MissionJudge missions;
    private final TicketPolicy tickets;

    public ScoreCalculator(RuleConfig rules) {
        this.rules = rules;
        this.weights = new WeightCalculator(rules);
        this.missions = new MissionJudge(rules);
        this.tickets = new TicketPolicy();
    }

    public DailyResult calculate(FortuneCard card, DriveStats stats, SignalSupport support) {
        if (stats.distanceMeters() < rules.minScoreMeters) {
            return new DailyResult(card, false, null, false, 0, 0);
        }
        WeightBreakdown weight = weights.calculate(stats, support);
        boolean mission = missions.achieved(card.mission(), stats);
        int score = scoreOf(card.grade().points(), weight.totalMilli(), mission);
        return new DailyResult(card, true, weight, mission, score, tickets.scoreTickets(score));
    }

    /** 반올림은 0.5 이상 올림. 가중치는 천분율 정수라 소수 오차가 없다. */
    public static int scoreOf(int fortunePoints, int weightMilli, boolean missionAchieved) {
        int base = (fortunePoints * weightMilli + 500) / 1000;
        return base + (missionAchieved ? MissionJudge.MISSION_BONUS : 0);
    }
}
