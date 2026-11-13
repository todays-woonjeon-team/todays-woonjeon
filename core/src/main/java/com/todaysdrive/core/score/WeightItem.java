package com.todaysdrive.core.score;

/**
 * 주행 가중치 항목 하나. 가중치는 1000 = 1.0인 정수(천분율)로 다뤄 소수 오차를 없앤다.
 */
public final class WeightItem {

    public enum Kind {
        TURN_SIGNAL("회전 시 방향지시등"),
        EARLY_SIGNAL("방향지시등 미리 켜기"),
        SMOOTH("부드러운 주행"),
        RAIN("빗길 운전"),
        REST("장거리 휴식");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final Kind kind;
    private final int deltaMilli;
    private final String detail;
    private final boolean halved;

    WeightItem(Kind kind, int deltaMilli, String detail, boolean halved) {
        this.kind = kind;
        this.deltaMilli = deltaMilli;
        this.detail = detail;
        this.halved = halved;
    }

    public Kind kind() {
        return kind;
    }

    /** 가중치 변화(천분율). +100 = +0.1 */
    public int deltaMilli() {
        return deltaMilli;
    }

    /** 판정 근거. 예) "5번 중 4번(80%)" */
    public String detail() {
        return detail;
    }

    /** 10km 미만 단거리라 절반만 반영했는지 */
    public boolean halved() {
        return halved;
    }

    /** 리포트 한 줄. 예) "회전 시 방향지시등 · 5번 중 4번(80%) · +0.1" */
    public String line() {
        return kind.label() + " · " + detail + " · " + WeightBreakdown.formatDelta(deltaMilli)
                + (halved ? " (단거리 절반 반영)" : "");
    }
}
