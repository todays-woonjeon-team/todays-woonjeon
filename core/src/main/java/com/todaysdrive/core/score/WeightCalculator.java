package com.todaysdrive.core.score;

import com.todaysdrive.core.RuleConfig;
import com.todaysdrive.core.drive.DriveStats;
import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.drive.VehicleSignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 주행 가중치 계산(기획서 3) 주행 평가 항목 표).
 *
 * <pre>
 * 회전 시 방향지시등  100%: +0.2 / 76% 이상: +0.1 / 50% 이상: 0 / 50% 미만: -0.2
 * 방향지시등 미리 켜기 80% 이상: +0.1 / 50% 이상: +0.05
 * 부드러운 주행       10km당 급조작 0회: +0.2 / 1회 이하: +0.1 / 2회 이하: 0 / 4회 이하: -0.1 / 초과: -0.3
 * 빗길 운전           와이퍼 켜고 5km 이상, 그동안 급가속·급감속 없음: +0.05
 * 장거리 휴식         1시간 30분 넘게 연속 주행한 날, 2시간 전에 15분 이상 P단: +0.05
 * </pre>
 * 10km 미만이면 방향지시등 두 항목은 절반만 반영한다. 지원하지 않는 신호에 기대는 항목과
 * 판정할 거리가 없는 항목(예: 회전 0번)은 빼고 계산한다.
 */
public final class WeightCalculator {

    private final RuleConfig rules;

    public WeightCalculator(RuleConfig rules) {
        this.rules = rules;
    }

    public WeightBreakdown calculate(DriveStats s, SignalSupport support) {
        List<WeightItem> items = new ArrayList<>();
        boolean shortTrip = s.distanceMeters() < rules.shortTripMeters;
        boolean canJudgeSignals = support.has(VehicleSignal.TURN_SIGNAL) && support.has(VehicleSignal.STEERING);

        if (canJudgeSignals && s.turns() > 0) {
            int n = s.turns();
            int k = s.turnsSignaled();
            int delta;
            if (k == n) {
                delta = 200;
            } else if (k * 100 >= 76 * n) {
                delta = 100;
            } else if (k * 100 >= 50 * n) {
                delta = 0;
            } else {
                delta = -200;
            }
            items.add(signalItem(WeightItem.Kind.TURN_SIGNAL, delta, n + "번 중 " + k + "번(" + pct(k, n) + "%)", shortTrip));
        }

        if (canJudgeSignals && s.earlyEligible() > 0) {
            int n = s.earlyEligible();
            int k = s.earlySignaled();
            int delta = k * 100 >= 80 * n ? 100 : (k * 100 >= 50 * n ? 50 : 0);
            items.add(signalItem(WeightItem.Kind.EARLY_SIGNAL, delta, n + "번 중 " + k + "번(" + pct(k, n) + "%)", shortTrip));
        }

        if (support.has(VehicleSignal.SPEED) && s.distanceMeters() > 0) {
            int harsh = s.harshTotal();
            double per10km = harsh * 10.0 / s.distanceKm();
            int delta;
            if (harsh == 0) {
                delta = 200;
            } else if (per10km <= 1.0) {
                delta = 100;
            } else if (per10km <= 2.0) {
                delta = 0;
            } else if (per10km <= 4.0) {
                delta = -100;
            } else {
                delta = -300;
            }
            String detail = String.format(Locale.ROOT, "급조작 %d번, 10km당 %.1f번", harsh, per10km);
            items.add(new WeightItem(WeightItem.Kind.SMOOTH, delta, detail, false));
        }

        if (support.has(VehicleSignal.WIPER) && s.wiperMeters() >= rules.rainMinMeters) {
            boolean calm = s.harshWhileWiper() == 0;
            String detail = String.format(Locale.ROOT, "와이퍼 켜고 %.1fkm, 급조작 %d번",
                    s.wiperMeters() / 1000.0, s.harshWhileWiper());
            items.add(new WeightItem(WeightItem.Kind.RAIN, calm ? 50 : 0, detail, false));
        }

        if (support.has(VehicleSignal.GEAR) && s.longestContinuousMillis() > rules.longDriveMillis) {
            boolean ok = restBonusEarned(s);
            String detail = ok ? "2시간 전에 15분 이상 휴식"
                    : (s.exceededRestDeadline() ? "2시간 넘게 연속 주행" : "휴식 없음");
            items.add(new WeightItem(WeightItem.Kind.REST, ok ? 50 : 0, detail, false));
        }

        return new WeightBreakdown(items);
    }

    /** 장거리 휴식 가점 조건. 미션 판정에서도 쓴다. */
    public boolean restBonusEarned(DriveStats s) {
        return s.longestContinuousMillis() > rules.longDriveMillis
                && s.restTakenInWindow()
                && !s.exceededRestDeadline();
    }

    private static WeightItem signalItem(WeightItem.Kind kind, int delta, String detail, boolean shortTrip) {
        return shortTrip && delta != 0
                ? new WeightItem(kind, delta / 2, detail, true)
                : new WeightItem(kind, delta, detail, false);
    }

    static int pct(int part, int whole) {
        return whole == 0 ? 0 : Math.round(part * 100f / whole);
    }
}
