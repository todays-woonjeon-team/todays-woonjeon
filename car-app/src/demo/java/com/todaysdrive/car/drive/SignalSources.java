package com.todaysdrive.car.drive;

import android.content.Context;

/** demo 빌드: 시나리오 재생 신호 */
public final class SignalSources {

    private SignalSources() {
    }

    public static SignalSource create(Context context) {
        return new ScenarioSignalSource();
    }
}
