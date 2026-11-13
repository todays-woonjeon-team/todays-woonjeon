package com.todaysdrive.core.fortune;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 운세·미션·꿀팁·응원 문구 묶음.
 *
 * <p>문구는 팀이 직접 써서 {@code fortune/phrases.tsv}에 넣는다. 차량 앱과 휴대폰 앱이 같은 파일을
 * 쓰기 때문에 서버 없이도 같은 운세가 나온다.
 *
 * <pre>
 * # 종류 [TAB] 키 [TAB] 문구
 * FORTUNE  DAEGIL  기다리던 소식이 들려오는 날...
 * TIP      -       깜빡이는 방향을 바꾸기 3초 전부터
 * CHEER    -       오늘도 무사히 도착한 당신, 수고했어요
 * MISSION  NO_HARD_BRAKE  급브레이크 없는 하루
 * </pre>
 */
public final class PhraseBook {

    public static final String DEFAULT_RESOURCE = "/fortune/phrases.tsv";

    private final Map<FortuneGrade, List<String>> fortunes;
    private final List<String> tips;
    private final List<String> cheers;
    private final Map<MissionType, String> missionTitles;

    private PhraseBook(Map<FortuneGrade, List<String>> fortunes, List<String> tips,
                       List<String> cheers, Map<MissionType, String> missionTitles) {
        this.fortunes = fortunes;
        this.tips = Collections.unmodifiableList(tips);
        this.cheers = Collections.unmodifiableList(cheers);
        this.missionTitles = missionTitles;
    }

    /** 공통 모듈에 들어 있는 기본 문구 파일을 읽는다. */
    public static PhraseBook loadDefault() {
        try (InputStream in = PhraseBook.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("문구 파일을 찾을 수 없습니다: " + DEFAULT_RESOURCE);
            }
            return load(in);
        } catch (IOException e) {
            throw new IllegalStateException("문구 파일을 읽지 못했습니다", e);
        }
    }

    public static PhraseBook load(InputStream in) throws IOException {
        Map<FortuneGrade, List<String>> fortunes = new EnumMap<>(FortuneGrade.class);
        for (FortuneGrade g : FortuneGrade.values()) {
            fortunes.put(g, new ArrayList<>());
        }
        List<String> tips = new ArrayList<>();
        List<String> cheers = new ArrayList<>();
        Map<MissionType, String> missions = new EnumMap<>(MissionType.class);

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        int lineNo = 0;
        while ((line = reader.readLine()) != null) {
            lineNo++;
            if (lineNo == 1 && line.startsWith("﻿")) {
                line = line.substring(1); // 엑셀에서 저장한 BOM 제거
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] cols = line.split("\t", 3);
            if (cols.length < 3 || cols[2].trim().isEmpty()) {
                throw new IOException(lineNo + "번째 줄 형식 오류(종류, 키, 문구를 탭으로 구분): " + line);
            }
            String kind = cols[0].trim();
            String key = cols[1].trim();
            String text = cols[2].trim();
            switch (kind) {
                case "FORTUNE":
                    fortunes.get(parseEnum(FortuneGrade.class, key, lineNo)).add(text);
                    break;
                case "TIP":
                    tips.add(text);
                    break;
                case "CHEER":
                    cheers.add(text);
                    break;
                case "MISSION":
                    missions.put(parseEnum(MissionType.class, key, lineNo), text);
                    break;
                default:
                    throw new IOException(lineNo + "번째 줄: 알 수 없는 종류 " + kind);
            }
        }
        for (Map.Entry<FortuneGrade, List<String>> e : fortunes.entrySet()) {
            if (e.getValue().isEmpty()) {
                throw new IOException(e.getKey().label() + " 운세 문구가 하나도 없습니다");
            }
            e.setValue(Collections.unmodifiableList(e.getValue()));
        }
        if (tips.isEmpty() || cheers.isEmpty()) {
            throw new IOException("꿀팁(TIP)과 응원(CHEER) 문구가 각각 하나 이상 필요합니다");
        }
        return new PhraseBook(fortunes, tips, cheers, missions);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String key, int lineNo)
            throws IOException {
        try {
            return Enum.valueOf(type, key);
        } catch (IllegalArgumentException e) {
            throw new IOException(lineNo + "번째 줄: 알 수 없는 키 " + key);
        }
    }

    public List<String> fortunes(FortuneGrade grade) {
        return fortunes.get(grade);
    }

    public List<String> tips() {
        return tips;
    }

    public List<String> cheers() {
        return cheers;
    }

    public String missionTitle(MissionType mission) {
        String title = missionTitles.get(mission);
        return title != null ? title : mission.defaultTitle();
    }

    public int size() {
        int n = tips.size() + cheers.size();
        for (List<String> list : fortunes.values()) {
            n += list.size();
        }
        return n;
    }
}
