package com.todaysdrive.core.reward;

import java.time.LocalDate;
import java.util.Map;

/**
 * 룰렛 티켓 규칙.
 * <ul>
 *   <li>오늘의 점수 100점 이상 1장, 130점 이상 2장(점수 티켓은 하루 최대 2장)</li>
 *   <li>100점 이상인 날이 7일 연속되면 1장 추가(7일, 14일, 21일…째 되는 날)</li>
 * </ul>
 */
public final class TicketPolicy {

    public static final int ONE_TICKET_SCORE = 100;
    public static final int TWO_TICKET_SCORE = 130;
    public static final int STREAK_DAYS = 7;

    public int scoreTickets(int score) {
        if (score >= TWO_TICKET_SCORE) {
            return 2;
        }
        if (score >= ONE_TICKET_SCORE) {
            return 1;
        }
        return 0;
    }

    public int streakBonus(int streakDays) {
        return streakDays > 0 && streakDays % STREAK_DAYS == 0 ? 1 : 0;
    }

    /**
     * 오늘까지 100점 이상이 며칠 연속인지. 오늘 100점 미만이면 0.
     *
     * @param pastScores 지난 날짜별 점수(점수를 계산하지 않은 날은 넣지 않음)
     */
    public int streakIncludingToday(LocalDate today, int todayScore, Map<LocalDate, Integer> pastScores) {
        if (todayScore < ONE_TICKET_SCORE) {
            return 0;
        }
        int streak = 1;
        LocalDate d = today.minusDays(1);
        while (true) {
            Integer s = pastScores.get(d);
            if (s == null || s < ONE_TICKET_SCORE) {
                return streak;
            }
            streak++;
            d = d.minusDays(1);
        }
    }
}
