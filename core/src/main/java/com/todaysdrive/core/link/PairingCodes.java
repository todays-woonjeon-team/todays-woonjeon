package com.todaysdrive.core.link;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * 연결 코드와 연동 ID를 만든다.
 *
 * <p>연동 ID는 추측할 수 없는 128bit 난수로, 이름·전화번호 없이 차와 휴대폰을 묶는 열쇠다.
 * 연결 코드는 휴대폰에 입력하기 쉬운 6자리 숫자이며 10분 뒤 만료되고 한 번 쓰면 지운다.
 */
public final class PairingCodes {

    public static final long CODE_TTL_MS = 10 * 60_000L;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PairingCodes() {
    }

    /** 32자리 16진수 */
    public static String newPairId() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        }
        return sb.toString();
    }

    /** 000000~999999 */
    public static String newCode() {
        return String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
    }

    public static boolean isValidCode(String code) {
        return code != null && code.matches("\\d{6}");
    }

    public static boolean isValidPairId(String id) {
        return id != null && id.matches("[0-9a-f]{32}");
    }

    /** 룰렛 기록 ID(시간순 정렬 가능) */
    public static String newSpinId(long nowMs) {
        return "s" + nowMs + "-" + String.format(Locale.ROOT, "%04x", RANDOM.nextInt(0x10000));
    }
}
