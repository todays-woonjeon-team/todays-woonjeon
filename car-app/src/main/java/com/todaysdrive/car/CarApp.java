package com.todaysdrive.car;

import android.app.Application;
import android.util.Log;

import com.todaysdrive.car.data.CarDatabase;
import com.todaysdrive.car.data.DayRepository;
import com.todaysdrive.car.data.LocalSettings;
import com.todaysdrive.car.drive.DriveSession;
import com.todaysdrive.car.link.LinkManager;
import com.todaysdrive.core.RuleConfig;
import com.todaysdrive.core.fortune.FortuneCard;
import com.todaysdrive.core.fortune.FortuneTeller;
import com.todaysdrive.core.fortune.MissionType;
import com.todaysdrive.core.fortune.PhraseBook;
import com.todaysdrive.core.report.DailySummaryFactory;
import com.todaysdrive.core.reward.PrizeTable;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

/** 차량 앱 전체에서 같이 쓰는 객체를 만든다. */
public class CarApp extends Application {

    private static final String TAG = "CarApp";

    private RuleConfig rules;
    private FortuneTeller teller;
    private DailySummaryFactory summaryFactory;
    private DayRepository days;
    private LocalSettings settings;
    private LinkManager link;
    private DriveSession session;
    private final PrizeTable prizes = PrizeTable.defaults();

    @Override
    public void onCreate() {
        super.onCreate();
        rules = loadRules();
        teller = new FortuneTeller(PhraseBook.loadDefault());
        summaryFactory = new DailySummaryFactory(rules);
        CarDatabase db = new CarDatabase(this);
        days = new DayRepository(db);
        settings = new LocalSettings(db);
        link = new LinkManager(BuildConfig.FIREBASE_DB_URL, settings, days);
        session = new DriveSession(this);
    }

    /** assets/rules.properties의 판정 기준값(없으면 기본값) */
    private RuleConfig loadRules() {
        try (InputStream in = getAssets().open("rules.properties")) {
            return RuleConfig.load(in);
        } catch (IOException | IllegalArgumentException e) {
            Log.w(TAG, "rules.properties를 읽지 못해 기본값을 씁니다", e);
            return RuleConfig.defaults();
        }
    }

    /** 그날의 운세 카드. 생년월일을 넣었으면 띠별 운세, 아니면 공통 운세 */
    public FortuneCard fortuneFor(LocalDate date) {
        return teller.cardFor(date, settings.zodiac(), MissionType.poolFor(session.support(), false));
    }

    public RuleConfig rules() {
        return rules;
    }

    public DailySummaryFactory summaryFactory() {
        return summaryFactory;
    }

    public DayRepository days() {
        return days;
    }

    public LocalSettings settings() {
        return settings;
    }

    public LinkManager link() {
        return link;
    }

    public DriveSession session() {
        return session;
    }

    public PrizeTable prizes() {
        return prizes;
    }
}
