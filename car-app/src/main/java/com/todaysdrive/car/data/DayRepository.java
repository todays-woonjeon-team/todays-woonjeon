package com.todaysdrive.car.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.todaysdrive.core.drive.DriveStats;
import com.todaysdrive.core.link.DailySummary;
import com.todaysdrive.core.link.Json;
import com.todaysdrive.core.reward.TicketLedger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 날짜별 주행 기록·리포트와 차량 룰렛 기록 */
public final class DayRepository {

    private final CarDatabase db;

    public DayRepository(CarDatabase db) {
        this.db = db;
    }

    // ── 주행 판정 누적값 ──

    public void saveStats(LocalDate date, DriveStats stats) {
        ContentValues v = new ContentValues();
        v.put("date", date.toString());
        v.put("json", Json.write(stats.toMap()));
        v.put("updated_at", System.currentTimeMillis());
        db.getWritableDatabase().insertWithOnConflict("day_stats", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public DriveStats loadStats(LocalDate date) {
        String json = queryJson("day_stats", date);
        return json == null ? null : DriveStats.fromMap(Json.parseObject(json));
    }

    // ── 주차 리포트 ──

    public void saveSummary(DailySummary summary) {
        ContentValues v = new ContentValues();
        v.put("date", summary.date());
        v.put("json", summary.toJson());
        v.put("updated_at", System.currentTimeMillis());
        db.getWritableDatabase().insertWithOnConflict("day_summary", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public DailySummary loadSummary(LocalDate date) {
        String json = queryJson("day_summary", date);
        return json == null ? null : DailySummary.fromJson(json);
    }

    /** 날짜 오름차순 */
    public List<DailySummary> allSummaries() {
        List<DailySummary> list = new ArrayList<>();
        try (Cursor c = db.getReadableDatabase().query("day_summary", new String[]{"json"},
                null, null, null, null, "date ASC")) {
            while (c.moveToNext()) {
                list.add(DailySummary.fromJson(c.getString(0)));
            }
        }
        return list;
    }

    private String queryJson(String table, LocalDate date) {
        try (Cursor c = db.getReadableDatabase().query(table, new String[]{"json"},
                "date = ?", new String[]{date.toString()}, null, null, null)) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    // ── 연결 전 룰렛 기록 ──

    public void saveSpin(TicketLedger.Spin spin) {
        ContentValues v = new ContentValues();
        v.put("id", spin.id);
        v.put("at", spin.atMs);
        v.put("prize_id", spin.prizeId);
        v.put("prize_name", spin.prizeName);
        v.put("win", spin.win ? 1 : 0);
        v.put("device", spin.device);
        db.getWritableDatabase().insertWithOnConflict("ticket_spin", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** 시간순 */
    public List<TicketLedger.Spin> spins() {
        List<TicketLedger.Spin> list = new ArrayList<>();
        try (Cursor c = db.getReadableDatabase().query("ticket_spin",
                new String[]{"id", "at", "prize_id", "prize_name", "win", "device"},
                null, null, null, null, "at ASC")) {
            while (c.moveToNext()) {
                list.add(new TicketLedger.Spin(c.getString(0), c.getLong(1), c.getString(2),
                        c.getString(3), c.getInt(4) == 1, c.getString(5)));
            }
        }
        return list;
    }

    /** 차량 안에서만 관리하는 티켓 장부(휴대폰과 연결하지 않았을 때) */
    public TicketLedger localLedger() {
        TicketLedger ledger = new TicketLedger();
        for (DailySummary s : allSummaries()) {
            ledger.grant(s.date(), s.ticketsEarned());
        }
        for (TicketLedger.Spin spin : spins()) {
            ledger.addSpin(spin);
        }
        return ledger;
    }
}
