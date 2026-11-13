package com.todaysdrive.phone.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.todaysdrive.core.link.DailySummary;
import com.todaysdrive.core.report.ReportTexts;
import com.todaysdrive.phone.PhoneApp;
import com.todaysdrive.phone.R;

/** 차량에서 만든 주차 리포트를 휴대폰에서 다시 본다(날짜를 넘기지 않으면 가장 최근). */
public class ReportActivity extends Activity {

    public static final String EXTRA_DATE = "date";

    private PhoneApp app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        app = (PhoneApp) getApplication();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String date = getIntent().getStringExtra(EXTRA_DATE);
        show(pick(date));
        app.run(app::syncDays, (days, error) -> {
            if (error == null) {
                show(date == null ? (days.isEmpty() ? null : days.get(days.size() - 1)) : MainActivity.findDay(days, date));
            }
        });
    }

    private DailySummary pick(String date) {
        return date == null ? app.settings().latestDay() : app.settings().cachedDay(date);
    }

    private void show(DailySummary s) {
        TextView textDate = findViewById(R.id.textReportDate);
        TextView textScore = findViewById(R.id.textScore);
        TextView textFormula = findViewById(R.id.textFormula);
        LinearLayout weightList = findViewById(R.id.weightList);
        weightList.removeAllViews();
        if (s == null) {
            textDate.setText("");
            textScore.setText("-");
            textFormula.setText(R.string.report_none);
            return;
        }
        textDate.setText(ReportTexts.dateText(s.localDate()) + " 주차 리포트");
        textScore.setText(ReportTexts.scoreText(s));
        textFormula.setText(ReportTexts.formula(s));
        ((TextView) findViewById(R.id.textTicketsEarned)).setText(ReportTexts.ticketsText(s));
        ((TextView) findViewById(R.id.textMissionResult)).setText(ReportTexts.missionText(s));
        ((TextView) findViewById(R.id.textDrive)).setText(ReportTexts.driveText(s));
        ((TextView) findViewById(R.id.textGood)).setText(s.goodPoint());
        ((TextView) findViewById(R.id.textBad)).setText(s.badPoint());
        ((TextView) findViewById(R.id.textWeekly)).setText(s.weeklyNote());
        ((TextView) findViewById(R.id.textCheer)).setText(s.cheer());
        for (String line : s.weightLines()) {
            TextView row = new TextView(this);
            row.setText(line);
            row.setTextColor(getColor(R.color.text));
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            row.setPadding(0, 4, 0, 4);
            weightList.addView(row);
        }
    }
}
