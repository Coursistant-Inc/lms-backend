package com.coursistant.lms.shared.idempotency;

import java.util.Base64;

/**
 * Serialization helper for idempotency entries stored in Redis.
 * Format: "STATE:fingerprint:httpStatus:base64Body"
 */
public class IdempotencyRecord {

    public enum State { PENDING, DONE }

    private final State state;
    private final String fingerprint;
    private final int httpStatus;
    private final byte[] body;

    private IdempotencyRecord(State state, String fingerprint, int httpStatus, byte[] body) {
        this.state = state;
        this.fingerprint = fingerprint;
        this.httpStatus = httpStatus;
        this.body = body;
    }

    public static String pending(String fingerprint) {
        return "PENDING:" + fingerprint + "::";
    }

    public static String done(String fingerprint, int httpStatus, byte[] body) {
        String encoded = (body != null && body.length > 0)
                ? Base64.getEncoder().encodeToString(body)
                : "";
        return "DONE:" + fingerprint + ":" + httpStatus + ":" + encoded;
    }

    public static IdempotencyRecord parse(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        int first = value.indexOf(':');
        if (first < 0) return null;

        String stateStr = value.substring(0, first);
        State state = State.valueOf(stateStr);

        String rest = value.substring(first + 1);
        int second = rest.indexOf(':');
        if (second < 0) return new IdempotencyRecord(state, rest, 0, null);

        String fingerprint = rest.substring(0, second);
        String afterFp = rest.substring(second + 1);

        int third = afterFp.indexOf(':');
        if (third < 0) return new IdempotencyRecord(state, fingerprint, 0, null);

        String statusStr = afterFp.substring(0, third);
        String bodyB64 = afterFp.substring(third + 1);

        int httpStatus = statusStr.isEmpty() ? 0 : Integer.parseInt(statusStr);
        byte[] body = bodyB64.isEmpty() ? new byte[0] : Base64.getDecoder().decode(bodyB64);

        return new IdempotencyRecord(state, fingerprint, httpStatus, body);
    }

    public State getState() { return state; }
    public String getFingerprint() { return fingerprint; }
    public int getHttpStatus() { return httpStatus; }
    public byte[] getBody() { return body; }
}
