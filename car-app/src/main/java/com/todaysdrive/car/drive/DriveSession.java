package com.todaysdrive.car.drive;

import android.util.Log;

import com.todaysdrive.car.CarApp;
import com.todaysdrive.core.drive.DemoScenarios;
import com.todaysdrive.core.drive.DriveAnalyzer;
import com.todaysdrive.core.drive.DriveStats;
import com.todaysdrive.core.drive.SignalEvent;
import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.link.DailySummary;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 차량 신호 → 판정 → 주차 리포트의 흐름을 잇는다. 앱 전체에 하나만 있다(메인 스레드 전용).
 *
 * <p>주행 중에는 화면 없이 신호만 쌓고, P단에 들어가는 순간 오늘의 리포트를 만들어 저장하고
 * (연결·동의한 경우) 서버에 올린다.
 */
public final class DriveSession implements SignalSource.Listener {

    private static final String TAG = "DriveSession";
    private static final long SAVE_INTERVAL_MS = 30_000;

    public interface Observer {
        void onDriveStateChanged();

        void onReportReady(DailySummary summary);
    }

    private final CarApp app;
    private final List<Observer> observers = new ArrayList<>();
    private SignalSource source;
    private SignalSupport support = SignalSupport.all();
    private DriveAnalyzer analyzer;
    private LocalDate day;
    private boolean parked = true;
    private boolean restricted;
    private long lastSaveAt;

    public DriveSession(CarApp app) {
        this.app = app;
    }

    public void start() {
        if (source != null) {
            return;
        }
        day = LocalDate.now();
        DriveStats saved = app.days().loadStats(day);
        analyzer = saved == null
                ? new DriveAnalyzer(app.rules())
                : DriveAnalyzer.resume(app.rules(), saved);
        source = SignalSources.create(app);
        source.start(this);
    }

    public void stop() {
        if (source == null) {
            return;
        }
        source.stop();
        source = null;
        app.days().saveStats(day, analyzer.peek());
    }

    /** 권한을 새로 받았을 때 신호 등록을 다시 한다. */
    public void restartSource() {
        stop();
        start();
    }

    public void addObserver(Observer o) {
        observers.add(o);
    }

    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    public boolean isParked() {
        return parked;
    }

    /** 화면을 쓰면 안 되는 상태(주행 중 또는 시스템 주행 제한) */
    public boolean isScreenRestricted() {
        return !parked || restricted;
    }

    public SignalSupport support() {
        return support;
    }

    public String[] runtimePermissions() {
        return source == null ? new String[0] : source.runtimePermissions();
    }

    public double currentSpeedKmh() {
        return analyzer == null ? 0 : analyzer.speedKmh();
    }

    public double todayDistanceKm() {
        return analyzer == null ? 0 : analyzer.peek().distanceKm();
    }

    // ── 시연(demo 빌드) ──

    public boolean canReplay() {
        return source != null && source.canReplay();
    }

    public boolean isReplaying() {
        return source != null && source.isReplaying();
    }

    public void replay(DemoScenarios scenario, Runnable onFinished) {
        if (source != null) {
            source.replay(scenario, onFinished);
        }
    }

    // ── 신호 ──

    @Override
    public void onSignal(SignalEvent event) {
        LocalDate eventDay = Instant.ofEpochMilli(event.timeMs()).atZone(ZoneId.systemDefault()).toLocalDate();
        if (!eventDay.equals(day)) {
            DriveStats finished = analyzer.startNewDay(event.timeMs());
            app.days().saveStats(day, finished);
            saveReport(day, finished, event.timeMs());
            day = eventDay;
        }

        analyzer.accept(event);

        boolean nowParked = analyzer.isParked();
        if (nowParked != parked) {
            parked = nowParked;
            if (parked) {
                onParked(event.timeMs());
            }
            notifyState();
        }
        long wall = System.currentTimeMillis(); // 시연 재생은 시간이 빨리 흐르므로 실제 시각으로 저장 간격을 잰다
        if (wall - lastSaveAt > SAVE_INTERVAL_MS) {
            lastSaveAt = wall;
            app.days().saveStats(day, analyzer.peek());
        }
    }

    @Override
    public void onSupportChanged(SignalSupport newSupport) {
        support = newSupport;
        Log.i(TAG, "지원 신호: " + newSupport);
        notifyState();
    }

    @Override
    public void onRestrictionChanged(boolean value) {
        restricted = value;
        notifyState();
    }

    private void onParked(long now) {
        DriveStats stats = analyzer.closeOut(now);
        app.days().saveStats(day, stats);
        DailySummary summary = saveReport(day, stats, now);
        if (summary != null && stats.distanceMeters() > 0) {
            for (Observer o : new ArrayList<>(observers)) {
                o.onReportReady(summary);
            }
        }
    }

    /** 지금까지의 기록으로 오늘 리포트를 다시 만든다(리포트 화면을 열 때). */
    public DailySummary refreshToday() {
        if (analyzer == null) {
            return app.days().loadSummary(LocalDate.now());
        }
        long now = Math.max(System.currentTimeMillis(), analyzer.peek().lastEventMs());
        DriveStats stats = analyzer.closeOut(now);
        app.days().saveStats(day, stats);
        return saveReport(day, stats, now);
    }

    private DailySummary saveReport(LocalDate date, DriveStats stats, long now) {
        if (stats.distanceMeters() <= 0 && app.days().loadSummary(date) == null) {
            return null; // 오늘 아직 달리지 않음
        }
        DailySummary summary = app.summaryFactory().create(app.fortuneFor(date), stats, support,
                app.days().allSummaries(), now);
        app.days().saveSummary(summary);
        app.link().uploadIfLinked(summary);
        return summary;
    }

    private void notifyState() {
        for (Observer o : new ArrayList<>(observers)) {
            o.onDriveStateChanged();
        }
    }
}
