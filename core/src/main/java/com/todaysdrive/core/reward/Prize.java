package com.todaysdrive.core.reward;

/** 룰렛 상품. weight는 당첨 비율(가중치). */
public final class Prize {

    private final String id;
    private final String name;
    private final int weight;
    private final boolean win;

    public Prize(String id, String name, int weight, boolean win) {
        if (weight <= 0) {
            throw new IllegalArgumentException("가중치는 1 이상: " + id);
        }
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.win = win;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int weight() {
        return weight;
    }

    /** "다음 기회에"면 false */
    public boolean isWin() {
        return win;
    }
}
