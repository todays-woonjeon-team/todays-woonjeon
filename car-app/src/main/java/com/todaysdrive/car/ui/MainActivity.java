package com.todaysdrive.car.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.todaysdrive.car.CarApp;
import com.todaysdrive.car.R;
import com.todaysdrive.car.drive.DriveMonitorService;
import com.todaysdrive.car.drive.DriveSession;
import com.todaysdrive.core.drive.DemoScenarios;
import com.todaysdrive.core.fortune.FortuneCard;
import com.todaysdrive.core.link.DailySummary;
import com.todaysdrive.core.report.ReportTexts;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 첫 화면. 왼쪽은 오늘의 운세 카드, 오른쪽은 오늘 점수·티켓과 메뉴.
 * 주행 중에는 안내 문구만 남기고 모두 가린다(차량에서는 시스템이 화면 자체를 막는다).
 */
public class MainActivity extends Activity implements DriveSession.Observer {

    private static final int REQUEST_PERMISSIONS = 1;
    private static final long PROGRESS_MILLIS = 500;

    private CarApp app;
    private TextView textDate;
    private TextView textHeadline;
    private TextView textFortune;
    private TextView textMission;
    private TextView textTip;
    private TextView textStatus;
    private TextView textToday;
    private TextView textTickets;
    private TextView textDemoProgress;
    private View drivingOverlay;
    private View demoPanel;
    private Spinner spinnerScenario;
    private Button buttonDemo;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable progress = new Runnable() {
        @Override
        public void run() {
            DriveSession s = app.session();
            if (s.isReplaying()) {
                textDemoProgress.setText(String.format(Locale.ROOT, "시연 주행 중 · %.0fkm/h · 오늘 %.1fkm",
                        s.currentSpeedKmh(), s.todayDistanceKm()));
                main.postDelayed(this, PROGRESS_MILLIS);
            } else {
                textDemoProgress.setText("");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        app = (CarApp) getApplication();

        textDate = findViewById(R.id.textDate);
        textHeadline = findViewById(R.id.textHeadline);
        textFortune = findViewById(R.id.textFortune);
        textMission = findViewById(R.id.textMission);
        textTip = findViewById(R.id.textTip);
        textStatus = findViewById(R.id.textStatus);
        textToday = findViewById(R.id.textToday);
        textTickets = findViewById(R.id.textTickets);
        textDemoProgress = findViewById(R.id.textDemoProgress);
        drivingOverlay = findViewById(R.id.drivingOverlay);
        demoPanel = findViewById(R.id.demoPanel);
        spinnerScenario = findViewById(R.id.spinnerScenario);
        buttonDemo = findViewById(R.id.buttonDemo);

        findViewById(R.id.buttonReport).setOnClickListener(v ->
                startActivity(new Intent(this, ReportActivity.class)));
        findViewById(R.id.buttonRoulette).setOnClickListener(v ->
                startActivity(new Intent(this, RouletteActivity.class)));
        findViewById(R.id.buttonLink).setOnClickListener(v ->
                startActivity(new Intent(this, LinkActivity.class)));
        findViewById(R.id.buttonBirth).setOnClickListener(v -> showBirthDialog());

        startForegroundService(new Intent(this, DriveMonitorService.class));
        app.session().start(); // 서비스보다 먼저 화면이 떠도 신호를 받도록(중복 호출은 무시됨)
        setupDemo();
        requestPermissionsIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.session().addObserver(this);
        refresh();
    }

    @Override
    protected void onPause() {
        app.session().removeObserver(this);
        main.removeCallbacks(progress);
        super.onPause();
    }

    private void refresh() {
        LocalDate today = LocalDate.now();
        FortuneCard card = app.fortuneFor(today);
        textDate.setText(ReportTexts.dateText(today));
        textHeadline.setText(card.headline());
        textFortune.setText(card.fortuneText());
        textMission.setText(card.missionTitle());
        textTip.setText(card.tip());

        DriveSession session = app.session();
        boolean restricted = session.isScreenRestricted();
        textStatus.setText(session.isParked() ? R.string.status_parked : R.string.status_driving);
        drivingOverlay.setVisibility(restricted ? View.VISIBLE : View.GONE);
        if (session.isReplaying()) {
            main.removeCallbacks(progress);
            main.post(progress);
        }

        DailySummary summary = app.days().loadSummary(today);
        if (summary == null) {
            textToday.setText(R.string.today_empty);
        } else {
            textToday.setText("오늘의 점수 " + ReportTexts.scoreText(summary) + " · " + ReportTexts.driveText(summary));
        }
        app.link().ticketBalance((balance, error) ->
                textTickets.setText(getString(R.string.tickets_left, balance == null ? 0 : balance)));
        buttonDemo.setEnabled(!session.isReplaying());
    }

    // ── 시연 주행(demo 빌드) ──

    private void setupDemo() {
        if (!app.session().canReplay()) {
            demoPanel.setVisibility(View.GONE);
            return;
        }
        demoPanel.setVisibility(View.VISIBLE);
        List<String> names = new ArrayList<>();
        for (DemoScenarios s : DemoScenarios.values()) {
            names.add(s.title() + " - " + s.description());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerScenario.setAdapter(adapter);
        buttonDemo.setOnClickListener(v -> {
            DemoScenarios scenario = DemoScenarios.values()[spinnerScenario.getSelectedItemPosition()];
            buttonDemo.setEnabled(false);
            app.session().replay(scenario, () -> buttonDemo.setEnabled(true));
            main.removeCallbacks(progress);
            main.postDelayed(progress, PROGRESS_MILLIS);
        });
    }

    // ── DriveSession.Observer ──

    @Override
    public void onDriveStateChanged() {
        refresh();
    }

    @Override
    public void onReportReady(DailySummary summary) {
        Intent intent = new Intent(this, ReportActivity.class);
        intent.putExtra(ReportActivity.EXTRA_DATE, summary.date());
        startActivity(intent);
    }

    // ── 띠 운세 ──

    private void showBirthDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.birth_title)
                .setMessage(R.string.birth_message)
                .setPositiveButton(R.string.birth_enter, (d, w) -> pickBirthDate())
                .setNegativeButton(R.string.birth_clear, (d, w) -> {
                    app.settings().setBirthDate(null);
                    refresh();
                })
                .show();
    }

    private void pickBirthDate() {
        LocalDate current = app.settings().birthDate();
        LocalDate initial = current != null ? current : LocalDate.of(1995, 1, 1);
        new DatePickerDialog(this, (view, year, month, day) -> {
            app.settings().setBirthDate(LocalDate.of(year, month + 1, day));
            refresh();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    // ── 권한 ──

    private void requestPermissionsIfNeeded() {
        List<String> needed = new ArrayList<>();
        for (String p : app.session().runtimePermissions()) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!needed.isEmpty()) {
            requestPermissions(needed.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            app.session().restartSource(); // 새로 받은 권한으로 신호를 다시 등록
            refresh();
        }
    }
}
