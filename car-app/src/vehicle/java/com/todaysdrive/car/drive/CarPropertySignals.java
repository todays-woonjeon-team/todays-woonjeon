package com.todaysdrive.car.drive;

import android.car.Car;
import android.car.VehiclePropertyIds;
import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context;
import android.util.Log;

import com.todaysdrive.core.drive.GearState;
import com.todaysdrive.core.drive.SignalEvent;
import com.todaysdrive.core.drive.VehicleSignal;

/**
 * AAOS 표준 Car API로 받는 신호: 차속(PERF_VEHICLE_SPEED), 기어(GEAR_SELECTION), 주행 제한 상태.
 *
 * <p>에뮬레이터에서는 adb로 값을 넣어 시험한다(docs/demo-guide.md).
 * <pre>
 * adb shell cmd car_service inject-vhal-event 0x11600207 8.3   # 속도 8.3m/s(약 30km/h)
 * adb shell cmd car_service inject-vhal-event 0x11400400 8     # 기어 D
 * </pre>
 */
final class CarPropertySignals {

    private static final String TAG = "CarPropertySignals";

    // VehicleGear 값 (VHAL 정의)
    private static final int GEAR_NEUTRAL = 0x0001;
    private static final int GEAR_REVERSE = 0x0002;
    private static final int GEAR_PARK = 0x0004;
    private static final int GEAR_DRIVE = 0x0008;

    private final Context context;
    private final SignalSink sink;
    private Car car;
    private CarPropertyManager properties;
    private CarUxRestrictionsManager uxRestrictions;

    private final CarPropertyManager.CarPropertyEventCallback callback =
            new CarPropertyManager.CarPropertyEventCallback() {
                @Override
                public void onChangeEvent(CarPropertyValue value) {
                    long now = System.currentTimeMillis();
                    Object v = value.getValue();
                    switch (value.getPropertyId()) {
                        case VehiclePropertyIds.PERF_VEHICLE_SPEED:
                            if (v instanceof Float) {
                                sink.emit(SignalEvent.speed(now, (Float) v * 3.6)); // m/s → km/h
                            }
                            break;
                        case VehiclePropertyIds.GEAR_SELECTION:
                            if (v instanceof Integer) {
                                sink.emit(SignalEvent.gear(now, toGear((Integer) v)));
                            }
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onErrorEvent(int propertyId, int areaId) {
                    Log.w(TAG, "속성 오류 0x" + Integer.toHexString(propertyId));
                }
            };

    CarPropertySignals(Context context, SignalSink sink) {
        this.context = context;
        this.sink = sink;
    }

    void start() {
        try {
            car = Car.createCar(context);
            properties = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);
        } catch (RuntimeException | NoClassDefFoundError e) {
            Log.e(TAG, "Car 서비스에 연결하지 못했습니다", e);
            return;
        }
        sink.setSupported(VehicleSignal.SPEED,
                register(VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL));
        sink.setSupported(VehicleSignal.GEAR,
                register(VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE));

        try {
            uxRestrictions = (CarUxRestrictionsManager) car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE);
            if (uxRestrictions != null) {
                uxRestrictions.registerListener(this::onUxRestrictionsChanged);
                CarUxRestrictions current = uxRestrictions.getCurrentCarUxRestrictions();
                if (current != null) {
                    onUxRestrictionsChanged(current);
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "주행 제한 상태를 받을 수 없습니다", e);
        }
    }

    private void onUxRestrictionsChanged(CarUxRestrictions restrictions) {
        sink.restricted(restrictions.isRequiresDistractionOptimization());
    }

    /** 권한이 없거나 이 차가 지원하지 않으면 false */
    private boolean register(int propertyId, float rate) {
        try {
            if (properties.getCarPropertyConfig(propertyId) == null) {
                return false;
            }
            return properties.registerCallback(callback, propertyId, rate);
        } catch (SecurityException | IllegalArgumentException e) {
            Log.w(TAG, "신호 등록 실패 0x" + Integer.toHexString(propertyId) + ": " + e.getMessage());
            return false;
        }
    }

    void stop() {
        try {
            if (properties != null) {
                properties.unregisterCallback(callback);
            }
            if (uxRestrictions != null) {
                uxRestrictions.unregisterListener();
            }
            if (car != null) {
                car.disconnect();
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "정리 중 오류", e);
        }
    }

    private static GearState toGear(int gear) {
        switch (gear) {
            case GEAR_PARK:
                return GearState.PARK;
            case GEAR_REVERSE:
                return GearState.REVERSE;
            case GEAR_NEUTRAL:
                return GearState.NEUTRAL;
            case GEAR_DRIVE:
                return GearState.DRIVE;
            default:
                return gear > GEAR_DRIVE ? GearState.DRIVE : GearState.UNKNOWN; // 수동 1~9단
        }
    }
}
