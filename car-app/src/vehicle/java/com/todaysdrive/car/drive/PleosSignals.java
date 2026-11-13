package com.todaysdrive.car.drive;

import android.content.Context;
import android.util.Log;

import com.todaysdrive.core.drive.SignalEvent;
import com.todaysdrive.core.drive.TurnSignalState;
import com.todaysdrive.core.drive.VehicleSignal;

import ai.pleos.playground.vehicle.Vehicle;

/**
 * Pleos Vehicle SDK 2.3.2로 받는 신호: 방향지시등, 핸들 각도, 브레이크 페달, 와이퍼, 안전벨트.
 *
 * <p>SDK 진입점(Vehicle, initialize, getTurnSignal 등)은 문서에서 확인한 이름으로 직접 부른다.
 * 리스너 등록 메서드와 콜백 시그니처는 에뮬레이터에서 확인하기 전까지 {@link PleosBridge}로 이름을 찾아
 * 연결하고, 실제 시그니처를 로그(태그 PleosBridge)로 남긴다. 확인이 끝나면 타입이 있는 코드로 바꾼다.
 * 체크리스트: docs/vehicle-signals.md
 */
final class PleosSignals {

    private static final String TAG = "PleosSignals";

    /** [확인 필요] SDK가 왼쪽 회전을 양수로 주면 true로 바꾼다. 앱 내부 기준은 왼쪽 음수 */
    private static final boolean STEERING_LEFT_IS_POSITIVE = false;

    private final Context context;
    private final SignalSink sink;
    private Vehicle vehicle;
    private PleosBridge.Registration turnSignalReg;
    private PleosBridge.Registration steeringReg;
    private PleosBridge.Registration brakeReg;
    private PleosBridge.Registration wiperReg;
    private PleosBridge.Registration seatBeltReg;

    PleosSignals(Context context, SignalSink sink) {
        this.context = context;
        this.sink = sink;
    }

    void start() {
        try {
            vehicle = new Vehicle(context);
            vehicle.initialize();
        } catch (RuntimeException e) {
            Log.e(TAG, "Vehicle SDK 초기화 실패", e);
            return;
        }

        Object turnSignal = vehicle.getTurnSignal();
        PleosBridge.checkCapability(turnSignal, "checkTurnSignalStateCapabilities",
                ok -> sink.setSupported(VehicleSignal.TURN_SIGNAL, ok));
        turnSignalReg = PleosBridge.register(turnSignal, "registerTurnSignalState",
                "TurnSignalStateListener", new PleosBridge.ValueSink() {
                    @Override
                    public void onValue(Object value) {
                        sink.emit(SignalEvent.turnSignal(System.currentTimeMillis(), toTurnSignal(value)));
                    }

                    @Override
                    public void onError(Throwable error) {
                        Log.w(TAG, "방향지시등 오류: " + error);
                    }
                });

        Object steering = vehicle.getSteeringWheel();
        PleosBridge.checkCapability(steering, "checkSteeringWheelAngleCapabilities",
                ok -> sink.setSupported(VehicleSignal.STEERING, ok));
        steeringReg = PleosBridge.register(steering, "registerSteeringWheelAngle",
                "SteeringWheelAngleListener", new PleosBridge.ValueSink() {
                    @Override
                    public void onValue(Object value) {
                        Double deg = PleosBridge.toDouble(value);
                        if (deg != null) {
                            double leftNegative = STEERING_LEFT_IS_POSITIVE ? -deg : deg;
                            sink.emit(SignalEvent.steering(System.currentTimeMillis(), leftNegative));
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        Log.w(TAG, "핸들 각도 오류: " + error);
                    }
                });

        Object brake = vehicle.getBrake();
        PleosBridge.checkCapability(brake, "checkBrakePedalStatusCapabilities",
                ok -> sink.setSupported(VehicleSignal.BRAKE, ok));
        brakeReg = PleosBridge.register(brake, "registerBrakePedalStatus",
                "BrakePedalStatusListener", booleanSink(VehicleSignal.BRAKE));

        Object wiper = vehicle.getWiper();
        PleosBridge.checkCapability(wiper, "checkWiperStateCapabilities",
                ok -> sink.setSupported(VehicleSignal.WIPER, ok));
        wiperReg = PleosBridge.register(wiper, "registerWiperState",
                "WiperStateListener", booleanSink(VehicleSignal.WIPER));

        // 안전벨트는 선택 항목. 좌석 영역 인자가 필요하면 연결되지 않고 미션에서 빠진다.
        Object safety = vehicle.getSafety();
        PleosBridge.checkCapability(safety, "checkSeatBeltBuckledStateCapabilities",
                ok -> sink.setSupported(VehicleSignal.SEATBELT, ok));
        seatBeltReg = PleosBridge.register(safety, "registerSeatBeltBuckledState",
                "SeatBeltBuckledStateListener", booleanSink(VehicleSignal.SEATBELT));
    }

    void stop() {
        PleosBridge.unregister(turnSignalReg);
        PleosBridge.unregister(steeringReg);
        PleosBridge.unregister(brakeReg);
        PleosBridge.unregister(wiperReg);
        PleosBridge.unregister(seatBeltReg);
        if (vehicle != null) {
            try {
                vehicle.release(); // 등록 해제가 끝난 뒤에 해제한다
            } catch (RuntimeException e) {
                Log.w(TAG, "release 오류", e);
            }
            vehicle = null;
        }
    }

    private PleosBridge.ValueSink booleanSink(VehicleSignal signal) {
        return new PleosBridge.ValueSink() {
            @Override
            public void onValue(Object value) {
                boolean on = PleosBridge.toBoolean(value);
                long now = System.currentTimeMillis();
                switch (signal) {
                    case BRAKE:
                        sink.emit(SignalEvent.brake(now, on));
                        break;
                    case WIPER:
                        sink.emit(SignalEvent.wiper(now, on));
                        break;
                    case SEATBELT:
                        sink.emit(SignalEvent.seatbelt(now, on));
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onError(Throwable error) {
                Log.w(TAG, signal + " 오류: " + error);
            }
        };
    }

    /**
     * [확인 필요] 숫자로 오면 VHAL TURN_SIGNAL_STATE 규칙(0 꺼짐, 1 오른쪽, 2 왼쪽)으로 읽고,
     * 상수·enum으로 오면 이름에 LEFT/RIGHT가 들어 있는지로 읽는다.
     */
    static TurnSignalState toTurnSignal(Object value) {
        if (value instanceof Number) {
            switch (((Number) value).intValue()) {
                case 1:
                    return TurnSignalState.RIGHT;
                case 2:
                    return TurnSignalState.LEFT;
                case 3:
                    return TurnSignalState.HAZARD;
                default:
                    return TurnSignalState.NONE;
            }
        }
        String name = String.valueOf(value).toUpperCase(java.util.Locale.ROOT);
        if (name.contains("HAZARD") || name.contains("EMERGENCY") || name.contains("BOTH")) {
            return TurnSignalState.HAZARD;
        }
        if (name.contains("LEFT")) {
            return TurnSignalState.LEFT;
        }
        if (name.contains("RIGHT")) {
            return TurnSignalState.RIGHT;
        }
        return TurnSignalState.NONE;
    }
}
