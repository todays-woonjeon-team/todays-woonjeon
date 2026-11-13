package com.todaysdrive.car.drive;

import com.todaysdrive.core.drive.DemoScenarios;
import com.todaysdrive.core.drive.SignalEvent;
import com.todaysdrive.core.drive.SignalSupport;

/**
 * 차량 신호를 내는 곳. 빌드에 따라 구현이 다르다.
 * <ul>
 *   <li>demo 빌드: {@code ScenarioSignalSource} - 기획서 이용 사례를 시나리오로 재생</li>
 *   <li>vehicle 빌드: {@code VehicleSignalSource} - Vehicle SDK + CarPropertyManager</li>
 * </ul>
 * 어느 쪽이든 콜백은 메인 스레드에서 부른다.
 */
public interface SignalSource {

    interface Listener {
        void onSignal(SignalEvent event);

        /** 받을 수 있는 신호 목록이 확인되거나 바뀜 */
        void onSupportChanged(SignalSupport support);

        /** 주행 제한 상태(CarUxRestrictions)가 바뀜. true면 화면을 쓰면 안 되는 상태 */
        void onRestrictionChanged(boolean restricted);
    }

    void start(Listener listener);

    void stop();

    SignalSupport support();

    /** 실행 중에 요청해야 하는 권한(없으면 빈 배열) */
    String[] runtimePermissions();

    /** 시연 주행을 재생할 수 있는 빌드인지 */
    default boolean canReplay() {
        return false;
    }

    /** 시연 주행 재생. 약 40초 안에 끝나도록 빠르게 돌린다. */
    default void replay(DemoScenarios scenario, Runnable onFinished) {
    }

    default boolean isReplaying() {
        return false;
    }
}
