package com.todaysdrive.phone.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.fortune.Zodiac;
import com.todaysdrive.core.link.DailySummary;
import com.todaysdrive.core.link.Json;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 휴대폰 앱 설정과 마지막으로 받은 리포트(오프라인 표시용). 모두 휴대폰 안에만 저장한다. */
public final class PhoneSettings {

    private static final String PAIR_ID = "pair_id";
    private static final String SUPPORT = "support";
    private static final String BIRTH_DATE = "birth_date";
    private static final String REMINDER_ON = "reminder_on";
    private static final String REMINDER_MINUTES = "reminder_minutes";
    private static final String NOTIFIED_AT = "notified_at";
    private static final String CACHED_DAYS = "cached_days";

    private final SharedPreferences prefs;

    public PhoneSettings(Context context) {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    public String pairId() {
        return prefs.getString(PAIR_ID, null);
    }

    public boolean isPaired() {
        return pairId() != null;
    }

    public void pair(String pairId, SignalSupport support) {
        prefs.edit()
                .putString(PAIR_ID, pairId)
                .putString(SUPPORT, String.join(",", support.toNames()))
                .apply();
    }

    public void unpair() {
        prefs.edit().remove(PAIR_ID).remove(SUPPORT).remove(CACHED_DAYS).remove(NOTIFIED_AT).apply();
    }

    /** 연결한 차가 지원하는 신호(미션 목록을 차와 맞추기 위함). 연결 전에는 전체 */
    public SignalSupport support() {
        String s = prefs.getString(SUPPORT, null);
        return s == null || s.isEmpty() ? SignalSupport.all() : SignalSupport.fromNames(Arrays.asList(s.split(",")));
    }

    /** 생년월일(선택). 차량 앱과 같은 날짜를 넣으면 같은 띠 운세가 나온다. */
    public LocalDate birthDate() {
        String v = prefs.getString(BIRTH_DATE, null);
        if (v == null) {
            return null;
        }
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public void setBirthDate(LocalDate date) {
        if (date == null) {
            prefs.edit().remove(BIRTH_DATE).apply();
        } else {
            prefs.edit().putString(BIRTH_DATE, date.toString()).apply();
        }
    }

    public Zodiac zodiac() {
        LocalDate b = birthDate();
        return b == null ? null : Zodiac.fromBirthDate(b);
    }

    public boolean reminderOn() {
        return prefs.getBoolean(REMINDER_ON, false);
    }

    /** 알림 시각(자정부터 분). 기본 오전 7시 30분 */
    public int reminderMinutes() {
        return prefs.getInt(REMINDER_MINUTES, 7 * 60 + 30);
    }

    public void setReminder(boolean on, int minutesOfDay) {
        prefs.edit().putBoolean(REMINDER_ON, on).putInt(REMINDER_MINUTES, minutesOfDay).apply();
    }

    /** 마지막으로 알린 리포트의 updatedAt */
    public long notifiedAt() {
        return prefs.getLong(NOTIFIED_AT, 0);
    }

    public void setNotifiedAt(long updatedAt) {
        prefs.edit().putLong(NOTIFIED_AT, updatedAt).apply();
    }

    public void cacheDays(List<DailySummary> days) {
        List<Object> list = new ArrayList<>();
        for (DailySummary d : days) {
            list.add(d.toMap());
        }
        prefs.edit().putString(CACHED_DAYS, Json.write(list)).apply();
    }

    /** 날짜 오름차순 */
    public List<DailySummary> cachedDays() {
        List<DailySummary> days = new ArrayList<>();
        String json = prefs.getString(CACHED_DAYS, null);
        if (json == null) {
            return days;
        }
        try {
            for (Object o : Json.asList(Json.parse(json))) {
                days.add(DailySummary.fromMap(Json.asMap(o)));
            }
        } catch (IllegalArgumentException e) {
            prefs.edit().remove(CACHED_DAYS).apply();
        }
        return days;
    }

    public DailySummary cachedDay(String date) {
        for (DailySummary d : cachedDays()) {
            if (d.date().equals(date)) {
                return d;
            }
        }
        return null;
    }

    public DailySummary latestDay() {
        List<DailySummary> days = cachedDays();
        return days.isEmpty() ? null : days.get(days.size() - 1);
    }
}
