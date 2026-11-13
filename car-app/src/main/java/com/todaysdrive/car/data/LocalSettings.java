package com.todaysdrive.car.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.todaysdrive.core.fortune.Zodiac;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** 차량 앱 설정. 모두 차량 내부에만 저장한다. */
public final class LocalSettings {

    private static final String BIRTH_DATE = "birth_date";
    private static final String PAIR_ID = "pair_id";
    private static final String LINK_CONSENT = "link_consent";

    private final CarDatabase db;

    public LocalSettings(CarDatabase db) {
        this.db = db;
    }

    /** 생년월일(선택). 띠별 운세에만 쓰고 서버로 보내지 않는다. */
    public LocalDate birthDate() {
        String v = get(BIRTH_DATE);
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
        put(BIRTH_DATE, date == null ? null : date.toString());
    }

    public Zodiac zodiac() {
        LocalDate birth = birthDate();
        return birth == null ? null : Zodiac.fromBirthDate(birth);
    }

    public String pairId() {
        return get(PAIR_ID);
    }

    public void setPairId(String pairId) {
        put(PAIR_ID, pairId);
    }

    public boolean linkConsent() {
        return "1".equals(get(LINK_CONSENT));
    }

    public void setLinkConsent(boolean consent) {
        put(LINK_CONSENT, consent ? "1" : null);
    }

    private String get(String key) {
        try (Cursor c = db.getReadableDatabase().query("settings", new String[]{"value"},
                "key = ?", new String[]{key}, null, null, null)) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    private void put(String key, String value) {
        SQLiteDatabase w = db.getWritableDatabase();
        if (value == null) {
            w.delete("settings", "key = ?", new String[]{key});
            return;
        }
        ContentValues v = new ContentValues();
        v.put("key", key);
        v.put("value", value);
        w.insertWithOnConflict("settings", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
