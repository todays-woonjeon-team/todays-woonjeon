package com.todaysdrive.core.score;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 주행 가중치 계산 결과(기본 1.0 + 항목별 가감, 0.5~1.5로 제한) */
public final class WeightBreakdown {

    public static final int BASE_MILLI = 1000;
    public static final int MIN_MILLI = 500;
    public static final int MAX_MILLI = 1500;

    private final List<WeightItem> items;
    private final int rawMilli;
    private final int totalMilli;

    WeightBreakdown(List<WeightItem> items) {
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        int sum = BASE_MILLI;
        for (WeightItem item : items) {
            sum += item.deltaMilli();
        }
        this.rawMilli = sum;
        this.totalMilli = Math.max(MIN_MILLI, Math.min(MAX_MILLI, sum));
    }

    public List<WeightItem> items() {
        return items;
    }

    /** 제한 전 합계 */
    public int rawMilli() {
        return rawMilli;
    }

    /** 최종 가중치(천분율, 500~1500) */
    public int totalMilli() {
        return totalMilli;
    }

    public boolean clamped() {
        return rawMilli != totalMilli;
    }

    public WeightItem item(WeightItem.Kind kind) {
        for (WeightItem i : items) {
            if (i.kind() == kind) {
                return i;
            }
        }
        return null;
    }

    public List<String> lines() {
        List<String> lines = new ArrayList<>();
        for (WeightItem i : items) {
            lines.add(i.line());
        }
        return lines;
    }

    /** 1350 → "1.35" */
    public static String format(int milli) {
        return BigDecimal.valueOf(milli, 3).stripTrailingZeros().toPlainString();
    }

    /** 100 → "+0.1", -200 → "-0.2", 0 → "0" */
    public static String formatDelta(int milli) {
        if (milli == 0) {
            return "0";
        }
        return (milli > 0 ? "+" : "") + format(milli);
    }
}
