package com.todaysdrive.car.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.todaysdrive.car.CarApp;
import com.todaysdrive.car.R;
import com.todaysdrive.core.link.DailySummary;
import com.todaysdrive.core.report.ReportTexts;

import java.time.LocalDate;

/** 주차 리포트: 오늘의 점수, 계산식, 받은 티켓, 잘한 점·아쉬운 점, 가중치 항목 */
public class ReportActivity extends Activity {

    public static final String EXTRA_DATE = "date";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        findViewById(R.id.buttonReportClose).setOnClickListener(v -> finish());
        findViewById(R.id.buttonGoRoulette).setOnClickListener(v -> {
            startActivity(new Intent(this, RouletteActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        CarApp app = (CarApp) getApplication();
        String dateExtra = getIntent().getStringExtra(EXTRA_DATE);
        LocalDate date = dateExtra == null ? LocalDate.now() : LocalDate.parse(dateExtra);

        DailySummary summary = date.equals(LocalDate.now()) && app.session().isParked()
                ? app.session().refreshToday()
                : app.days().loadSummary(date);
        if (summary == null) {
            summary = app.days().loadSummary(date);
        }
        show(date, summary);
    }

    private void show(LocalDate date, DailySummary s) {
        TextView textDate = findViewById(R.id.textReportDate);
        TextView textScore = findViewById(R.id.textScore);
        TextView textFormula = findViewById(R.id.textFormula);
        TextView textTickets = findViewById(R.id.textTicketsEarned);
        TextView textMission = findViewById(R.id.textMissionResult);
        TextView textDrive = findViewById(R.id.textDrive);
        TextView textGood = findViewById(R.id.textGood);
        TextView textBad = findViewById(R.id.textBad);
        TextView textWeekly = findViewById(R.id.textWeekly);
        TextView textCheer = findViewById(R.id.textCheer);
        LinearLayout weightList = findViewById(R.id.weightList);

        textDate.setText(ReportTexts.dateText(date) + " 주차 리포트");
        weightList.removeAllViews();
        if (s == null) {
            textScore.setText("-");
            textFormula.setText(R.string.report_empty);
            return;
        }
        textScore.setText(ReportTexts.scoreText(s));
        textFormula.setText(ReportTexts.formula(s));
        textTickets.setText(ReportTexts.ticketsText(s));
        textMission.setText(ReportTexts.missionText(s));
        textDrive.setText(ReportTexts.driveText(s));
        textGood.setText(s.goodPoint());
        textBad.setText(s.badPoint());
        textWeekly.setText(s.weeklyNote());
        textCheer.setText(s.cheer());

        for (String line : s.weightLines()) {
            TextView row = new TextView(this);
            row.setText(line);
            row.setTextColor(getColor(R.color.text));
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            row.setPadding(0, 6, 0, 6);
            weightList.addView(row);
        }
    }
}
