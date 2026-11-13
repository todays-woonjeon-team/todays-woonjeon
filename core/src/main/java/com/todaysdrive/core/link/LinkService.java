package com.todaysdrive.core.link;

import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.reward.Prize;
import com.todaysdrive.core.reward.TicketLedger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * 차량 앱과 휴대폰 앱의 연동 기능.
 *
 * <pre>
 * codes/{6자리 코드}           = { pairId, expiresAt }          연결할 때 잠깐만 존재
 * pairs/{pairId}/info          = { createdAt, support[] }        차가 지원하는 신호(미션 목록 맞추기용)
 * pairs/{pairId}/days/{날짜}    = 하루 결과 요약(DailySummary)
 * pairs/{pairId}/tickets       = { grants{날짜: 장수}, spins{id: 결과} }
 * </pre>
 * 위치, 주행 원본 기록, 생년월일, 이름·연락처는 어디에도 올리지 않는다.
 */
public final class LinkService {

    private static final int MAX_RETRY = 5;

    private final RemoteStore store;
    private final LongSupplier clock;

    public LinkService(RemoteStore store) {
        this(store, System::currentTimeMillis);
    }

    public LinkService(RemoteStore store, LongSupplier clock) {
        this.store = store;
        this.clock = clock;
    }

    // ── 연결 ──

    /** 차량 앱: 새 연동 ID와 연결 코드를 만든다. 이미 연동 ID가 있으면 그것을 다시 쓴다. */
    public Pairing createPairing(String existingPairId, SignalSupport support) throws IOException {
        String pairId = PairingCodes.isValidPairId(existingPairId) ? existingPairId : PairingCodes.newPairId();
        long now = clock.getAsLong();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("createdAt", now);
        info.put("support", support.toNames());
        store.put("pairs/" + pairId + "/info", info);

        String code = null;
        for (int i = 0; i < MAX_RETRY && code == null; i++) {
            String candidate = PairingCodes.newCode();
            Map<String, Object> existing = Json.asMap(store.get("codes/" + candidate));
            long expires = Json.asLong(existing.get("expiresAt"), 0);
            if (existing.isEmpty() || expires < now) {
                code = candidate;
            }
        }
        if (code == null) {
            throw new IOException("연결 코드를 만들지 못했습니다. 잠시 후 다시 시도해 주세요");
        }
        long expiresAt = now + PairingCodes.CODE_TTL_MS;
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("pairId", pairId);
        entry.put("expiresAt", expiresAt);
        store.put("codes/" + code, entry);
        return new Pairing(pairId, code, expiresAt);
    }

    /** 휴대폰 앱: 연결 코드로 연동 ID를 받는다. 코드는 한 번 쓰면 지운다. 틀리거나 만료되면 null */
    public String claimCode(String code) throws IOException {
        if (!PairingCodes.isValidCode(code)) {
            return null;
        }
        Map<String, Object> entry = Json.asMap(store.get("codes/" + code));
        String pairId = Json.asString(entry.get("pairId"), null);
        long expiresAt = Json.asLong(entry.get("expiresAt"), 0);
        if (pairId == null) {
            return null;
        }
        store.delete("codes/" + code);
        if (expiresAt < clock.getAsLong() || !PairingCodes.isValidPairId(pairId)) {
            return null;
        }
        return pairId;
    }

    public SignalSupport fetchSupport(String pairId) throws IOException {
        Map<String, Object> info = Json.asMap(store.get("pairs/" + pairId + "/info"));
        List<Object> names = Json.asList(info.get("support"));
        return names.isEmpty() ? SignalSupport.all() : SignalSupport.fromNames(names);
    }

    // ── 하루 결과 ──

    /** 차량 앱: 하루 결과 요약과 그날 받은 티켓 수를 올린다. 같은 날을 다시 올리면 덮어쓴다. */
    public void uploadDay(String pairId, DailySummary summary) throws IOException {
        store.put("pairs/" + pairId + "/days/" + summary.date(), summary.toMap());
        store.put("pairs/" + pairId + "/tickets/grants/" + summary.date(), summary.ticketsEarned());
    }

    /** 날짜 오름차순 */
    public List<DailySummary> fetchDays(String pairId) throws IOException {
        Map<String, Object> days = Json.asMap(store.get("pairs/" + pairId + "/days"));
        List<DailySummary> list = new ArrayList<>();
        for (Object v : days.values()) {
            list.add(DailySummary.fromMap(Json.asMap(v)));
        }
        list.sort((a, b) -> a.date().compareTo(b.date()));
        return list;
    }

    // ── 티켓 ──

    public TicketLedger fetchTickets(String pairId) throws IOException {
        return TicketLedger.fromMap(Json.asMap(store.get("pairs/" + pairId + "/tickets")));
    }

    /**
     * 티켓 한 장을 쓰고 룰렛 결과를 기록한다. 다른 기기가 같은 순간에 썼으면 다시 읽어서 재시도한다.
     *
     * @return 기록된 결과. 남은 티켓이 없으면 null
     */
    public TicketLedger.Spin spendTicket(String pairId, Prize prize, String device) throws IOException {
        String path = "pairs/" + pairId + "/tickets";
        for (int i = 0; i < MAX_RETRY; i++) {
            RemoteStore.Versioned current = store.getVersioned(path);
            TicketLedger ledger = TicketLedger.fromMap(Json.asMap(current.value));
            if (ledger.balance() <= 0) {
                return null;
            }
            long now = clock.getAsLong();
            TicketLedger.Spin spin = ledger.spend(PairingCodes.newSpinId(now), prize, now, device);
            if (store.putIfMatch(path, ledger.toMap(), current.etag)) {
                return spin;
            }
        }
        throw new IOException("다른 기기와 동시에 사용 중입니다. 다시 시도해 주세요");
    }

    /**
     * 연결 전에 차량에서 돌린 룰렛 기록을 서버 장부로 옮긴다(이미 있는 기록은 건너뜀).
     * 그래야 연결한 뒤에도 같은 티켓을 두 번 쓰지 않는다.
     */
    public void importSpins(String pairId, List<TicketLedger.Spin> localSpins) throws IOException {
        if (localSpins.isEmpty()) {
            return;
        }
        String path = "pairs/" + pairId + "/tickets";
        for (int i = 0; i < MAX_RETRY; i++) {
            RemoteStore.Versioned current = store.getVersioned(path);
            TicketLedger ledger = TicketLedger.fromMap(Json.asMap(current.value));
            Set<String> known = new HashSet<>();
            for (TicketLedger.Spin s : ledger.spins()) {
                known.add(s.id);
            }
            boolean changed = false;
            for (TicketLedger.Spin s : localSpins) {
                if (known.add(s.id)) {
                    ledger.addSpin(s);
                    changed = true;
                }
            }
            if (!changed || store.putIfMatch(path, ledger.toMap(), current.etag)) {
                return;
            }
        }
        throw new IOException("룰렛 기록을 옮기지 못했습니다. 다시 시도해 주세요");
    }

    /** 연결 코드 발급 결과 */
    public static final class Pairing {
        public final String pairId;
        public final String code;
        public final long expiresAt;

        public Pairing(String pairId, String code, long expiresAt) {
            this.pairId = pairId;
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }
}
