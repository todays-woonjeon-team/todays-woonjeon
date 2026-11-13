package com.todaysdrive.car.drive;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.todaysdrive.core.drive.SignalEvent;
import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.drive.VehicleSignal;

import java.util.EnumSet;
import java.util.Set;

/**
 * 실제 차량 신호. 두 곳에서 받은 신호를 하나로 합친다.
 * <ul>
 *   <li>{@link CarPropertySignals}: 차속, 기어, 주행 제한 상태 (AAOS 표준 API)</li>
 *   <li>{@link PleosSignals}: 방향지시등, 핸들 각도, 브레이크 페달, 와이퍼, 안전벨트 (Pleos Vehicle SDK)</li>
 * </ul>
 * 차종에 따라 지원하지 않는 신호는 지원 목록에서 빠지고, 그 신호에 기대는 평가 항목과 미션도 빠진다.
 */
final class VehicleSignalSource implements SignalSource, SignalSink {

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Set<VehicleSignal> supported = EnumSet.noneOf(VehicleSignal.class);
    private CarPropertySignals carProps;
    private PleosSignals pleos;
    private Listener listener;

    VehicleSignalSource(Context context) {
        this.context = context;
    }

    @Override
    public void start(Listener l) {
        listener = l;
        carProps = new CarPropertySignals(context, this);
        pleos = new PleosSignals(context, this);
        carProps.start();
        pleos.start();
        l.onSupportChanged(support());
    }

    @Override
    public void stop() {
        if (pleos != null) {
            pleos.stop();
        }
        if (carProps != null) {
            carProps.stop();
        }
        listener = null;
    }

    @Override
    public synchronized SignalSupport support() {
        return SignalSupport.of(supported);
    }

    @Override
    public String[] runtimePermissions() {
        return new String[]{"android.car.permission.CAR_SPEED"};
    }

    // ── SignalSink: 어느 스레드에서 와도 메인 스레드로 넘긴다 ──

    @Override
    public void emit(SignalEvent event) {
        main.post(() -> {
            if (listener != null) {
                listener.onSignal(event);
            }
        });
    }

    @Override
    public void setSupported(VehicleSignal signal, boolean ok) {
        boolean changed;
        synchronized (this) {
            changed = ok ? supported.add(signal) : supported.remove(signal);
        }
        if (changed) {
            main.post(() -> {
                if (listener != null) {
                    listener.onSupportChanged(support());
                }
            });
        }
    }

    @Override
    public void restricted(boolean restricted) {
        main.post(() -> {
            if (listener != null) {
                listener.onRestrictionChanged(restricted);
            }
        });
    }
}
