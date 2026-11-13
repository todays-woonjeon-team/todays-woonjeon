package com.todaysdrive.core.fortune;

/**
 * 운세 등급. 좋은 운세만 쓰며, 등급이 그날의 운세 점수가 된다.
 */
public enum FortuneGrade {
    DAEGIL("대길", 100),
    GIL("길", 90),
    SOGIL("소길", 80);

    private final String label;
    private final int points;

    FortuneGrade(String label, int points) {
        this.label = label;
        this.points = points;
    }

    public String label() {
        return label;
    }

    /** 오늘의 점수 계산에 쓰는 운세 점수(80~100). */
    public int points() {
        return points;
    }
}
