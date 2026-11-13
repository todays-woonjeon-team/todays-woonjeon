package com.todaysdrive.core.fortune;

import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.drive.VehicleSignal;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 오늘의 행운 미션. 차량 신호로 달성 여부를 확인할 수 있는 것만 둔다.
 * 달성 판정은 {@link com.todaysdrive.core.score.MissionJudge}가 한다.
 */
public enum MissionType {
    ALL_TURNS_SIGNALED("오늘 회전은 전부 깜빡이와 함께", false,
            VehicleSignal.TURN_SIGNAL, VehicleSignal.STEERING, VehicleSignal.SPEED),
    EARLY_SIGNAL("깜빡이는 핸들 돌리기 3초 전에 (80% 이상)", false,
            VehicleSignal.TURN_SIGNAL, VehicleSignal.STEERING, VehicleSignal.SPEED),
    NO_HARD_BRAKE("급브레이크 없는 하루", false, VehicleSignal.SPEED),
    SMOOTH_START("급출발 없는 하루", false, VehicleSignal.SPEED),
    SEATBELT_BEFORE_START("출발 전 전 좌석 안전벨트", false,
            VehicleSignal.SEATBELT, VehicleSignal.SPEED),
    /** 장거리 주행이 예상되는 날에만 낸다(NaviHelper 경로 정보 연동 후). */
    REST_BEFORE_2H("2시간 안에 한 번 쉬어 가기", true, VehicleSignal.GEAR, VehicleSignal.SPEED);

    private final String defaultTitle;
    private final boolean longTripOnly;
    private final Set<VehicleSignal> required;

    MissionType(String defaultTitle, boolean longTripOnly, VehicleSignal first, VehicleSignal... rest) {
        this.defaultTitle = defaultTitle;
        this.longTripOnly = longTripOnly;
        this.required = Collections.unmodifiableSet(EnumSet.of(first, rest));
    }

    /** 문구 파일에 제목이 없을 때 쓰는 기본 제목 */
    public String defaultTitle() {
        return defaultTitle;
    }

    public Set<VehicleSignal> requiredSignals() {
        return required;
    }

    public boolean isAvailable(SignalSupport support, boolean longTripExpected) {
        if (longTripOnly && !longTripExpected) {
            return false;
        }
        return support.hasAll(required);
    }

    /** 이 차에서 낼 수 있는 미션 목록. 비어 있으면 급브레이크 미션만 낸다. */
    public static Set<MissionType> poolFor(SignalSupport support, boolean longTripExpected) {
        EnumSet<MissionType> pool = EnumSet.noneOf(MissionType.class);
        for (MissionType m : values()) {
            if (m.isAvailable(support, longTripExpected)) {
                pool.add(m);
            }
        }
        if (pool.isEmpty()) {
            pool.add(NO_HARD_BRAKE);
        }
        return pool;
    }
}
