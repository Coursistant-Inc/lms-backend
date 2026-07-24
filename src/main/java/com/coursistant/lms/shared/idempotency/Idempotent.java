package com.coursistant.lms.shared.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as idempotent.
 * Requires the client to send an {@code Idempotency-Key} header.
 * The first successful (2xx) response is cached and replayed on subsequent
 * requests with the same key.
 *
 * <p><b>Usage constraint:</b> Only use on endpoints whose response is fully
 * expressed by the {@code ApiResponse} JSON body. Do NOT apply to endpoints
 * that set cookies or custom response headers (e.g. login, register,
 * refresh-token, logout), as replay only restores the body.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    long ttlSeconds() default 86400;
}
