package com.todaysdrive.core.link;

import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.drive.VehicleSignal;
import com.todaysdrive.core.reward.PrizeTable;
import com.todaysdrive.core.reward.TicketLedger;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LinkTest {

    private final InMemoryStore store = new InMemoryStore();
    private final AtomicLong now = new AtomicLong(1_000_000);
    private final LinkService link = new LinkService(store, now::get);

    // ── JSON ──

    @Test
    public void JSON_왕복() {
        String json = "{\"a\":1,\"b\":2.5,\"c\":\"한글 \\\"따옴표\\\"\\n줄바꿈\",\"d\":[true,false,null],\"e\":{}}";
        Map<String, Object> m = Json.parseObject(json);
        assertEquals(1L, m.get("a"));
        assertEquals(2.5, m.get("b"));
        assertEquals("한글 \"따옴표\"\n줄바꿈", m.get("c"));
        assertEquals(Arrays.asList(true, false, null), m.get("d"));
        assertEquals(m, Json.parseObject(Json.write(m)));
        assertEquals("3", Json.write(3.0));
        assertNull(Json.parse("null"));
    }

    @Test
    public void 하루_요약은_JSON으로_그대로_복원된다() {
        DailySummary s = sample("2026-11-10", 108, 1);
        DailySummary back = DailySummary.fromJson(s.toJson());
        assertEquals(s.toMap(), back.toMap());
        assertEquals(2, back.weightLines().size());
    }

    @Test
    public void 요약에는_위치나_개인정보_항목이_없다() {
        String json = sample("2026-11-10", 108, 1).toJson().toLowerCase();
        for (String banned : new String[]{"lat", "lng", "location", "birth", "name\"", "phone", "email"}) {
            assertFalse(banned, json.contains(banned));
        }
    }

    static DailySummary sample(String date, int score, int tickets) {
        return DailySummary.builder(date)
                .score(true, score, 1200)
                .fortune("GIL", "GIL-3")
                .mission("ALL_TURNS_SIGNALED", "오늘 회전은 전부 깜빡이와 함께", false)
                .tickets(tickets, 1, 0)
                .report("회전 5번 중 4번 깜빡이를 켰어요.", "다만 미리 켠 건 4번 중 1번이에요.", "수고했어요", "")
                .weightLines(Arrays.asList("회전 시 방향지시등 · 5번 중 4번(80%) · +0.1",
                        "부드러운 주행 · 급조작 1번, 10km당 0.8번 · +0.1"))
                .drive(12.43, 28)
                .counts(5, 4, 4, 1, 1)
                .updatedAt(123)
                .build();
    }

    // ── 연결 ──

    @Test
    public void 연결_코드로_차와_휴대폰을_묶는다() throws Exception {
        SignalSupport support = SignalSupport.of(VehicleSignal.SPEED, VehicleSignal.TURN_SIGNAL);
        LinkService.Pairing pairing = link.createPairing(null, support);
        assertTrue(PairingCodes.isValidCode(pairing.code));
        assertTrue(PairingCodes.isValidPairId(pairing.pairId));

        String pairId = link.claimCode(pairing.code);
        assertEquals(pairing.pairId, pairId);
        assertNull("한 번 쓴 코드는 지운다", link.claimCode(pairing.code));
        assertEquals(support.signals(), link.fetchSupport(pairId).signals());
    }

    @Test
    public void 만료된_코드나_틀린_코드는_거절() throws Exception {
        LinkService.Pairing pairing = link.createPairing(null, SignalSupport.all());
        now.addAndGet(PairingCodes.CODE_TTL_MS + 1);
        assertNull(link.claimCode(pairing.code));
        assertNull(link.claimCode("12ab56"));
        assertNull(link.claimCode("000000"));
    }

    @Test
    public void 다시_연결해도_같은_연동_ID를_쓴다() throws Exception {
        LinkService.Pairing first = link.createPairing(null, SignalSupport.all());
        LinkService.Pairing second = link.createPairing(first.pairId, SignalSupport.all());
        assertEquals(first.pairId, second.pairId);
    }

    // ── 하루 결과와 티켓 ──

    @Test
    public void 하루_결과를_올리면_티켓도_함께_쌓인다() throws Exception {
        String pairId = PairingCodes.newPairId();
        link.uploadDay(pairId, sample("2026-11-10", 108, 1));
        link.uploadDay(pairId, sample("2026-11-09", 131, 2));
        link.uploadDay(pairId, sample("2026-11-10", 108, 1)); // 다시 올려도 덮어씀

        List<DailySummary> days = link.fetchDays(pairId);
        assertEquals(2, days.size());
        assertEquals("2026-11-09", days.get(0).date());
        assertEquals(3, link.fetchTickets(pairId).balance());
    }

    @Test
    public void 티켓은_어느_기기에서_써도_한_번만_쓰인다() throws Exception {
        String pairId = PairingCodes.newPairId();
        link.uploadDay(pairId, sample("2026-11-10", 108, 1));

        TicketLedger.Spin spin = link.spendTicket(pairId, PrizeTable.defaults().byId("coffee"), "phone");
        assertNotNull(spin);
        assertNull("남은 티켓이 없으면 돌릴 수 없음",
                link.spendTicket(pairId, PrizeTable.defaults().byId("coffee"), "car"));
        assertEquals(0, link.fetchTickets(pairId).balance());
    }

    @Test
    public void 동시에_다른_기기가_바꾸면_다시_읽고_재시도() throws Exception {
        String pairId = PairingCodes.newPairId();
        link.uploadDay(pairId, sample("2026-11-09", 108, 1));
        // 휴대폰이 읽은 직후 차량이 오늘 티켓을 올림 → 첫 쓰기는 거절되고 다시 읽어서 성공
        store.beforeNextConditionalPut = () -> store.put(
                "pairs/" + pairId + "/tickets/grants/2026-11-10", 2);

        assertNotNull(link.spendTicket(pairId, PrizeTable.defaults().byId("air"), "phone"));
        TicketLedger ledger = link.fetchTickets(pairId);
        assertEquals(3, ledger.granted());
        assertEquals(1, ledger.used());
        assertEquals(2, ledger.balance());
    }

    @Test
    public void 연결_전_차량에서_쓴_티켓도_서버_장부에_옮긴다() throws Exception {
        String pairId = PairingCodes.newPairId();
        link.uploadDay(pairId, sample("2026-11-09", 131, 2));
        TicketLedger local = new TicketLedger();
        local.grant("2026-11-09", 2);
        TicketLedger.Spin used = local.spend("s100-0001", PrizeTable.defaults().byId("coffee"), 100, "car");

        link.importSpins(pairId, java.util.Collections.singletonList(used));
        link.importSpins(pairId, java.util.Collections.singletonList(used)); // 두 번 옮겨도 한 번만
        assertEquals(1, link.fetchTickets(pairId).balance());
    }

    @Test
    public void Firebase_주소_형식() {
        FirebaseRestClient client = new FirebaseRestClient("https://demo-default-rtdb.firebaseio.com/");
        assertEquals("https://demo-default-rtdb.firebaseio.com/pairs/x/days.json", client.urlFor("pairs/x/days"));
    }
}
