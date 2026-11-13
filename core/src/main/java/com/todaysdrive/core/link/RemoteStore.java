package com.todaysdrive.core.link;

import java.io.IOException;

/**
 * 연동 서버 저장소. 실제로는 {@link FirebaseRestClient}, 테스트에서는 메모리 저장소를 쓴다.
 * 경로는 "pairs/abc/days/2026-11-10"처럼 슬래시로 구분한다.
 */
public interface RemoteStore {

    /** 값이 없으면 null */
    Object get(String path) throws IOException;

    /** 값과 함께 버전 표시(ETag)를 읽는다. 동시에 티켓을 쓰는 것을 막을 때 쓴다. */
    Versioned getVersioned(String path) throws IOException;

    void put(String path, Object value) throws IOException;

    /** 읽은 뒤 아무도 바꾸지 않았을 때만 쓴다. 그 사이 바뀌었으면 false */
    boolean putIfMatch(String path, Object value, String etag) throws IOException;

    void delete(String path) throws IOException;

    final class Versioned {
        public final Object value;
        public final String etag;

        public Versioned(Object value, String etag) {
            this.value = value;
            this.etag = etag;
        }
    }
}
