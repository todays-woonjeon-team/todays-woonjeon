package com.todaysdrive.core.score;

import com.todaysdrive.core.fortune.FortuneCard;

/** 오늘의 점수 계산 결과 */
public final class DailyResult {

    private final FortuneCard card;
    private final boolean scored;
    private final WeightBreakdown weight;
    private final boolean missionAchieved;
    private final int score;
    private final int scoreTickets;

    DailyResult(FortuneCard card, boolean scored, WeightBreakdown weight, boolean missionAchieved,
                int score, int scoreTickets) {
        this.card = card;
        this.scored = scored;
        this.weight = weight;
        this.missionAchieved = missionAchieved;
        this.score = score;
        this.scoreTickets = scoreTickets;
    }

    public FortuneCard card() {
        return card;
    }

    /** 하루 주행이 3km 미만이면 false(점수 없이 운세만 보여 줌) */
    public boolean scored() {
        return scored;
    }

    /** scored가 false면 null */
    public WeightBreakdown weight() {
        return weight;
    }

    public boolean missionAchieved() {
        return missionAchieved;
    }

    /** 오늘의 점수 = 운세 점수 × 주행 가중치 + 미션 5점 (scored가 false면 0) */
    public int score() {
        return score;
    }

    /** 점수로 받는 티켓(0~2장). 연속 달성 보너스는 {@link com.todaysdrive.core.reward.TicketPolicy} 참고 */
    public int scoreTickets() {
        return scoreTickets;
    }
}
