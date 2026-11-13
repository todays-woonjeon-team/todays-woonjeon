package com.todaysdrive.phone;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.todaysdrive.core.fortune.FortuneCard;
import com.todaysdrive.core.fortune.FortuneTeller;
import com.todaysdrive.core.fortune.MissionType;
import com.todaysdrive.core.fortune.PhraseBook;
import com.todaysdrive.core.link.DailySummary;
import com.todaysdrive.core.link.FirebaseRestClient;
import com.todaysdrive.core.link.LinkService;
import com.todaysdrive.core.reward.PrizeTable;
import com.todaysdrive.phone.data.PhoneSettings;
import com.todaysdrive.phone.work.Notifier;
import com.todaysdrive.phone.work.WorkScheduler;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 휴대폰 앱 전체에서 같이 쓰는 객체 */
public class PhoneApp extends Application {

    public interface Callback<T> {
        void done(T result, Exception error);
    }

    private PhoneSettings settings;
    private FortuneTeller teller;
    private LinkService link;
    private final PrizeTable prizes = PrizeTable.defaults();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        settings = new PhoneSettings(this);
        teller = new FortuneTeller(PhraseBook.loadDefault());
        String url = BuildConfig.FIREBASE_DB_URL;
        link = url == null || url.isEmpty() ? null : new LinkService(new FirebaseRestClient(url));
        Notifier.createChannels(this);
        WorkScheduler.scheduleAll(this);
    }

    /** 차량 앱과 같은 문구 파일·같은 규칙으로 만든 오늘의 운세(서버를 거치지 않음) */
    public FortuneCard fortuneFor(LocalDate date) {
        return teller.cardFor(date, settings.zodiac(), MissionType.poolFor(settings.support(), false));
    }

    /** 서버에서 하루 결과 목록을 받아 휴대폰에 저장한다(백그라운드 스레드에서 호출). */
    public List<DailySummary> syncDays() throws Exception {
        if (link == null || !settings.isPaired()) {
            return settings.cachedDays();
        }
        List<DailySummary> days = link.fetchDays(settings.pairId());
        settings.cacheDays(days);
        return days;
    }

    /** 백그라운드에서 task를 실행하고 결과를 메인 스레드로 돌려준다. */
    public <T> void run(Callable<T> task, Callback<T> cb) {
        io.execute(() -> {
            T result = null;
            Exception error = null;
            try {
                result = task.call();
            } catch (Exception e) {
                error = e;
            }
            T r = result;
            Exception err = error;
            main.post(() -> cb.done(r, err));
        });
    }

    public PhoneSettings settings() {
        return settings;
    }

    /** 서버 주소 없이 빌드했으면 null */
    public LinkService link() {
        return link;
    }

    public PrizeTable prizes() {
        return prizes;
    }
}
