package com.todaysdrive.phone.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.todaysdrive.core.link.DailySummary;
import com.todaysdrive.core.report.ReportTexts;
import com.todaysdrive.core.report.WeeklyStats;
import com.todaysdrive.phone.PhoneApp;
import com.todaysdrive.phone.R;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** 최근 7일 점수와 지난주 대비 변화 */
public class WeeklyActivity extends Activity {

    private PhoneApp app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly);
        app = (PhoneApp) getApplication();
    }

    @Override
    protected void onResume() {
        super.onResume();
        show(app.settings().cachedDays());
        app.run(app::syncDays, (days, error) -> {
            if (error == null) {
                show(days);
            }
        });
    }

    private void show(List<DailySummary> days) {
        LocalDate today = LocalDate.now();
        WeeklyStats thisWeek = WeeklyStats.weekEnding(today, days);
        WeeklyStats lastWeek = WeeklyStats.weekEnding(today.minusDays(7), days);

        TextView summary = findViewById(R.id.textWeekSummary);
        if (thisWeek.scoredDays() == 0) {
            summary.setText(R.string.weekly_empty);
        } else {
            summary.setText(String.format(Locale.ROOT,
                    "평균 %d점 · 받은 티켓 %d장 · %.1fkm\n회전 시 점등률 %s · 미리 켜기 %s",
                    thisWeek.averageScore(), thisWeek.tickets(), thisWeek.distanceKm(),
                    pct(thisWeek.signalRatePct()), pct(thisWeek.earlyRatePct())));
        }
        ((TextView) findViewById(R.id.textWeekCompare)).setText(WeeklyStats.compareText(thisWeek, lastWeek));

        LinearLayout list = findViewById(R.id.dayList);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            DailySummary d = MainActivity.findDay(days, date.toString());
            View row = inflater.inflate(R.layout.item_week_day, list, false);
            ((TextView) row.findViewById(R.id.textDay)).setText(ReportTexts.dateText(date).replaceFirst("^\\d+월 ", ""));
            ProgressBar bar = row.findViewById(R.id.barScore);
            TextView score = row.findViewById(R.id.textDayScore);
            if (d != null && d.scored()) {
                bar.setProgress(d.score());
                score.setText(d.score() + "점" + (d.ticketsEarned() > 0 ? " · 티켓 " + d.ticketsEarned() : ""));
                row.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ReportActivity.class);
                    intent.putExtra(ReportActivity.EXTRA_DATE, d.date());
                    startActivity(intent);
                });
            } else {
                bar.setProgress(0);
                score.setText("-");
            }
            list.addView(row);
        }
    }

    private static String pct(int value) {
        return value < 0 ? "-" : value + "%";
    }
}
