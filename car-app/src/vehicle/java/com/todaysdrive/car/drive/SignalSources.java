package com.todaysdrive.car.drive;

import android.content.Context;

/** vehicle 빌드: 실제 차량 신호(Vehicle SDK + CarPropertyManager) */
public final class SignalSources {

    private SignalSources() {
    }

    public static SignalSource create(Context context) {
        return new VehicleSignalSource(context.getApplicationContext());
    }
}
