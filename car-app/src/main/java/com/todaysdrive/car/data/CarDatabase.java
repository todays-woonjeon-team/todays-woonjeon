package com.todaysdrive.car.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 차량 내부 저장소. 주행 기록과 생년월일은 여기에만 두고 서버로 보내지 않는다.
 * <ul>
 *   <li>day_stats: 날짜별 주행 판정 누적값(DriveStats JSON)</li>
 *   <li>day_summary: 날짜별 주차 리포트(DailySummary JSON)</li>
 *   <li>ticket_spin: 휴대폰과 연결하기 전 차량에서 돌린 룰렛 기록</li>
 *   <li>settings: 생년월일, 연동 ID, 동의 여부</li>
 * </ul>
 */
public final class CarDatabase extends SQLiteOpenHelper {

    private static final String NAME = "todays_drive.db";
    private static final int VERSION = 1;

    public CarDatabase(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE day_stats (date TEXT PRIMARY KEY, json TEXT NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE day_summary (date TEXT PRIMARY KEY, json TEXT NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE ticket_spin (id TEXT PRIMARY KEY, at INTEGER NOT NULL, prize_id TEXT NOT NULL, "
                + "prize_name TEXT NOT NULL, win INTEGER NOT NULL, device TEXT NOT NULL)");
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 첫 버전. 스키마가 바뀌면 여기서 옮긴다.
    }
}
