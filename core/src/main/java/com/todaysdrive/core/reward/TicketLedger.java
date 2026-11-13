package com.todaysdrive.core.reward;

import com.todaysdrive.core.link.Json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 티켓 장부. 날짜별로 받은 장수와 룰렛을 돌린 기록으로 남은 티켓을 계산한다.
 *
 * <p>받은 티켓은 날짜를 키로 저장하므로 같은 날 결과를 여러 번 올려도 두 번 쌓이지 않는다.
 * 쓴 티켓은 룰렛 한 번마다 기록 하나라서, 차량과 휴대폰 어느 쪽에서 써도 한 번만 쓰인다.
 */
public final class TicketLedger {

    private final TreeMap<String, Integer> grants = new TreeMap<>();
    private final LinkedHashMap<String, Spin> spins = new LinkedHashMap<>();

    public void grant(String date, int tickets) {
        if (tickets <= 0) {
            grants.remove(date);
        } else {
            grants.put(date, tickets);
        }
    }

    public int granted() {
        int sum = 0;
        for (int n : grants.values()) {
            sum += n;
        }
        return sum;
    }

    public int used() {
        return spins.size();
    }

    public int balance() {
        return granted() - used();
    }

    /** 티켓 한 장을 써서 룰렛 결과를 기록한다. 남은 티켓이 없으면 예외. */
    public Spin spend(String spinId, Prize prize, long atMs, String device) {
        if (balance() <= 0) {
            throw new IllegalStateException("남은 티켓이 없습니다");
        }
        if (spins.containsKey(spinId)) {
            throw new IllegalArgumentException("이미 기록된 룰렛: " + spinId);
        }
        Spin spin = new Spin(spinId, atMs, prize.id(), prize.name(), prize.isWin(), device);
        spins.put(spinId, spin);
        return spin;
    }

    public void addSpin(Spin spin) {
        spins.put(spin.id, spin);
    }

    /** 최근 순 */
    public List<Spin> spins() {
        List<Spin> list = new ArrayList<>(spins.values());
        Collections.reverse(list);
        return list;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("grants", new LinkedHashMap<String, Object>(grants));
        Map<String, Object> s = new LinkedHashMap<>();
        for (Spin spin : spins.values()) {
            s.put(spin.id, spin.toMap());
        }
        m.put("spins", s);
        return m;
    }

    public static TicketLedger fromMap(Map<String, Object> m) {
        TicketLedger ledger = new TicketLedger();
        if (m == null) {
            return ledger;
        }
        for (Map.Entry<String, Object> e : Json.asMap(m.get("grants")).entrySet()) {
            ledger.grant(e.getKey(), Json.asInt(e.getValue(), 0));
        }
        List<Spin> loaded = new ArrayList<>();
        for (Map.Entry<String, Object> e : Json.asMap(m.get("spins")).entrySet()) {
            loaded.add(Spin.fromMap(e.getKey(), Json.asMap(e.getValue())));
        }
        loaded.sort((a, b) -> Long.compare(a.atMs, b.atMs));
        for (Spin s : loaded) {
            ledger.addSpin(s);
        }
        return ledger;
    }

    /** 룰렛 한 번 */
    public static final class Spin {
        public final String id;
        public final long atMs;
        public final String prizeId;
        public final String prizeName;
        public final boolean win;
        public final String device;

        public Spin(String id, long atMs, String prizeId, String prizeName, boolean win, String device) {
            this.id = id;
            this.atMs = atMs;
            this.prizeId = prizeId;
            this.prizeName = prizeName;
            this.win = win;
            this.device = device;
        }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("at", atMs);
            m.put("prizeId", prizeId);
            m.put("prizeName", prizeName);
            m.put("win", win);
            m.put("device", device);
            return m;
        }

        static Spin fromMap(String id, Map<String, Object> m) {
            return new Spin(id, Json.asLong(m.get("at"), 0), Json.asString(m.get("prizeId"), ""),
                    Json.asString(m.get("prizeName"), ""), Json.asBool(m.get("win"), false),
                    Json.asString(m.get("device"), ""));
        }
    }
}
