package com.todaysdrive.core.link;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 외부 라이브러리 없이 쓰는 작은 JSON 도구. Firebase REST 응답과 SQLite 저장에 쓴다.
 *
 * <p>객체는 {@code Map<String, Object>}, 배열은 {@code List<Object>}, 숫자는 정수면 Long·아니면 Double,
 * 그 밖은 String·Boolean·null로 읽는다.
 */
public final class Json {

    private Json() {
    }

    // ───────────────────────────── 쓰기 ─────────────────────────────

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            writeString(sb, (String) v);
        } else if (v instanceof Boolean) {
            sb.append(v.toString());
        } else if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                sb.append("null");
            } else if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                sb.append((long) d);
            } else {
                sb.append(String.format(Locale.ROOT, "%s", d));
            }
        } else if (v instanceof Number) {
            sb.append(((Number) v).longValue());
        } else if (v instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, String.valueOf(e.getKey()));
                sb.append(':');
                writeValue(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof Collection) {
            sb.append('[');
            boolean first = true;
            for (Object item : (Collection<?>) v) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeValue(sb, item);
            }
            sb.append(']');
        } else {
            writeString(sb, v.toString());
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    // ───────────────────────────── 읽기 ─────────────────────────────

    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.skipWs();
        Object v = p.readValue();
        p.skipWs();
        if (!p.atEnd()) {
            throw new IllegalArgumentException("JSON 뒤에 남은 글자가 있습니다: " + p.pos);
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        if (v == null) {
            return null;
        }
        if (!(v instanceof Map)) {
            throw new IllegalArgumentException("JSON 객체가 아닙니다");
        }
        return (Map<String, Object>) v;
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        boolean atEnd() {
            return pos >= s.length();
        }

        void skipWs() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        Object readValue() {
            if (atEnd()) {
                throw error("값이 없습니다");
            }
            char c = s.charAt(pos);
            switch (c) {
                case '{':
                    return readObject();
                case '[':
                    return readArray();
                case '"':
                    return readString();
                case 't':
                    expect("true");
                    return Boolean.TRUE;
                case 'f':
                    expect("false");
                    return Boolean.FALSE;
                case 'n':
                    expect("null");
                    return null;
                default:
                    return readNumber();
            }
        }

        Map<String, Object> readObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // {
            skipWs();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWs();
                String key = readString();
                skipWs();
                consume(':');
                skipWs();
                map.put(key, readValue());
                skipWs();
                char c = next();
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw error("',' 또는 '}'가 필요합니다");
                }
            }
        }

        List<Object> readArray() {
            List<Object> list = new ArrayList<>();
            pos++; // [
            skipWs();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                skipWs();
                list.add(readValue());
                skipWs();
                char c = next();
                if (c == ']') {
                    return list;
                }
                if (c != ',') {
                    throw error("',' 또는 ']'가 필요합니다");
                }
            }
        }

        String readString() {
            consume('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    char e = next();
                    switch (e) {
                        case '"':
                        case '\\':
                        case '/':
                            sb.append(e);
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'u':
                            if (pos + 4 > s.length()) {
                                throw error("잘못된 \\u 이스케이프");
                            }
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                            break;
                        default:
                            throw error("잘못된 이스케이프 \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        Object readNumber() {
            int start = pos;
            boolean decimal = false;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '.' || c == 'e' || c == 'E') {
                    decimal = true;
                } else if (!(c == '-' || c == '+' || (c >= '0' && c <= '9'))) {
                    break;
                }
                pos++;
            }
            String num = s.substring(start, pos);
            if (num.isEmpty()) {
                throw error("알 수 없는 값");
            }
            try {
                if (!decimal) {
                    return Long.parseLong(num);
                }
                return Double.parseDouble(num);
            } catch (NumberFormatException e) {
                throw error("숫자 형식 오류 " + num);
            }
        }

        void expect(String word) {
            if (!s.startsWith(word, pos)) {
                throw error(word + "가 필요합니다");
            }
            pos += word.length();
        }

        char peek() {
            if (atEnd()) {
                throw error("JSON이 중간에 끝났습니다");
            }
            return s.charAt(pos);
        }

        char next() {
            char c = peek();
            pos++;
            return c;
        }

        void consume(char c) {
            if (next() != c) {
                throw error("'" + c + "'가 필요합니다");
            }
        }

        IllegalArgumentException error(String msg) {
            return new IllegalArgumentException("JSON 오류(" + pos + "): " + msg);
        }
    }

    // ───────────────────────────── 값 꺼내기 ─────────────────────────────

    public static int asInt(Object v, int fallback) {
        return v instanceof Number ? ((Number) v).intValue() : fallback;
    }

    public static long asLong(Object v, long fallback) {
        return v instanceof Number ? ((Number) v).longValue() : fallback;
    }

    public static double asDouble(Object v, double fallback) {
        return v instanceof Number ? ((Number) v).doubleValue() : fallback;
    }

    public static boolean asBool(Object v, boolean fallback) {
        return v instanceof Boolean ? (Boolean) v : fallback;
    }

    public static String asString(Object v, String fallback) {
        return v instanceof String ? (String) v : fallback;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object v) {
        return v instanceof Map ? (Map<String, Object>) v : new LinkedHashMap<>();
    }

    /**
     * 배열을 꺼낸다. Firebase는 배열을 숫자 키 객체로 돌려줄 때가 있어 그 경우도 받아 준다.
     */
    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object v) {
        if (v instanceof List) {
            return (List<Object>) v;
        }
        List<Object> list = new ArrayList<>();
        if (v instanceof Map) {
            list.addAll(((Map<String, Object>) v).values());
        }
        return list;
    }
}
