package com.todaysdrive.core.report;

import com.todaysdrive.core.fortune.FortuneGrade;
import com.todaysdrive.core.link.DailySummary;
import com.todaysdrive.core.score.MissionJudge;
import com.todaysdrive.core.score.WeightBreakdown;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/** 차량·휴대폰 화면이 같이 쓰는 표시 문구 */
public final class ReportTexts {

    private ReportTexts() {
    }

    public static String gradeLabel(String gradeName) {
        try {
            FortuneGrade g = FortuneGrade.valueOf(gradeName);
            return g.label() + " " + g.points() + "점";
        } catch (IllegalArgumentException e) {
            return "운세";
        }
    }

    /** 예) "길 90점 × 1.2 + 미션 5점" */
    public static String formula(DailySummary s) {
        if (!s.scored()) {
            return "3km 미만이라 점수를 계산하지 않았어요";
        }
        return gradeLabel(s.fortuneGrade()) + " × " + WeightBreakdown.format(s.weightMilli())
                + " + 미션 " + (s.missionAchieved() ? MissionJudge.MISSION_BONUS : 0) + "점";
    }

    public static String scoreText(DailySummary s) {
        return s.scored() ? s.score() + "점" : "-";
    }

    /** 예) "티켓 2장을 받았어요 (7일 연속 보너스 포함)" */
    public static String ticketsText(DailySummary s) {
        if (s.ticketsEarned() == 0) {
            return s.scored() ? "100점부터 티켓을 받아요" : "오늘은 티켓이 없어요";
        }
        String text = "티켓 " + s.ticketsEarned() + "장을 받았어요";
        if (s.streakBonus() > 0) {
            text += " (" + s.streakDays() + "일 연속 보너스 포함)";
        }
        return text;
    }

    public static String missionText(DailySummary s) {
        return (s.missionAchieved() ? "미션 성공 · " : "미션 실패 · ") + s.missionTitle();
    }

    /** 예) "12.4km · 28분" */
    public static String driveText(DailySummary s) {
        return String.format(Locale.ROOT, "%.1fkm · %d분", s.distanceKm(), s.driveMinutes());
    }

    /** 예) "11월 10일 (화)" */
    public static String dateText(LocalDate date) {
        return date.getMonthValue() + "월 " + date.getDayOfMonth() + "일 ("
                + date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN) + ")";
    }
}
