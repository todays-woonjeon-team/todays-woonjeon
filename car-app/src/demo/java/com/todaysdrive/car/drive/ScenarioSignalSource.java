package com.todaysdrive.car.drive;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.todaysdrive.core.drive.DemoScenarios;
import com.todaysdrive.core.drive.GearState;
import com.todaysdrive.core.drive.SignalEvent;
import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.drive.VehicleSignal;

import java.util.List;

/**
 * 시연용 신호. 기획서 이용 사례 시나리오를 실제 차량 신호처럼 시간 순서대로 낸다.
 *
 * <p>신호의 시각은 시나리오 시간 그대로라서(예: 15분 주행) 판정 결과는 실제 주행과 같고,
 * 화면에서는 약 40초 만에 끝나도록 빠르게 재생한다.
 */
final class ScenarioSignalSource implements SignalSource {

    private static final long TARGET_PLAY_MILLIS = 40_000;
    private static final long PUMP_MILLIS = 50;

    private final Handler main = new Handler(Looper.getMainLooper());
    private Listener listener;
    private long lastEmitted;

    private List<SignalEvent> queue;
    private int index;
    private long firstTime;
    private long startUptime;
    private long speedUp;
    private Runnable onFinished;

    private final Runnable pump = new Runnable() {
        @Override
        public void run() {
            if (queue == null || listener == null) {
                return;
            }
            long scenarioNow = firstTime + (SystemClock.uptimeMillis() - startUptime) * speedUp;
            while (index < queue.size() && queue.get(index).timeMs() <= scenarioNow) {
                SignalEvent e = queue.get(index++);
                lastEmitted = e.timeMs();
                listener.onSignal(e);
            }
            if (index < queue.size()) {
                main.postDelayed(this, PUMP_MILLIS);
            } else {
                queue = null;
                if (onFinished != null) {
                    Runnable done = onFinished;
                    onFinished = null;
                    done.run();
                }
            }
        }
    };

    @Override
    public void start(Listener l) {
        listener = l;
        l.onSupportChanged(support());
        l.onRestrictionChanged(false);
        long now = Math.max(System.currentTimeMillis(), lastEmitted + 1);
        lastEmitted = now;
        l.onSignal(SignalEvent.gear(now, GearState.PARK)); // 시작은 주차 상태
    }

    @Override
    public void stop() {
        main.removeCallbacks(pump);
        queue = null;
        listener = null;
    }

    @Override
    public SignalSupport support() {
        return SignalSupport.of(VehicleSignal.SPEED, VehicleSignal.GEAR, VehicleSignal.STEERING,
                VehicleSignal.TURN_SIGNAL, VehicleSignal.BRAKE, VehicleSignal.WIPER, VehicleSignal.SEATBELT);
    }

    @Override
    public String[] runtimePermissions() {
        return new String[0];
    }

    @Override
    public boolean canReplay() {
        return true;
    }

    @Override
    public boolean isReplaying() {
        return queue != null;
    }

    @Override
    public void replay(DemoScenarios scenario, Runnable finished) {
        if (listener == null || isReplaying()) {
            return;
        }
        long base = Math.max(System.currentTimeMillis(), lastEmitted + 1000);
        queue = scenario.events(base);
        index = 0;
        firstTime = queue.get(0).timeMs();
        long duration = queue.get(queue.size() - 1).timeMs() - firstTime;
        speedUp = Math.max(1, duration / TARGET_PLAY_MILLIS);
        startUptime = SystemClock.uptimeMillis();
        onFinished = finished;
        main.post(pump);
    }
}
