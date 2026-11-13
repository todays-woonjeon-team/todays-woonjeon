package com.todaysdrive.core.report;

import com.todaysdrive.core.RuleConfig;
import com.todaysdrive.core.drive.DriveStats;
import com.todaysdrive.core.score.DailyResult;
import com.todaysdrive.core.score.WeightCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 주차 리포트의 "잘한 점 하나, 아쉬운 점 하나"를 고른다.
 * 예) 잘한 점 "회전 5번 중 4번 깜빡이를 켰어요." / 아쉬운 점 "다만 미리 켠 건 4번 중 1번이에요."
 */
public final class ReportWriter {

    private final RuleConfig rules;
    private final WeightCalculator weights;

    public ReportWriter(RuleConfig rules) {
        this.rules = rules;
        this.weights = new WeightCalculator(rules);
    }

    public String goodPoint(DriveStats s, DailyResult result) {
        if (!result.scored()) {
            return String.format(Locale.ROOT, "오늘은 %.1fkm만 달려서 점수 없이 운세만 확인해요.", s.distanceKm());
        }
        int n = s.turns();
        int k = s.turnsSignaled();
        boolean allSignaled = n > 0 && k == n;
        boolean earlyGood = s.earlyEligible() > 0 && s.earlySignaled() * 100 >= 80 * s.earlyEligible();
        double per10 = per10km(s);

        if (allSignaled && earlyGood) {
            return "회전 " + n + "번 모두 깜빡이를 미리 켰어요.";
        }
        if (allSignaled) {
            return "회전 " + n + "번 모두 깜빡이를 켰어요.";
        }
        if (weights.restBonusEarned(s)) {
            return "2시간이 되기 전에 쉬어 갔어요.";
        }
        if (s.wiperMeters() >= rules.rainMinMeters && s.harshWhileWiper() == 0) {
            return String.format(Locale.ROOT, "빗길 %.1fkm를 급조작 없이 달렸어요.", s.wiperMeters() / 1000.0);
        }
        if (s.harshTotal() == 0) {
            return "급가속·급감속 없이 부드럽게 달렸어요.";
        }
        if (earlyGood) {
            return "깜빡이를 미리 켠 비율이 " + pct(s.earlySignaled(), s.earlyEligible()) + "%예요.";
        }
        if (n > 0 && k * 100 >= 76 * n) {
            return "회전 " + n + "번 중 " + k + "번 깜빡이를 켰어요.";
        }
        if (per10 <= 1.0) {
            return String.format(Locale.ROOT, "10km당 급조작이 %.1f번뿐이었어요.", per10);
        }
        return String.format(Locale.ROOT, "오늘도 %.1fkm를 무사히 다녀왔어요.", s.distanceKm());
    }

    public String badPoint(DriveStats s, DailyResult result) {
        if (!result.scored()) {
            return "";
        }
        int n = s.turns();
        int k = s.turnsSignaled();
        if (n > 0 && k * 100 < 50 * n) {
            return "회전 " + n + "번 중 깜빡이를 켠 건 " + k + "번이에요.";
        }
        if (per10km(s) > 2.0) {
            return harshText(s) + " 있었어요.";
        }
        if (s.exceededRestDeadline()) {
            return "2시간 넘게 쉬지 않고 달렸어요.";
        }
        if (s.earlyEligible() > 0 && s.earlySignaled() * 100 < 50 * s.earlyEligible()) {
            return "다만 미리 켠 건 " + s.earlyEligible() + "번 중 " + s.earlySignaled() + "번이에요.";
        }
        if (n > 0 && k < n) {
            return "회전 " + (n - k) + "번은 깜빡이 없이 돌았어요.";
        }
        if (s.harshTotal() > 0) {
            return harshText(s) + " 있었어요.";
        }
        return "아쉬운 점이 없는 하루였어요.";
    }

    private static String harshText(DriveStats s) {
        List<String> parts = new ArrayList<>();
        add(parts, "급가속", s.harshAccel());
        add(parts, "급감속", s.harshDecel());
        add(parts, "급출발", s.harshStart());
        add(parts, "급회전", s.harshTurn());
        return String.join(", ", parts) + "이"; // "급감속 1번이 있었어요"
    }

    private static void add(List<String> parts, String label, int count) {
        if (count > 0) {
            parts.add(label + " " + count + "번");
        }
    }

    private static double per10km(DriveStats s) {
        return s.distanceKm() <= 0 ? 0 : s.harshTotal() * 10.0 / s.distanceKm();
    }

    private static int pct(int part, int whole) {
        return whole == 0 ? 0 : Math.round(part * 100f / whole);
    }
}
