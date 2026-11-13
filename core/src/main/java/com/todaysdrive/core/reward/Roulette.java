package com.todaysdrive.core.reward;

import java.util.Random;

/**
 * 룰렛 당첨 결정. 시연 단계에서는 앱에서 정하고, 실제 상품을 주는 단계부터는 같은 규칙을 서버에서 돌린다.
 */
public final class Roulette {

    private final PrizeTable table;
    private final Random random;

    public Roulette(PrizeTable table, Random random) {
        this.table = table;
        this.random = random;
    }

    /** 당첨된 칸의 번호 */
    public int spinIndex() {
        int roll = random.nextInt(table.totalWeight());
        for (int i = 0; i < table.prizes().size(); i++) {
            int w = table.prizes().get(i).weight();
            if (roll < w) {
                return i;
            }
            roll -= w;
        }
        return table.prizes().size() - 1;
    }

    public Prize spin() {
        return table.prizes().get(spinIndex());
    }

    public PrizeTable table() {
        return table;
    }
}
