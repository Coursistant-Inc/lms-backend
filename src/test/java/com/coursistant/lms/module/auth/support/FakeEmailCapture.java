package com.coursistant.lms.module.auth.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Captures outbound auth emails without SMTP.
 */
public final class FakeEmailCapture {

    public record CapturedEmail(String to, String subject, String body) {
    }

    private final List<CapturedEmail> sent = new CopyOnWriteArrayList<>();

    public void record(String to, String subject, String body) {
        sent.add(new CapturedEmail(to, subject, body));
    }

    public List<CapturedEmail> all() {
        return Collections.unmodifiableList(new ArrayList<>(sent));
    }

    public void clear() {
        sent.clear();
    }

    public int size() {
        return sent.size();
    }
}
