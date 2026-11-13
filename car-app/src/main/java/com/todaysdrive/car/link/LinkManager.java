package com.todaysdrive.car.link;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.todaysdrive.car.data.DayRepository;
import com.todaysdrive.car.data.LocalSettings;
import com.todaysdrive.core.drive.SignalSupport;
import com.todaysdrive.core.link.DailySummary;
import com.todaysdrive.core.link.FirebaseRestClient;
import com.todaysdrive.core.link.LinkService;
import com.todaysdrive.core.reward.Prize;
import com.todaysdrive.core.reward.TicketLedger;
import com.todaysdrive.core.link.PairingCodes;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 휴대폰 연동과 티켓 관리.
 *
 * <p>연결하지 않았거나 동의하지 않았으면 서버와 통신하지 않고, 티켓도 차량 안에서만 관리한다.
 * 연결하면 티켓은 서버 한 곳에서 관리해 차와 휴대폰 어느 쪽에서 써도 한 번만 쓰인다.
 * 네트워크 작업은 백그라운드 스레드에서 하고, 결과는 메인 스레드로 돌려준다.
 */
public final class LinkManager {

    private static final String TAG = "LinkManager";

    public interface Callback<T> {
        void done(T result, Exception error);
    }

    private final LinkService service; // 서버 주소가 없으면 null
    private final LocalSettings settings;
    private final DayRepository days;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public LinkManager(String firebaseDbUrl, LocalSettings settings, DayRepository days) {
        this.service = firebaseDbUrl == null || firebaseDbUrl.isEmpty()
                ? null : new LinkService(new FirebaseRestClient(firebaseDbUrl));
        this.settings = settings;
        this.days = days;
    }

    /** local.properties에 서버 주소를 넣고 빌드했는지 */
    public boolean isConfigured() {
        return service != null;
    }

    public boolean isLinked() {
        return service != null && settings.linkConsent() && PairingCodes.isValidPairId(settings.pairId());
    }

    /** 연결 코드를 만든다. 동의를 저장하고, 지금까지의 리포트와 룰렛 기록을 서버로 옮긴다. */
    public void createCode(SignalSupport support, Callback<LinkService.Pairing> cb) {
        if (service == null) {
            cb.done(null, new IllegalStateException("연동 서버 주소가 설정되지 않았습니다(README 참고)"));
            return;
        }
        String existing = settings.pairId();
        io.execute(() -> {
            try {
                LinkService.Pairing pairing = service.createPairing(existing, support);
                settings.setPairId(pairing.pairId);
                settings.setLinkConsent(true);
                for (DailySummary s : days.allSummaries()) {
                    service.uploadDay(pairing.pairId, s);
                }
                service.importSpins(pairing.pairId, days.spins());
                post(cb, pairing, null);
            } catch (Exception e) {
                post(cb, null, e);
            }
        });
    }

    /** 연결을 끊는다. 서버의 룰렛 기록은 차량에도 남겨 티켓이 다시 생기지 않게 한다. */
    public void unlink(Callback<Void> cb) {
        String pairId = settings.pairId();
        io.execute(() -> {
            try {
                if (service != null && PairingCodes.isValidPairId(pairId)) {
                    for (TicketLedger.Spin spin : service.fetchTickets(pairId).spins()) {
                        days.saveSpin(spin);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "서버 룰렛 기록을 가져오지 못했습니다", e);
            }
            settings.setLinkConsent(false);
            settings.setPairId(null);
            post(cb, null, null);
        });
    }

    /** 주차 리포트를 서버에 올린다(연결·동의한 경우만). 실패하면 다음 주차 때 다시 올라간다. */
    public void uploadIfLinked(DailySummary summary) {
        if (!isLinked()) {
            return;
        }
        String pairId = settings.pairId();
        io.execute(() -> {
            try {
                service.uploadDay(pairId, summary);
            } catch (Exception e) {
                Log.w(TAG, "리포트 업로드 실패(다음에 다시 시도)", e);
            }
        });
    }

    public void ticketBalance(Callback<Integer> cb) {
        boolean linked = isLinked();
        String pairId = settings.pairId();
        io.execute(() -> {
            try {
                int balance = linked ? service.fetchTickets(pairId).balance() : days.localLedger().balance();
                post(cb, balance, null);
            } catch (Exception e) {
                post(cb, days.localLedger().balance(), e);
            }
        });
    }

    /** 티켓 한 장을 써서 룰렛 결과를 기록한다. 남은 티켓이 없으면 결과가 null */
    public void spendTicket(Prize prize, Callback<TicketLedger.Spin> cb) {
        boolean linked = isLinked();
        String pairId = settings.pairId();
        io.execute(() -> {
            try {
                TicketLedger.Spin spin;
                if (linked) {
                    spin = service.spendTicket(pairId, prize, "car");
                } else {
                    TicketLedger ledger = days.localLedger();
                    if (ledger.balance() <= 0) {
                        spin = null;
                    } else {
                        long now = System.currentTimeMillis();
                        spin = ledger.spend(PairingCodes.newSpinId(now), prize, now, "car");
                        days.saveSpin(spin);
                    }
                }
                post(cb, spin, null);
            } catch (Exception e) {
                post(cb, null, e);
            }
        });
    }

    private <T> void post(Callback<T> cb, T result, Exception error) {
        main.post(() -> cb.done(result, error));
    }
}
