package com.todaysdrive.core.drive;

/** 기어 상태. code는 시나리오 파일에 쓰는 글자. */
public enum GearState {
    PARK("P"), REVERSE("R"), NEUTRAL("N"), DRIVE("D"), UNKNOWN("?");

    private final String code;

    GearState(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static GearState fromCode(String code) {
        for (GearState g : values()) {
            if (g.code.equalsIgnoreCase(code) || g.name().equalsIgnoreCase(code)) {
                return g;
            }
        }
        throw new IllegalArgumentException("알 수 없는 기어: " + code);
    }
}
