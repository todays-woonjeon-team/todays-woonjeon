package com.todaysdrive.core.drive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 이 차에서 받을 수 있는 신호 목록.
 *
 * <p>Vehicle SDK의 checkXXXCapabilities 결과로 만든다. 지원하지 않는 신호에 기대는 평가 항목과
 * 미션은 빼고 계산한다.
 */
public final class SignalSupport {

    private final Set<VehicleSignal> signals;

    private SignalSupport(Set<VehicleSignal> signals) {
        this.signals = Collections.unmodifiableSet(signals);
    }

    public static SignalSupport all() {
        return new SignalSupport(EnumSet.allOf(VehicleSignal.class));
    }

    public static SignalSupport of(VehicleSignal first, VehicleSignal... rest) {
        return new SignalSupport(EnumSet.of(first, rest));
    }

    public static SignalSupport of(Collection<VehicleSignal> signals) {
        return signals.isEmpty()
                ? new SignalSupport(EnumSet.noneOf(VehicleSignal.class))
                : new SignalSupport(EnumSet.copyOf(signals));
    }

    public boolean has(VehicleSignal signal) {
        return signals.contains(signal);
    }

    public boolean hasAll(Collection<VehicleSignal> required) {
        return signals.containsAll(required);
    }

    public Set<VehicleSignal> signals() {
        return signals;
    }

    /** 서버 저장용 이름 목록 */
    public List<String> toNames() {
        List<String> names = new ArrayList<>();
        for (VehicleSignal s : signals) {
            names.add(s.name());
        }
        return names;
    }

    /** 모르는 이름은 무시한다(앱 버전이 달라도 깨지지 않도록). */
    public static SignalSupport fromNames(Collection<?> names) {
        EnumSet<VehicleSignal> set = EnumSet.noneOf(VehicleSignal.class);
        if (names != null) {
            for (Object n : names) {
                for (VehicleSignal s : VehicleSignal.values()) {
                    if (s.name().equals(String.valueOf(n))) {
                        set.add(s);
                    }
                }
            }
        }
        return new SignalSupport(set);
    }

    @Override
    public String toString() {
        return signals.toString();
    }
}
