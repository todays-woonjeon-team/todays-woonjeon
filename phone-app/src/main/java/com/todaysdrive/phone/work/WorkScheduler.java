package com.todaysdrive.phone.work;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.todaysdrive.phone.data.PhoneSettings;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

/** WorkManager 작업 예약 */
public final class WorkScheduler {

    private static final String REPORT_CHECK = "report-check";
    private static final String FORTUNE_REMINDER = "fortune-reminder";

    private WorkScheduler() {
    }

    public static void scheduleAll(Context context) {
        scheduleReportCheck(context);
        scheduleReminder(context);
    }

    /** 연결돼 있으면 15분마다 새 리포트가 있는지 확인한다(WorkManager 최소 간격). */
    public static void scheduleReportCheck(Context context) {
        WorkManager wm = WorkManager.getInstance(context);
        if (!new PhoneSettings(context).isPaired()) {
            wm.cancelUniqueWork(REPORT_CHECK);
            return;
        }
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ReportCheckWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build();
        wm.enqueueUniquePeriodicWork(REPORT_CHECK, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    /** 사용자가 정한 시각에 오늘의 운세 알림. 알림을 보낸 뒤 다음 날 것을 다시 예약한다. */
    public static void scheduleReminder(Context context) {
        WorkManager wm = WorkManager.getInstance(context);
        PhoneSettings settings = new PhoneSettings(context);
        if (!settings.reminderOn()) {
            wm.cancelUniqueWork(FORTUNE_REMINDER);
            return;
        }
        int minutes = settings.reminderMinutes();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.toLocalDate().atTime(LocalTime.of(minutes / 60, minutes % 60));
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        long delay = Duration.between(now, next).toMillis();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FortuneReminderWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();
        wm.enqueueUniqueWork(FORTUNE_REMINDER, ExistingWorkPolicy.REPLACE, request);
    }
}
