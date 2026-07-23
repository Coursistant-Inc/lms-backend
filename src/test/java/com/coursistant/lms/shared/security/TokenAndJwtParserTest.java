package com.coursistant.lms.shared.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.shared.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class TokenAndJwtParserTest {

    private JwtParserUtil jwtParserUtil;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @BeforeEach
    void setUp() throws Exception {
        privateKey = (RSAPrivateKey) RsaKeyUtil.loadPrivateKey("classpath:test-private.pem");
        publicKey = (RSAPublicKey) RsaKeyUtil.loadPublicKey("classpath:test-public.pem");

        setStaticField(TokenUtils.class, "staticPrivateKey", privateKey);
        setStaticField(TokenUtils.class, "staticAccessExpireHours", 2);

        jwtParserUtil = new JwtParserUtil();
        setField(jwtParserUtil, "publicKey", publicKey);
    }

    @Test
    void createAndVerify_validToken_success() {
        String token = TokenUtils.createAccessToken(42, "USER");

        DecodedJWT jwt = jwtParserUtil.verify(token);

        assertEquals(42, jwt.getClaim("userId").asInt());
        assertEquals("USER", jwt.getClaim("role").asString());
        assertEquals("access", jwt.getClaim("type").asString());
        assertEquals("42", jwt.getSubject());
    }

    @Test
    void verify_expiredToken_throwsCustomException() {
        String expired = JWT.create()
                .withSubject("1")
                .withClaim("userId", 1)
                .withClaim("role", "USER")
                .withClaim("type", "access")
                .withIssuedAt(new Date(System.currentTimeMillis() - 7200_000))
                .withExpiresAt(new Date(System.currentTimeMillis() - 3600_000))
                .sign(Algorithm.RSA256(null, privateKey));

        CustomException ex = assertThrows(CustomException.class, () -> jwtParserUtil.verify(expired));
        assertEquals(ResultCodeEnum.TOKEN_CHECK_ERROR.code, ex.getCode());
    }

    @Test
    void verify_tamperedToken_throwsCustomException() {
        String token = TokenUtils.createAccessToken(1, "USER");
        String[] parts = token.split("\\.");
        // flip a character in the payload segment
        char[] payload = parts[1].toCharArray();
        payload[0] = payload[0] == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + new String(payload) + "." + parts[2];

        CustomException ex = assertThrows(CustomException.class, () -> jwtParserUtil.verify(tampered));
        assertEquals(ResultCodeEnum.TOKEN_CHECK_ERROR.code, ex.getCode());
    }

    @Test
    void verify_wrongKey_throwsCustomException() throws Exception {
        RSAPrivateKey altPrivate = (RSAPrivateKey) RsaKeyUtil.loadPrivateKey("classpath:test-private-alt.pem");
        String token = JWT.create()
                .withSubject("1")
                .withClaim("userId", 1)
                .withClaim("role", "USER")
                .withClaim("type", "access")
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
                .sign(Algorithm.RSA256(null, altPrivate));

        CustomException ex = assertThrows(CustomException.class, () -> jwtParserUtil.verify(token));
        assertEquals(ResultCodeEnum.TOKEN_CHECK_ERROR.code, ex.getCode());
    }

    private static void setStaticField(Class<?> clazz, String name, Object value) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
