package com.todaysdrive.phone.work;

import android.content.Context;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.todaysdrive.core.link.DailySummary;
import com.todaysdrive.core.report.ReportTexts;
import com.todaysdrive.phone.PhoneApp;
import com.todaysdrive.phone.R;
import com.todaysdrive.phone.data.PhoneSettings;

import java.util.List;

/** 새 주차 리포트가 올라왔으면 알림을 보낸다. 차량이 주차 후에 올리므로 알림도 주차 후에만 온다. */
public class ReportCheckWorker extends Worker {

    public ReportCheckWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        PhoneApp app = (PhoneApp) getApplicationContext();
        PhoneSettings settings = app.settings();
        try {
            List<DailySummary> days = app.syncDays();
            if (days.isEmpty()) {
                return Result.success();
            }
            DailySummary latest = days.get(days.size() - 1);
            if (latest.updatedAt() > settings.notifiedAt()) {
                settings.setNotifiedAt(latest.updatedAt());
                Context c = getApplicationContext();
                String text = ReportTexts.scoreText(latest) + " · " + ReportTexts.ticketsText(latest)
                        + "\n" + latest.goodPoint();
                Notifier.report(c, latest.date(), c.getString(R.string.notify_report_title), text);
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
