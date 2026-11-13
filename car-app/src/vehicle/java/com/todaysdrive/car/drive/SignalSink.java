package com.todaysdrive.car.drive;

import com.todaysdrive.core.drive.SignalEvent;
import com.todaysdrive.core.drive.VehicleSignal;

/** 신호 수신부가 받은 값을 넘기는 곳 */
interface SignalSink {

    void emit(SignalEvent event);

    void setSupported(VehicleSignal signal, boolean supported);

    void restricted(boolean restricted);
}
