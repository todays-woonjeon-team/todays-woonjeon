package com.todaysdrive.core.report;

import com.todaysdrive.core.link.DailySummary;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class ReportTextsTest {

    @Test
    public void 점수_계산식과_티켓_문구() {
        DailySummary s = DailySummary.builder("2026-11-10")
                .score(true, 113, 1350)
                .fortune("SOGIL", "SOGIL-1")
                .mission("REST_BEFORE_2H", "2시간 안에 한 번 쉬어 가기", true)
                .tickets(1, 7, 1)
                .drive(158.04, 131)
                .build();
        assertEquals("소길 80점 × 1.35 + 미션 5점", ReportTexts.formula(s));
        assertEquals("113점", ReportTexts.scoreText(s));
        assertEquals("티켓 2장을 받았어요 (7일 연속 보너스 포함)", ReportTexts.ticketsText(s));
        assertEquals("미션 성공 · 2시간 안에 한 번 쉬어 가기", ReportTexts.missionText(s));
        assertEquals("158.0km · 131분", ReportTexts.driveText(s));
        assertEquals("11월 10일 (화)", ReportTexts.dateText(LocalDate.of(2026, 11, 10)));
    }
}
