package com.todaysdrive.core.reward;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 룰렛 상품표. 큰 상품 하나보다 자주 당첨되는 작은 상품 위주로 구성한다.
 * 룰렛 칸 크기는 당첨 확률에 비례하게 그리고, 확률표를 화면에 함께 보여 준다.
 */
public final class PrizeTable {

    private final List<Prize> prizes;
    private final int totalWeight;

    public PrizeTable(List<Prize> prizes) {
        if (prizes.isEmpty()) {
            throw new IllegalArgumentException("상품이 없습니다");
        }
        this.prizes = Collections.unmodifiableList(new ArrayList<>(prizes));
        int sum = 0;
        for (Prize p : prizes) {
            sum += p.weight();
        }
        this.totalWeight = sum;
    }

    /** 시연용 기본 상품표(합계 100 = 확률 %) */
    public static PrizeTable defaults() {
        List<Prize> list = new ArrayList<>();
        list.add(new Prize("coffee", "커피 쿠폰", 25, true));
        list.add(new Prize("washer", "워셔액", 12, true));
        list.add(new Prize("air", "차량용 방향제", 12, true));
        list.add(new Prize("pillow", "목베개", 5, true));
        list.add(new Prize("blanket", "차량용 담요", 5, true));
        list.add(new Prize("service", "차량 정비권(엔진오일 교환)", 1, true));
        list.add(new Prize("next", "다음 기회에", 40, false));
        return new PrizeTable(list);
    }

    public List<Prize> prizes() {
        return prizes;
    }

    public int totalWeight() {
        return totalWeight;
    }

    public Prize byId(String id) {
        for (Prize p : prizes) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    public double probability(Prize prize) {
        return (double) prize.weight() / totalWeight;
    }

    /** 확률 공개 문구. 예) "커피 쿠폰 25%" */
    public List<String> probabilityLines() {
        List<String> lines = new ArrayList<>();
        for (Prize p : prizes) {
            lines.add(String.format(Locale.ROOT, "%s %s%%", p.name(), trim(probability(p) * 100)));
        }
        return lines;
    }

    private static String trim(double pct) {
        return pct == Math.rint(pct) ? String.valueOf((long) pct) : String.format(Locale.ROOT, "%.1f", pct);
    }
}
