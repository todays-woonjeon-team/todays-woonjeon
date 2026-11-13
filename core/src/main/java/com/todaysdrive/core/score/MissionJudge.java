package com.todaysdrive.core.score;

import com.todaysdrive.core.RuleConfig;
import com.todaysdrive.core.drive.DriveStats;
import com.todaysdrive.core.fortune.MissionType;

/** 오늘의 행운 미션 달성 판정. 달성하면 오늘의 점수에 5점을 더한다. */
public final class MissionJudge {

    public static final int MISSION_BONUS = 5;

    private final WeightCalculator weights;

    public MissionJudge(RuleConfig rules) {
        this.weights = new WeightCalculator(rules);
    }

    public boolean achieved(MissionType mission, DriveStats s) {
        switch (mission) {
            case ALL_TURNS_SIGNALED:
                return s.turns() > 0 && s.turnsSignaled() == s.turns();
            case EARLY_SIGNAL:
                return s.earlyEligible() > 0 && s.earlySignaled() * 100 >= 80 * s.earlyEligible();
            case NO_HARD_BRAKE:
                return s.harshDecel() == 0;
            case SMOOTH_START:
                return s.tripStarts() > 0 && s.harshStart() == 0;
            case SEATBELT_BEFORE_START:
                return s.tripStarts() > 0 && s.tripStartsBuckled() == s.tripStarts();
            case REST_BEFORE_2H:
                return weights.restBonusEarned(s);
            default:
                return false;
        }
    }
}
