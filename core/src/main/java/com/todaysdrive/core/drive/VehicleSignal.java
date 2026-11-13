package com.todaysdrive.core.drive;

/**
 * 앱이 쓰는 차량 신호 종류. 차종에 따라 지원하지 않는 신호가 있을 수 있다.
 */
public enum VehicleSignal {
    /** 차속(km/h). AAOS CarPropertyManager PERF_VEHICLE_SPEED */
    SPEED,
    /** 기어(P/R/N/D). AAOS GEAR_SELECTION */
    GEAR,
    /** 핸들 각도(도). 왼쪽이 음수. Vehicle SDK SteeringWheel */
    STEERING,
    /** 방향지시등. Vehicle SDK TurnSignal */
    TURN_SIGNAL,
    /** 브레이크 페달 밟힘 여부. Vehicle SDK Brake */
    BRAKE,
    /** 와이퍼 작동 여부. Vehicle SDK Wiper */
    WIPER,
    /** 탑승 좌석 안전벨트 착용 여부. Vehicle SDK Safety/Seat */
    SEATBELT
}
