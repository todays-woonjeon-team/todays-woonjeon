package com.todaysdrive.phone.work;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.todaysdrive.phone.R;
import com.todaysdrive.phone.ui.MainActivity;
import com.todaysdrive.phone.ui.ReportActivity;

/** 휴대폰 알림. 운세 알림은 사용자가 정한 시각에, 리포트 알림은 주차 후에만 보낸다. */
public final class Notifier {

    private static final String CHANNEL_FORTUNE = "fortune";
    private static final String CHANNEL_REPORT = "report";
    private static final int ID_FORTUNE = 10;
    private static final int ID_REPORT = 11;

    private Notifier() {
    }

    public static void createChannels(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL_FORTUNE,
                context.getString(R.string.channel_fortune), NotificationManager.IMPORTANCE_DEFAULT));
        nm.createNotificationChannel(new NotificationChannel(CHANNEL_REPORT,
                context.getString(R.string.channel_report), NotificationManager.IMPORTANCE_DEFAULT));
    }

    public static void fortune(Context context, String title, String text) {
        post(context, CHANNEL_FORTUNE, ID_FORTUNE, title, text, new Intent(context, MainActivity.class));
    }

    public static void report(Context context, String date, String title, String text) {
        Intent intent = new Intent(context, ReportActivity.class);
        intent.putExtra(ReportActivity.EXTRA_DATE, date);
        post(context, CHANNEL_REPORT, ID_REPORT, title, text, intent);
    }

    private static void post(Context context, String channel, int id, String title, String text, Intent intent) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(context, id, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_stat_drive)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build();
        context.getSystemService(NotificationManager.class).notify(id, n);
    }
}
