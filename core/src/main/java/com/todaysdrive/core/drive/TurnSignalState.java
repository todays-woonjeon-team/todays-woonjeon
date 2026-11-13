package com.todaysdrive.core.drive;

/** 방향지시등 상태 */
public enum TurnSignalState {
    NONE(0), LEFT(-1), RIGHT(1), HAZARD(0);

    private final int direction;

    TurnSignalState(int direction) {
        this.direction = direction;
    }

    /**
     * 핸들 방향과 같은 부호. 왼쪽 -1, 오른쪽 +1, 그 밖(꺼짐·비상등) 0.
     * 비상등은 회전 신호로 치지 않는다.
     */
    public int direction() {
        return direction;
    }
}
