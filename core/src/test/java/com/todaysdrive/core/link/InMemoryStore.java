package com.todaysdrive.core.link;

import java.util.LinkedHashMap;
import java.util.Map;

/** 테스트용 저장소. Firebase처럼 경로로 읽고 쓰며, 값의 JSON으로 ETag를 만든다. */
final class InMemoryStore implements RemoteStore {

    private Map<String, Object> root = new LinkedHashMap<>();

    /** 다음 putIfMatch 직전에 실행할 동작(다른 기기가 끼어드는 상황 재현) */
    Runnable beforeNextConditionalPut;

    @Override
    public Object get(String path) {
        Object node = root;
        for (String key : path.split("/")) {
            if (!(node instanceof Map)) {
                return null;
            }
            node = ((Map<?, ?>) node).get(key);
        }
        return copy(node);
    }

    @Override
    public Versioned getVersioned(String path) {
        Object v = get(path);
        return new Versioned(v, etag(v));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void put(String path, Object value) {
        String[] keys = path.split("/");
        Map<String, Object> node = root;
        for (int i = 0; i < keys.length - 1; i++) {
            Object child = node.get(keys[i]);
            if (!(child instanceof Map)) {
                child = new LinkedHashMap<String, Object>();
                node.put(keys[i], child);
            }
            node = (Map<String, Object>) child;
        }
        Object v = copy(value);
        if (v == null) {
            node.remove(keys[keys.length - 1]);
        } else {
            node.put(keys[keys.length - 1], v);
        }
    }

    @Override
    public boolean putIfMatch(String path, Object value, String etag) {
        if (beforeNextConditionalPut != null) {
            Runnable r = beforeNextConditionalPut;
            beforeNextConditionalPut = null;
            r.run();
        }
        if (!etag(get(path)).equals(etag)) {
            return false;
        }
        put(path, value);
        return true;
    }

    @Override
    public void delete(String path) {
        put(path, null);
    }

    private static Object copy(Object v) {
        return v == null ? null : Json.parse(Json.write(v));
    }

    private static String etag(Object v) {
        return Integer.toHexString(Json.write(v).hashCode());
    }
}
