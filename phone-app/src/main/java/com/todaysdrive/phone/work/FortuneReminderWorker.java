package com.todaysdrive.phone.work;

import android.content.Context;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.todaysdrive.core.fortune.FortuneCard;
import com.todaysdrive.phone.PhoneApp;

import java.time.LocalDate;

/** 출근 전 오늘의 운세·미션 알림 */
public class FortuneReminderWorker extends Worker {

    public FortuneReminderWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        PhoneApp app = (PhoneApp) getApplicationContext();
        FortuneCard card = app.fortuneFor(LocalDate.now());
        Notifier.fortune(app, card.headline(),
                card.fortuneText() + "\n오늘의 미션: " + card.missionTitle() + "\n꿀팁: " + card.tip());
        WorkScheduler.scheduleReminder(app); // 다음 날 예약
        return Result.success();
    }
}
