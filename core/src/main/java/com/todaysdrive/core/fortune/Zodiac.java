package com.todaysdrive.core.fortune;

import java.time.LocalDate;

/**
 * 띠. 생년월일은 선택 입력이며 띠를 고르는 데만 쓰고 기기 밖으로 보내지 않는다.
 */
public enum Zodiac {
    RAT("쥐"), OX("소"), TIGER("호랑이"), RABBIT("토끼"), DRAGON("용"), SNAKE("뱀"),
    HORSE("말"), SHEEP("양"), MONKEY("원숭이"), ROOSTER("닭"), DOG("개"), PIG("돼지");

    private final String label;

    Zodiac(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 양력 연도 기준 띠. 1900년, 2020년이 쥐띠. */
    public static Zodiac fromYear(int year) {
        return values()[Math.floorMod(year - 4, 12)];
    }

    /**
     * 생년월일로 띠를 구한다.
     *
     * <p>띠는 보통 설날 기준이지만 설날은 해마다 날짜가 달라서, 입춘(2월 4일) 전에 태어났으면
     * 전년도 띠로 계산한다. 경계에 있는 사용자는 설정 화면에서 띠를 직접 고를 수 있다.
     */
    public static Zodiac fromBirthDate(LocalDate birthDate) {
        int year = birthDate.getYear();
        int month = birthDate.getMonthValue();
        if (month == 1 || (month == 2 && birthDate.getDayOfMonth() < 4)) {
            year -= 1;
        }
        return fromYear(year);
    }
}
