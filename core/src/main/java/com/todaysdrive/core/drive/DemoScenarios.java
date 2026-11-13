package com.todaysdrive.core.drive;

import java.util.List;

/**
 * 기획서의 이용 사례를 그대로 재현하는 시연용 주행 시나리오.
 * 차량 앱 demo 빌드의 "시연 주행" 버튼과 단위 테스트가 함께 쓴다.
 */
public enum DemoScenarios {

    /** 사례 1. 출근길 12.4km. 회전 5번 중 4번 깜빡이(미리 켠 건 1번), 급감속 1번 → 가중치 1.2 */
    COMMUTE("출근길", "회전 5번 중 4번 깜빡이, 급감속 1번"),
    /** 사례 2. 거친 운전 10.5km. 회전 5번 중 2번 깜빡이, 급가속·급감속 1번씩 → 가중치 0.8 */
    ROUGH("서두른 날", "회전 5번 중 2번 깜빡이, 급가속·급감속"),
    /** 빗길 11km. 와이퍼를 켜고 급조작 없이, 모든 회전에 깜빡이를 미리 → 가중치 상한 1.5 */
    RAINY("비 오는 퇴근길", "와이퍼 ON, 급조작 없음, 깜빡이 모두 미리"),
    /** 사례 3. 장거리 약 160km. 1시간 40분 주행 뒤 20분 휴식 → 가중치 1.35 */
    LONG_TRIP("고향 가는 길", "1시간 40분 주행 후 20분 휴식, 급감속 9번");

    private final String title;
    private final String description;

    DemoScenarios(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public List<SignalEvent> events(long startMs) {
        switch (this) {
            case COMMUTE:
                return commute(startMs);
            case ROUGH:
                return rough(startMs);
            case RAINY:
                return rainy(startMs);
            case LONG_TRIP:
                return longTrip(startMs);
            default:
                throw new IllegalStateException(name());
        }
    }

    private static final int LEFT = -1;
    private static final int RIGHT = 1;
    private static final long EARLY = 4000;
    private static final long LATE = 1000;

    private static List<SignalEvent> commute(long start) {
        ScenarioBuilder b = depart(start, 50);
        b.cruiseUntilKm(1.5);
        intersection(b, RIGHT, true, EARLY, 50);
        b.cruiseUntilKm(3.5);
        intersection(b, LEFT, true, LATE, 50);
        b.cruiseUntilKm(5.5);
        intersection(b, RIGHT, false, LATE, 50);
        b.cruiseUntilKm(7.5);
        suddenBrake(b, 20);
        b.cruiseUntilKm(9.0);
        intersection(b, LEFT, true, LATE, 50);
        b.cruiseUntilKm(11.0);
        intersection(b, RIGHT, true, LATE, 50);
        b.cruiseUntilKm(12.3);
        return park(b);
    }

    private static List<SignalEvent> rough(long start) {
        ScenarioBuilder b = depart(start, 50);
        b.cruiseUntilKm(1.5);
        intersection(b, LEFT, false, LATE, 50);
        b.cruiseUntilKm(3.0);
        intersection(b, RIGHT, true, LATE, 50);
        b.cruiseUntilKm(4.5);
        intersection(b, RIGHT, false, LATE, 30);
        b.speedTo(50, 8); // 급가속
        b.cruiseUntilKm(6.5);
        suddenBrake(b, 20);
        b.cruiseUntilKm(8.0);
        intersection(b, LEFT, true, LATE, 50);
        b.cruiseUntilKm(9.5);
        intersection(b, LEFT, false, LATE, 50);
        b.cruiseUntilKm(10.4);
        return park(b);
    }

    private static List<SignalEvent> rainy(long start) {
        ScenarioBuilder b = new ScenarioBuilder(start);
        b.gear(GearState.PARK).seatbelt(true).wiper(true).pause(2000).gear(GearState.DRIVE);
        b.speedTo(40, 3);
        b.cruiseUntilKm(2.0);
        intersection(b, LEFT, true, EARLY, 40);
        b.cruiseUntilKm(4.5);
        intersection(b, RIGHT, true, EARLY, 40);
        b.cruiseUntilKm(7.0);
        intersection(b, RIGHT, true, EARLY, 40);
        b.cruiseUntilKm(9.5);
        intersection(b, LEFT, true, EARLY, 40);
        b.cruiseUntilKm(10.9);
        return park(b);
    }

    private static List<SignalEvent> longTrip(long start) {
        ScenarioBuilder b = depart(start, 90);
        // 1구간: 쉬지 않고 1시간 40분. 회전 15번(깜빡이 14번, 미리 12번), 급감속 6번
        for (int i = 0; i < 15; i++) {
            b.cruiseKm(7.5);
            boolean signal = i != 7;
            long lead = (i == 3 || i == 11) ? LATE : EARLY;
            intersection(b, i % 2 == 0 ? RIGHT : LEFT, signal, lead, 90);
            if (i % 2 == 1 && i < 12) {
                b.cruiseKm(1.0);
                suddenBrake(b, 20);
            }
        }
        b.holdUntil(start + 2000 + 100 * 60_000L);
        b.speedTo(0, 3).hold(1000).gear(GearState.PARK);

        // 휴게소에서 20분 휴식(시동 끔)
        b.pause(20 * 60_000L);

        // 2구간: 약 20km. 회전 10번(깜빡이 9번, 미리 8번), 급감속 3번
        b.gear(GearState.DRIVE).speedTo(60, 4);
        for (int i = 0; i < 10; i++) {
            b.cruiseKm(1.5);
            boolean signal = i != 4;
            long lead = i == 8 ? LATE : EARLY;
            intersection(b, i % 2 == 0 ? LEFT : RIGHT, signal, lead, 60);
            if (i == 1 || i == 5 || i == 8) {
                suddenBrake(b, 20);
            }
        }
        return park(b);
    }

    private static ScenarioBuilder depart(long start, double cruiseKmh) {
        ScenarioBuilder b = new ScenarioBuilder(start);
        b.gear(GearState.PARK).seatbelt(true).pause(2000).gear(GearState.DRIVE);
        b.speedTo(cruiseKmh, 4);
        return b;
    }

    /** 교차로 회전: 20km/h로 감속 → (깜빡이) → lead만큼 대기 → 핸들 360도 → 다시 가속 */
    private static void intersection(ScenarioBuilder b, int dir, boolean signal, long leadMs,
                                     double backToKmh) {
        b.speedTo(20, 3);
        if (signal) {
            b.signal(dir < 0 ? TurnSignalState.LEFT : TurnSignalState.RIGHT);
        }
        b.hold(leadMs);
        b.turn(dir, 360, 4000);
        b.hold(500);
        if (signal) {
            b.signal(TurnSignalState.NONE);
        }
        b.speedTo(backToKmh, 4);
    }

    /** 브레이크를 밟고 초당 10km/h로 dropKmh만큼 감속했다가 원래 속도로 돌아감 */
    private static void suddenBrake(ScenarioBuilder b, double dropKmh) {
        double cruise = b.speedKmh();
        b.brake(true).speedTo(cruise - dropKmh, 10).brake(false).hold(2000).speedTo(cruise, 4);
    }

    private static List<SignalEvent> park(ScenarioBuilder b) {
        b.speedTo(0, 3).hold(1000).gear(GearState.PARK);
        return b.build();
    }
}
