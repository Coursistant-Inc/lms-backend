package com.coursistant.lms.shared.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.shared.exception.CustomException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPublicKey;

@Component
public class JwtParserUtil {

    private RSAPublicKey publicKey = null;

    @Value("${token.public-key-path:public.pem}")
    private String publicKeyPath;

    @PostConstruct
    public void init() {
        try {
            publicKey = (RSAPublicKey) RsaKeyUtil.loadPublicKey(publicKeyPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key from: " + publicKeyPath, e);
        }
    }

    /**
     * Verify the token signature and expiration, return the decoded JWT.
     * Callers should extract claims directly from the returned DecodedJWT.
     */
    public DecodedJWT verify(String token) {
        try {
            return JWT.require(Algorithm.RSA256(publicKey, null))
                    .withIssuer("https://usc.xlearnedu.com")
                    .withAudience("com.coursistant.lms")
                    .build()
                    .verify(token);
        } catch (Exception e) {
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
    }
}
