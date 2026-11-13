package com.todaysdrive.phone.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.todaysdrive.core.fortune.FortuneCard;
import com.todaysdrive.core.link.DailySummary;
import com.todaysdrive.core.report.ReportTexts;
import com.todaysdrive.phone.PhoneApp;
import com.todaysdrive.phone.R;

import java.time.LocalDate;
import java.util.List;

/** 휴대폰 첫 화면: 오늘의 운세·미션, 최근 주차 리포트, 남은 티켓 */
public class MainActivity extends Activity {

    private PhoneApp app;
    private TextView textLatestScore;
    private TextView textLatestDetail;
    private TextView textTickets;
    private TextView textSync;
    private Button buttonPair;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        app = (PhoneApp) getApplication();

        textLatestScore = findViewById(R.id.textLatestScore);
        textLatestDetail = findViewById(R.id.textLatestDetail);
        textTickets = findViewById(R.id.textTickets);
        textSync = findViewById(R.id.textSync);
        buttonPair = findViewById(R.id.buttonPair);

        buttonPair.setOnClickListener(v -> startActivity(new Intent(this, PairActivity.class)));
        findViewById(R.id.buttonReport).setOnClickListener(v -> startActivity(new Intent(this, ReportActivity.class)));
        findViewById(R.id.buttonRoulette).setOnClickListener(v -> startActivity(new Intent(this, RouletteActivity.class)));
        findViewById(R.id.buttonWeekly).setOnClickListener(v -> startActivity(new Intent(this, WeeklyActivity.class)));
        findViewById(R.id.buttonSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showFortune();
        boolean paired = app.settings().isPaired();
        buttonPair.setVisibility(paired ? View.GONE : View.VISIBLE);
        showLatest(app.settings().latestDay());
        if (!paired) {
            textTickets.setText("");
            textSync.setText(R.string.not_paired);
            return;
        }
        textSync.setText("");
        app.run(app::syncDays, (days, error) -> {
            if (error != null) {
                textSync.setText(R.string.sync_failed);
                return;
            }
            showLatest(days.isEmpty() ? null : days.get(days.size() - 1));
            loadTickets();
        });
    }

    private void showFortune() {
        LocalDate today = LocalDate.now();
        FortuneCard card = app.fortuneFor(today);
        ((TextView) findViewById(R.id.textDate)).setText(ReportTexts.dateText(today));
        ((TextView) findViewById(R.id.textHeadline)).setText(card.headline());
        ((TextView) findViewById(R.id.textFortune)).setText(card.fortuneText());
        ((TextView) findViewById(R.id.textMission)).setText(card.missionTitle());
        ((TextView) findViewById(R.id.textTip)).setText(card.tip());
    }

    private void showLatest(DailySummary latest) {
        if (latest == null) {
            textLatestScore.setText("-");
            textLatestDetail.setText(R.string.latest_empty);
            return;
        }
        textLatestScore.setText(ReportTexts.scoreText(latest));
        textLatestDetail.setText(ReportTexts.dateText(latest.localDate()) + " · " + ReportTexts.driveText(latest)
                + "\n" + latest.goodPoint());
    }

    private void loadTickets() {
        if (app.link() == null) {
            return;
        }
        String pairId = app.settings().pairId();
        app.run(() -> app.link().fetchTickets(pairId).balance(), (balance, error) -> {
            if (error == null) {
                textTickets.setText(getString(R.string.tickets_left, balance));
            }
        });
    }

    /** 다른 화면에서 지난 기록을 찾을 때 쓴다. */
    static DailySummary findDay(List<DailySummary> days, String date) {
        for (DailySummary d : days) {
            if (d.date().equals(date)) {
                return d;
            }
        }
        return null;
    }
}
