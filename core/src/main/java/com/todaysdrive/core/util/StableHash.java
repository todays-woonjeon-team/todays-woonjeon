package com.todaysdrive.core.util;

import java.nio.charset.StandardCharsets;

/**
 * 기기와 JVM에 관계없이 항상 같은 값을 내는 해시(FNV-1a 64bit).
 *
 * <p>차량 앱과 휴대폰 앱이 서버를 거치지 않고도 같은 날 같은 운세를 고르도록 쓴다.
 * {@link String#hashCode()}도 명세상 고정이지만, 의도를 드러내기 위해 별도로 둔다.
 */
public final class StableHash {

    private static final long OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long PRIME = 0x100000001b3L;

    private StableHash() {
    }

    public static long fnv1a64(String text) {
        long hash = OFFSET_BASIS;
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
            hash ^= (b & 0xff);
            hash *= PRIME;
        }
        return hash;
    }

    /** key로 0 이상 bound 미만의 인덱스를 고른다. */
    public static int index(String key, int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound는 1 이상이어야 합니다: " + bound);
        }
        return (int) Long.remainderUnsigned(fnv1a64(key), bound);
    }
}
