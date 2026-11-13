package com.todaysdrive.core.link;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Firebase Realtime Database REST API 클라이언트.
 *
 * <p>Firebase SDK(google-services.json) 없이 HTTPS로만 통신해서 저장소에 계정 정보가 들어가지 않는다.
 * 데이터베이스 주소는 local.properties의 {@code firebase.dbUrl}로 넣는다.
 *
 * <p>안드로이드 메인 스레드에서 부르면 안 된다(네트워크 작업).
 */
public final class FirebaseRestClient implements RemoteStore {

    private static final int TIMEOUT_MS = 10_000;

    private final String baseUrl;

    /** @param databaseUrl 예) https://프로젝트-default-rtdb.asia-southeast1.firebasedatabase.app */
    public FirebaseRestClient(String databaseUrl) {
        if (databaseUrl == null || !databaseUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Firebase 주소는 https://로 시작해야 합니다");
        }
        this.baseUrl = databaseUrl.endsWith("/") ? databaseUrl.substring(0, databaseUrl.length() - 1) : databaseUrl;
    }

    String urlFor(String path) {
        String clean = path.startsWith("/") ? path.substring(1) : path;
        return baseUrl + "/" + clean + ".json";
    }

    @Override
    public Object get(String path) throws IOException {
        Response r = send("GET", path, null, false, null);
        r.throwIfError();
        return Json.parse(r.body);
    }

    @Override
    public Versioned getVersioned(String path) throws IOException {
        Response r = send("GET", path, null, true, null);
        r.throwIfError();
        return new Versioned(Json.parse(r.body), r.etag);
    }

    @Override
    public void put(String path, Object value) throws IOException {
        send("PUT", path, Json.write(value), false, null).throwIfError();
    }

    @Override
    public boolean putIfMatch(String path, Object value, String etag) throws IOException {
        Response r = send("PUT", path, Json.write(value), false, etag);
        if (r.code == 412) {
            return false; // 그 사이 다른 기기가 바꿈
        }
        r.throwIfError();
        return true;
    }

    @Override
    public void delete(String path) throws IOException {
        send("DELETE", path, null, false, null).throwIfError();
    }

    private Response send(String method, String path, String body, boolean wantEtag, String ifMatch)
            throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(urlFor(path)).toURL().openConnection();
        try {
            conn.setRequestMethod(method);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");
            if (wantEtag) {
                conn.setRequestProperty("X-Firebase-ETag", "true");
            }
            if (ifMatch != null) {
                conn.setRequestProperty("if-match", ifMatch);
            }
            if (body != null) {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(bytes);
                }
            }
            int code = conn.getResponseCode();
            InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String text = in == null ? "" : readAll(in);
            return new Response(code, text, conn.getHeaderField("ETag"));
        } finally {
            conn.disconnect();
        }
    }

    private static String readAll(InputStream in) throws IOException {
        try (InputStream input = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = input.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static final class Response {
        final int code;
        final String body;
        final String etag;

        Response(int code, String body, String etag) {
            this.code = code;
            this.body = body.isEmpty() ? "null" : body;
            this.etag = etag;
        }

        void throwIfError() throws IOException {
            if (code >= 300) {
                throw new IOException("서버 응답 " + code + ": " + body);
            }
        }
    }
}
