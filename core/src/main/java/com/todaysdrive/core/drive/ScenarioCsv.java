package com.todaysdrive.core.drive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 시나리오 CSV 읽기·쓰기. 시연용 주행을 파일로 만들어 차량 앱 assets/scenarios에 넣을 수 있다.
 *
 * <pre>
 * time_ms,signal,value
 * 0,GEAR,P
 * 0,SEATBELT,1
 * 1000,GEAR,D
 * 1500,SPEED,2.0
 * 9000,TURN_SIGNAL,LEFT
 * 9200,STEERING,-36
 * </pre>
 */
public final class ScenarioCsv {

    public static final String HEADER = "time_ms,signal,value";

    private ScenarioCsv() {
    }

    public static List<SignalEvent> read(InputStream in) throws IOException {
        List<SignalEvent> events = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        int lineNo = 0;
        while ((line = reader.readLine()) != null) {
            lineNo++;
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("#") || s.startsWith("time_ms")) {
                continue;
            }
            String[] cols = s.split(",");
            if (cols.length != 3) {
                throw new IOException(lineNo + "번째 줄 형식 오류: " + line);
            }
            try {
                events.add(parse(Long.parseLong(cols[0].trim()), cols[1].trim(), cols[2].trim()));
            } catch (IllegalArgumentException e) {
                throw new IOException(lineNo + "번째 줄 값 오류: " + line, e);
            }
        }
        return events;
    }

    private static SignalEvent parse(long t, String signal, String value) {
        switch (VehicleSignal.valueOf(signal.toUpperCase(Locale.ROOT))) {
            case SPEED:
                return SignalEvent.speed(t, Double.parseDouble(value));
            case STEERING:
                return SignalEvent.steering(t, Double.parseDouble(value));
            case GEAR:
                return SignalEvent.gear(t, GearState.fromCode(value));
            case TURN_SIGNAL:
                return SignalEvent.turnSignal(t, TurnSignalState.valueOf(value.toUpperCase(Locale.ROOT)));
            case BRAKE:
                return SignalEvent.brake(t, parseBool(value));
            case WIPER:
                return SignalEvent.wiper(t, parseBool(value));
            case SEATBELT:
                return SignalEvent.seatbelt(t, parseBool(value));
            default:
                throw new IllegalArgumentException(signal);
        }
    }

    private static boolean parseBool(String v) {
        return v.equals("1") || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("on");
    }

    public static String write(List<SignalEvent> events) {
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (SignalEvent e : events) {
            sb.append(e.timeMs()).append(',').append(e.signal()).append(',');
            switch (e.signal()) {
                case GEAR:
                    sb.append(e.gearValue().code());
                    break;
                case TURN_SIGNAL:
                    sb.append(e.turnSignalValue());
                    break;
                case BRAKE:
                case WIPER:
                case SEATBELT:
                    sb.append(e.isOn() ? 1 : 0);
                    break;
                default:
                    sb.append(String.format(Locale.ROOT, "%.2f", e.value()));
                    break;
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
